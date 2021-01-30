(ns wkok.buy2let.profile.events
  (:require
   [re-frame.core :as rf]
   [nano-id.core :as nid]
   [goog.crypt.base64 :as b64]
   [wkok.buy2let.shared :as shared]
   [wkok.buy2let.backend.multimethods :as mm]
   [wkok.buy2let.site.events :as se]))


(rf/reg-event-db
  ::view-profile
  (fn [db [_ _]]
    (-> (assoc-in db [:site :active-page] :profile)
        (assoc-in [:site :active-panel] :profile-view)
        (assoc-in [:site :heading] "Profile"))))

(rf/reg-event-db
  ::edit-profile
  (fn [db [_ _]]
    (-> (assoc-in db [:form :old :profile] (get-in db [:security :user]))
        (assoc-in [:site :active-page] :profile)
        (assoc-in [:site :active-panel] :profile-edit)
        (assoc-in [:site :heading] "Edit profile"))))

(defn create-verification [user email-changed-token]
  {:to (:email user)
   :template {:name "verify-email"
              :data {:user-name (:name user)
                     :verify-url (str (shared/url-host)
                                      "?email-changed-verification="
                                      (b64/encodeString {:email (:email user)
                                                         :email-changed-token email-changed-token}))}}})

(defn assoc-avatar-url
  [profile {:keys [db]}]
  (if-let [avatar-url-temp (get-in db [:site :avatar-url-temp])]
    (assoc profile :avatar-url avatar-url-temp)
    profile))

(defn assoc-email-changed-token
  [profile {:keys [email-changed-token]}]
  (if email-changed-token
    (assoc profile :email-changed-token email-changed-token)
    (dissoc profile :email-changed-token)))

(rf/reg-event-fx
 ::save-profile
 (fn [cofx [_ profile]]
   (let [db (:db cofx)
         on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message (str %)}])
         email-changed-token (when (not= (:email profile) (get-in db [:security :claims :email]))
                               (nid/nano-id))
         verification-fx (if email-changed-token
                           (mm/send-email-changed-verification-fx {:verification (create-verification profile email-changed-token)
                                                                   :on-error on-error})
                           {})
         user (-> (assoc-avatar-url profile {:profile profile :db db})
                  (assoc-email-changed-token {:email-changed-token email-changed-token}))]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (merge {:db            (-> (assoc-in db [:security :user] user)
                                (assoc-in [:site :avatar-url-temp] nil))}
            (mm/save-profile-fx {:user user :on-error on-error})
            verification-fx))))

(rf/reg-event-db
 ::load-avatar-url
 (fn [db [_ url]]
   (-> (assoc-in db [:site :avatar-url-temp] url)
       (assoc-in [:site :splash] false))))

(rf/reg-event-db
 ::clear-temp-avatar
 (fn [db [_ _]]
   (assoc-in db [:site :avatar-url-temp] nil)))

(rf/reg-event-fx
 ::get-avatar-url
 (fn [cofx [_ avatar-id]]
   (let [db (:db cofx)]
     (merge {:db            db}
            (mm/blob-url-fx
             {:path (str "avatars/" (-> (get-in db [:security :user :id]) name) "/" avatar-id)
              :on-success #(rf/dispatch [::load-avatar-url %])
              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                    :message %}])})))))

(rf/reg-event-fx
 ::upload-avatar
 [(rf/inject-cofx ::shared/gen-id)]  ; Generate avatar-id
 (fn [cofx [_ avatar]]
   (let [db (:db cofx)
         avatar-id (-> (:id cofx) name)]
     (merge {:db            (assoc-in db [:site :splash] true)}
            (mm/upload-avatar-fx
             {:path (str "avatars/" (-> (get-in db [:security :user :id]) name) "/" avatar-id)
              :avatar-id avatar-id
              :avatar avatar
              :on-success #(rf/dispatch [::get-avatar-url avatar-id])
              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))