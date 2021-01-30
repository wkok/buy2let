(ns wkok.buy2let.subscription.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.spec :as spec]
            [wkok.buy2let.backend.multimethods :as mm]
            [wkok.buy2let.site.events :as se]))

(rf/reg-event-db
 ::view-subscription
 (fn [db _]
   (when (not (get-in db [:site :location :currency]))
     (rf/dispatch [::se/detect-location]))
   (let [account-id (get-in db [:security :account])
         accounts (get-in db [:security :accounts])
         account (when account-id (account-id accounts))
         subscribed-properties (get-in account [:subscription :properties] 1)]
     (-> (assoc-in db [:site :active-page] :subscription)
         (assoc-in [:site :active-panel] :subscription-view)
         (assoc-in [:site :heading] "Subscription")
         (assoc-in [:site :subscription :properties] (inc subscribed-properties))))))

(rf/reg-event-fx
 ::manage-subscription
 (fn [cofx [_ _]]
   (let [db (:db cofx)
         account-id (-> (get-in db [:security :account]) name)]
     (merge {:db (assoc-in db [:site :show-progress] true)}
            (mm/manage-subscription {:mode (get-in db [:backend :mode])
                                     :account-id account-id
                                     :on-success #(js/window.location.assign %)
                                     :on-error #(do (rf/dispatch [::se/show-progress false])
                                                    (rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                               :message (str %)}]))})))))

(rf/reg-event-db
 ::validate-subscription
 (fn [db [_ input]]
   (let [account (spec/conform ::spec/account input)
         subscription (:subscription account)
         valid-qty? (<= (count (:properties db)) (:properties subscription))
         error (or (when (:deleteToken account)
                     "Account deletion initiated, please check your email. You may cancel this in Account settings")
                   (when (= "failed" (get-in account [:subscription :status]))
                     "Account in arrears, please check your payment information to avoid suspension"))]
     (when (not valid-qty?)
       (rf/dispatch [::show-active-properties-dialog true]))
     (-> (update-in db [:security :accounts] #(assoc % (:id account) account))
         (assoc-in [:site :subscription :properties] (inc (:properties subscription)))
         (assoc-in [:site :snack :error] error)))))

(rf/reg-event-fx
 ::upgrade-subscription
 (fn [cofx _]
   (let [db (:db cofx)
         account-id (-> (get-in db [:security :account]) name)
         payment-instance (get-in db [:backend :subscription :instance])]
     (merge {:db (assoc-in db [:site :show-progress] true)}
            (mm/upgrade-subscription {:mode (get-in db [:backend :mode])
                                      :account-id account-id
                                      :quantity (get-in db [:site :subscription :properties])
                                      :currency (get-in db [:site :location :currency] "USD")
                                      :on-success (mm/upgrade-subscription-checkout {:payment-instance payment-instance})
                                      :on-error #(do (rf/dispatch [::se/show-progress false])
                                                     (rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                                :message (str %)}]))})))))

(rf/reg-event-fx
 ::downgrade-subscription
 (fn [cofx [_ property-ids]]
   (let [db (:db cofx)
         account-id (-> (get-in db [:security :account]) name)
         deleter (fn [properties]
                   (->> (remove #(some #{(key %)} property-ids) properties)
                        (into {})))]
     (merge {:db (-> (assoc-in db [:site :subscription :show-active-properties-dialog] false)
                     (update :properties deleter)
                     (update :ledger deleter))}
            (mm/downgrade-subscription {:account-id account-id
                                        :property-ids property-ids
                                        :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                              :message (str %)}])})))))

(rf/reg-event-db
 ::set-subscription-properties
 (fn [db [_ properties]]
   (assoc-in db [:site :subscription :properties] properties)))

(rf/reg-event-db
 ::set-active-subscription-properties
 (fn [db [_ properties]]
   (assoc-in db [:site :subscription :active-properties] properties)))

(rf/reg-event-db
 ::ack-inactive-delete
 (fn [db [_ ack]]
   (assoc-in db [:site :subscription :inactive-delete-ack] ack)))

(rf/reg-event-db
 ::show-active-properties-dialog
 (fn [db [_ show]]
   (assoc-in db [:site :subscription :show-active-properties-dialog] show)))