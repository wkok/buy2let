(ns wkok.buy2let.account.events
  (:require [re-frame.core :as rf]
            [nano-id.core :as nid]
            [wkok.buy2let.db.default :as ddb]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.backend.multimethods :as mm]
            [wkok.buy2let.db.events :as dbe]
            [wkok.buy2let.spec :as spec]
            [goog.crypt.base64 :as b64]
            [wkok.buy2let.site.events :as se]))

(rf/reg-event-db
 :set-active-account
 (fn [db [_ input]]
   (let [account-id (spec/conform ::spec/id input)]
     (rf/dispatch [::dbe/get-crud account-id])
     (-> ddb/default-db
         (assoc-in [:site :account-selector] (get-in db [:site :account-selector]))
         (assoc-in [:site :location] (get-in db [:site :location]))
         (assoc :security (:security db))
         (assoc :backend (:backend db))
         (assoc-in [:security :account] account-id)))))

(rf/reg-event-db
 :select-account
 (fn [db [_ account-id]]
   (assoc-in db [:site :account-selector :account-id] account-id)))

(rf/reg-event-db
 ::remember-account
 (fn [db [_ remember]]
   (assoc-in db [:site :account-selector :remember] remember)))

(rf/reg-event-db
 :load-account
 (fn [db [_ input]]
   (let [account (spec/conform ::spec/account input)]
     (update-in db [:security :accounts] #(assoc % (:id account) account)))))

(rf/reg-event-db
 ::view-account
 (fn [db [_ _]]
   (-> (assoc-in db [:site :active-page] :account)
       (assoc-in [:site :active-panel] :account-view)
       (assoc-in [:site :heading] "Account"))))

(rf/reg-event-db
 ::edit-account
 (fn [db [_ _]]
   (let [accounts (get-in db [:security :accounts])
         account-id (get-in db [:security :account])
         account (account-id accounts)]
     (-> (assoc-in db [:form :old :account] account)
         (assoc-in [:site :active-page] :account)
         (assoc-in [:site :active-panel] :account-edit)
         (assoc-in [:site :heading] "Edit account")))))


(rf/reg-event-fx
 ::save-account
 (fn [cofx [_ input]]
   (let [db (:db cofx)
         conformed (spec/conform ::spec/account (:account input))
         account (if-let [avatar-url-temp (get-in db [:site :account-avatar-url-temp])]
                   (assoc conformed :avatar-url avatar-url-temp)
                   conformed)]
     (when (:back input true) 
       (js/window.history.back))                              ;opportunistic.. assume success 99% of the time..
     (merge {:db            (-> (assoc-in db [:security :accounts (:id account)] account)
                                (assoc-in [:site :account-avatar-url-temp] nil))}
            (mm/save-account-fx {:account account
                                 :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))

(rf/reg-event-db
 ::delete-account-understood
 (fn [db [_ delete-token account-id]]
   (rf/dispatch [::se/splash false])
   (assoc-in db [:security :accounts account-id :deleteToken] delete-token)))

(defn active-delegates? [db]
  (not (empty? (->> (:delegates db)
                    (filter #(not= "REVOKED" (-> % val :status)))))))

(defn create-delete-confirmation [mode user account delete-token]
  {:to (:email user)
   :template {:name "delete-account"
              :data {:user-name (:name user)
                     :account-name (:name account)
                     :delete-url (str (shared/url-host)
                                      "?mode=" (name mode)
                                      "&delete-confirmation="
                                      (b64/encodeString {:user-id (:id user)
                                                         :account-id (:id account)
                                                         :delete-token delete-token}))}}})

(rf/reg-event-fx
 ::delete-account
 (fn [cofx _]
   (let [db (:db cofx)
         accounts (get-in db [:security :accounts])
         account-id (get-in db [:security :account])
         account (account-id accounts)
         user (get-in db [:security :user])
         delete-token (nid/nano-id)
         mode (get-in db [:backend :mode])]

     (if (active-delegates? db)
       (rf/dispatch [::se/dialog {:heading "Delegated access"
                                  :message "You have users linked / invited to your account. Please revoke
                                               their access first, before continuing with account deletion"
                                  :buttons   {:middle {:text     "Take me there"
                                                       :on-click (fn [] (js/window.location.assign "#/delegates"))}}}])
       (if (:email user)
         (merge {:db (assoc-in db [:site :splash] true)}
                (mm/delete-account-fx
                 {:user-id (:id user)
                  :account-id account-id
                  :delete-token delete-token
                  :confirmation (create-delete-confirmation mode user account delete-token)
                  :on-success #(rf/dispatch [::se/dialog {:heading   "Email confirmation"
                                                          :message   "Please check your email for an account deletion link.
                                                                                Your account will remain active until you confirm deletion 
                                                                                by clicking the link in the email."
                                                          :buttons   {:middle {:text     "Understood"
                                                                               :on-click (fn [] (rf/dispatch [::delete-account-understood delete-token account-id]))}}
                                                          :closeable false}])
                  :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                        :message (str %)}])}))
         (rf/dispatch [::delete-account-confirm {:user-id (:id user)
                                                 :account-id account-id} user]))))))

(defn reset-query-params []
  (when (.-pushState js/history)
    (let [url (str
               (.. js/window -location -origin)
               (.. js/window -location -pathname))]
      (.pushState (.-history js/window) #js {:path url} "" url))))

(rf/reg-event-fx
 ::delete-account-confirm
 (fn [cofx [_ {:keys [account-id user-id delete-token]} user]]
   (reset-query-params)
   (merge {:db                (assoc-in (:db cofx) [:site :splash] true)}
          (mm/delete-account-confirm-fx
           {:mode (get-in (:db cofx) [:backend :mode])
            :user-id user-id
            :account-id account-id
            :delete-token delete-token
            :on-success #(do
                           (rf/dispatch [::se/splash false])
                           (rf/dispatch [::se/show-progress false])
                           (rf/dispatch [::se/dialog {:heading   "Account deleted"
                                                      :message   "Successfully deleted your account. Any subscriptions were automatically cancelled. Hope to have you back soon!"
                                                      :buttons   {:middle {:text     "Goodbye"
                                                                           :on-click (fn [] (rf/dispatch [:sign-out]))}}
                                                      :closeable false}]))
            :on-error #(do
                         (rf/dispatch [:refresh-token user])
                         (rf/dispatch [::se/splash false])
                         (rf/dispatch [::se/show-progress false])
                         (rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                    :message (str %)}]))}))))

(rf/reg-event-fx
 ::save-default-account
 (fn [cofx [_ user remember account-id]]
   (let [updated-user (if remember
                        (assoc user :default-account-id account-id)
                        (dissoc user :default-account-id))]
     (merge {:db (assoc-in (:db cofx) [:security :user] updated-user)}
            (mm/save-profile-fx {           :user updated-user
                                 :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))

(rf/reg-event-db
 ::load-avatar-url
 (fn [db [_ url]]
   (-> (assoc-in db [:site :account-avatar-url-temp] url)
       (assoc-in [:site :splash] false))))

(rf/reg-event-db
 ::clear-temp-avatar
 (fn [db [_ _]]
   (assoc-in db [:site :account-avatar-url-temp] nil)))

(rf/reg-event-fx
 ::get-avatar-url
 (fn [cofx [_ avatar-id]]
   (let [db (:db cofx)]
     (merge {:db            db}
            (mm/blob-url-fx
             {:path (str "data/" 
                         (-> (get-in db [:security :account]) name)
                         "/avatars/" avatar-id)
              :on-success #(rf/dispatch [::load-avatar-url %])
              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                    :message %}])})))))

(rf/reg-event-fx
 ::upload-avatar
 [(rf/inject-cofx ::shared/gen-id)]  ; Generate avatar-id
 (fn [cofx [_ avatar]]
   (let [db (:db cofx)
         avatar-id (-> (:id cofx) name)
         account-id (get-in db [:security :account])]
     (merge {:db            (assoc-in db [:site :splash] true)}
            (mm/upload-avatar-fx
             {:path (str "data/"
                         (-> account-id name)
                         "/avatars/" avatar-id)
              :metadata {:customMetadata {"accountId" account-id}}
              :avatar-id avatar-id
              :avatar avatar
              :on-success #(rf/dispatch [::get-avatar-url avatar-id])
              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))