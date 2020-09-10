(ns wkok.buy2let.backend.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.db.events :as dbe]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.backend.impl :as impl]
            [wkok.buy2let.backend.subs :as bs]
            [wkok.buy2let.backend.effects]
            [wkok.buy2let.backend.protocol :as bp]
            [wkok.buy2let.spec :as spec]
            [reagent.core :as ra]))

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
 ::get-account
 (fn [_ [_ account-id]]
   (bp/get-account-fx impl/backend
                      {:account-id account-id
                       :on-success #(rf/dispatch [:load-account %])})))


(def selected-account-id (ra/atom :none))
(defn select-account []
  (let [accounts @(rf/subscribe [::bs/accounts])]
    [:div
     [:select {:value     @selected-account-id
               :on-change #(reset! selected-account-id (-> % .-target .-value))}
      (for [account accounts]
        ^{:key (key account)}
        [:option {:value (key account)} (-> account val :name)])]
     [:br]]))

(defn account-dialog []
  {:heading   "Which account?"
   :message   "Please choose the account you want to access"
   :panel     [select-account]
   :buttons   {:middle {:text     "Continue"
                        :on-click #(do
                                     (rf/dispatch [:set-active-account @selected-account-id])
                                    ;;  (rf/dispatch [::get-account @selected-account-id])
                                     (rf/dispatch [::se/dialog]))}}
   :closeable false})

(defn choose-account-fx [db user]
  (reset! selected-account-id (-> (:accounts user) first name))
  (rf/dispatch [::se/dialog (account-dialog)])
  (merge {:db (assoc-in db [:security :user] user)}
         (bp/get-accounts-fx impl/backend
                             {:account-ids (:accounts user)
                              :on-success #(rf/dispatch [:load-account %])})))

(defn get-account-fx [db user]
  (merge {:db            (assoc-in db [:security :user] user)}
         (bp/get-account-fx impl/backend
                            {:account-id (first (:accounts user))
                             :on-success #(do (rf/dispatch [:load-account %])
                                              (rf/dispatch [:set-active-account (:id %)]))})))

(rf/reg-event-fx
 :load-user
 (fn [cofx [_ input]]
   (let [user (spec/conform ::spec/user input)]
     (if (-> (:accounts user) second) ; User has access to more than one account
       (choose-account-fx (:db cofx) user)
       (get-account-fx (:db cofx) user)))))

(rf/reg-event-db
 :load-account
 (fn [db [_ input]]
   (let [account (spec/conform ::spec/account input)]
     (update-in db [:security :accounts] #(assoc % (:id account) account)))))

(rf/reg-event-db
 :set-active-account
 (fn [db [_ account-id]]
   (rf/dispatch [::dbe/get-crud account-id])
   (assoc-in db [:security :account] account-id)))

(rf/reg-event-fx
 :create-user
 [(rf/inject-cofx ::shared/gen-id)]                        ; Generate account id
 (fn [cofx [_ input]]
   (let [auth (spec/conform ::spec/auth input)
         user (spec/conform ::spec/user
                            {:id       (keyword (:uid auth))
                             :name     (:display-name auth)
                             :email    (:email auth)
                             :accounts [(:id cofx)]})
         account {:id   (keyword (:id cofx))
                  :name (:display-name auth)}]
     (merge {:db                (-> (assoc-in (:db cofx) [:security :user] user)
                                    (assoc-in [:security :account] (:id account))
                                    (assoc-in [:site :show-progress] false))}
            (bp/create-user-fx impl/backend
                               {:user user
                                :account account
                                :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                      :message (str %)}])})))))

(rf/reg-event-fx
 :sign-out
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
                                                                                                  :on-click (fn [] (rf/dispatch [:sign-out]))}}
                                                                             :closeable false}]))
                                   :on-error #(do
                                                (rf/dispatch [::se/show-progress false])
                                                (rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                           :message (str %)}]))})))))


