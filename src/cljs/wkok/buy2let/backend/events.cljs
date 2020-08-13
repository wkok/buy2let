(ns wkok.buy2let.backend.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.db.events :as dbe]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.backend.impl :as impl]
            [wkok.buy2let.backend.effects]
            [wkok.buy2let.backend.protocol :as bp]
            [wkok.buy2let.spec :as spec]))

(rf/reg-event-fx
 ::sign-in
 (fn [_ [_ provider]]
   (case provider
     :google (bp/google-sign-in-fx impl/backend)
     :facebook (bp/facebook-sign-in-fx impl/backend)
     :twitter (bp/twitter-sign-in-fx impl/backend)
     :github (bp/github-sign-in-fx impl/backend))))

(rf/reg-event-db
  :unlink-succeeded
  (fn [db [_ provider]]
    (-> (assoc-in db [:security :auth :provider-data] (->> (get-in db [:security :auth])
                                                           :provider-data
                                                           js->clj
                                                           (remove #(= (get % "providerId") provider))))
        (assoc-in [:site :dialog] {:heading "Success" :message (str "Successfully unlinked " provider)}))))

(rf/reg-event-fx
  ::link
  (fn [_ [_ provider]]
    {:link-provider provider}))

(rf/reg-event-fx
  ::unlink
  (fn [_ [_ provider]]
    {:unlink-provider provider}))


(defn registered? [user]
  (not (nil? user)))

(rf/reg-event-fx
 :get-user
 (fn [_ [_ input]]
   (let [auth (spec/conform ::spec/auth input)]
     (bp/get-user-fx impl/backend
                     {:auth auth
                      :on-success #(if (registered? %)
                                     (rf/dispatch [:load-user
                                                   (assoc % :accounts (map (fn [a] (keyword a)) (:accounts %)))])
                                     (rf/dispatch [:create-user auth]))}))))

(rf/reg-event-fx
 :load-user
 (fn [cofx [_ input]]
   (let [user (spec/conform ::spec/user input)]
     (merge {:db            (assoc-in (:db cofx) [:security :user] user)}
            (bp/get-account-fx impl/backend
                               {:user user
                                :on-success #(rf/dispatch [:load-account %])})))))

(rf/reg-event-db
 :load-account
 (fn [db [_ input]]
   (let [account (spec/conform ::spec/account input)]
     (rf/dispatch [::dbe/get-crud (:id account)])
     (-> (assoc-in db [:security :accounts] {(:id account) account})
         (assoc-in [:security :account] (:id account)))))) ;TODO Account chooser

(rf/reg-event-fx
 :create-user
 [(rf/inject-cofx ::shared/gen-id)]                        ; Generate account id
 (fn [cofx [_ input]]
   (let [auth (spec/conform ::spec/auth input)
         user {:id       (keyword (:uid auth))
               :name     (:display-name auth)
               :email    (:email auth)
               :accounts [(:id cofx)]}
         account {:id   (keyword (:id cofx))
                  :name (:display-name auth)}]
     (merge {:db                (-> (assoc-in (:db cofx) [:security :user] user)
                                    (assoc-in [:security :accounts (:id account)] account)
                                    (assoc-in [:security :account] (:id account))
                                    (assoc-in [:site :show-progress] false))}
            (bp/create-user-fx impl/backend
                               {:user user
                                :account account
                                :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                      :message (str %)}])})))))

(rf/reg-event-fx
 ::sign-out
 (fn [_ _]
   (merge {:db            shared/default-db}
          (bp/sign-out-fx impl/backend))))

(rf/reg-event-fx
 ::delete-account
 (fn [cofx _]
   (let [db (:db cofx)
         account-id (get-in db [:security :account])
         user-id (get-in db [:security :user :id])]
     (merge {:db                (assoc-in db [:site :show-progress] true)}
            (bp/delete-account-fx impl/backend
                                  {:user-id user-id
                                   :account-id account-id
                                   :on-success #(do
                                                  (rf/dispatch [::se/show-progress false])
                                                  (rf/dispatch [::se/dialog {:heading   "Account deleted"
                                                                             :message   "Successfully deleted your account. Hope to have you back soon!"
                                                                             :buttons   {:middle {:text     "Good bye"
                                                                                                  :on-click (fn [] (rf/dispatch [::sign-out]))}}
                                                                             :closeable false}]))
                                   :on-error #(do
                                                (rf/dispatch [::se/show-progress false])
                                                (rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                           :message (str %)}]))})))))


