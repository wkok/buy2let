(ns wkok.buy2let.account.events
  (:require [re-frame.core :as rf]
            [nano-id.core :as nid]
            [wkok.buy2let.db.default :as ddb]
            [wkok.buy2let.backend.protocol :as bp]
            [wkok.buy2let.backend.impl :as impl]
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
         (assoc :security (:security db))
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
         account (spec/conform ::spec/account input)]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (merge {:db            (assoc-in db [:security :accounts (:id account)] account)}
            (bp/save-account-fx impl/backend
                                {:account account
                                 :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))

(rf/reg-event-db
 ::delete-account-understood
 (fn [db [_ delete-token account-id]]
   (rf/dispatch [::se/splash false])
   (assoc-in db [:security :accounts account-id :deleteToken] delete-token)))

(defn active-delegates? [db]
  (not (empty? (->> (:delegates db)
                    (filter #(not= "REVOKED" (-> % val :status)))))))

(defn create-delete-confirmation [user account delete-token]
  {:to (:email user)
   :template {:name "delete-account"
              :data {:user-name (:name user)
                     :account-name (:name account)
                     :delete-url (str (.. js/window -location -protocol) "//"
                                      (.. js/window -location -host)
                                      "?delete-confirmation="
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
         delete-token (nid/nano-id)]

     (if (active-delegates? db)
       (rf/dispatch [::se/dialog {:heading "Delegated access"
                                  :message "You have users linked / invited to your account. Please revoke
                                               their access first, before continuing with account deletion"
                                  :buttons   {:middle {:text     "Take me there"
                                                       :on-click (fn [] (js/window.location.assign "#/delegates"))}}}])
       (merge {:db (assoc-in db [:site :splash] true)}
              (bp/delete-account-fx
               impl/backend
               {:user-id (:id user)
                :account-id account-id
                :delete-token delete-token
                :confirmation (create-delete-confirmation user account delete-token)
                :on-success #(rf/dispatch [::se/dialog {:heading   "Email confirmation"
                                                        :message   "Please check your email for an account deletion link.
                                                                                Your account will remain active until you confirm deletion 
                                                                                by clicking the link in the email."
                                                        :buttons   {:middle {:text     "Understood"
                                                                             :on-click (fn [] (rf/dispatch [::delete-account-understood delete-token account-id]))}}
                                                        :closeable false}])
                :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                      :message (str %)}])}))))))

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
          (bp/delete-account-confirm-fx
           impl/backend
           {:user-id user-id
            :account-id account-id
            :delete-token delete-token
            :on-success #(do
                           (rf/dispatch [::se/splash false])
                           (rf/dispatch [::se/show-progress false])
                           (rf/dispatch [::se/dialog {:heading   "Account deleted"
                                                      :message   "Successfully deleted your account. Hope to have you back soon!"
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
            (bp/save-profile-fx impl/backend
                                {:user updated-user
                                 :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))
