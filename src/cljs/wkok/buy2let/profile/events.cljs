(ns wkok.buy2let.profile.events
  (:require
   [re-frame.core :as rf]
   [clojure.string :as s]
   [wkok.buy2let.shared :as shared]
   [wkok.buy2let.backend.protocol :as bp]
   [wkok.buy2let.backend.impl :as impl]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.backend.subs :as fs]))


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

(rf/reg-event-fx
 ::save-profile
 (fn [cofx [_ profile]]
   (let [db (:db cofx)
         user (if-let [avatar-url-temp (get-in db [:site :avatar-url-temp])]
                (assoc profile :avatar-url avatar-url-temp)
                profile)]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (merge {:db            (-> (assoc-in db [:security :user] user)
                                (assoc-in [:site :avatar-url-temp] nil))}
            (bp/save-profile-fx impl/backend
                                {:user user
                                 :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))

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
            (bp/blob-url-fx
             impl/backend
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
            (bp/upload-avatar-fx
             impl/backend
             {:user-id (get-in db [:security :user :id])
              :avatar-id avatar-id
              :avatar avatar
              :on-success #(rf/dispatch [::get-avatar-url avatar-id])
              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))