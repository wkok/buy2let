(ns wkok.buy2let.wizard.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.reconcile.events :as re]
            [wkok.buy2let.shared :as shared]
            [tick.alpha.api :as t]
            [cljc.java-time.month :as tm]
            [wkok.buy2let.backend.multimethods :as mm]
            [wkok.buy2let.crud.types :as types]))

(rf/reg-event-db
 ::navigate
 (fn [db [_ direction]]
   (let [active-step (get-in db [:wizard :active-step])]
     (case direction
       :next (assoc-in db [:wizard :active-step] (inc active-step))
       :back (assoc-in db [:wizard :active-step] (dec active-step))))))

(rf/reg-event-db
 ::set-property-name
 (fn [db [_ name]]
   (assoc-in db [:wizard :property-name] name)))

(rf/reg-event-db
 ::set-mortgage-payment
 (fn [db [_ question answer]]
   (case question
     :yes? (assoc-in db [:wizard :mortgage-payment] answer)
     :no? (assoc-in db [:wizard :mortgage-payment] (not answer)))))

(rf/reg-event-db
 ::set-rental-agent
 (fn [db [_ question answer]]
   (case question
     :yes? (assoc-in db [:wizard :rental-agent] answer)
     :no? (assoc-in db [:wizard :rental-agent] (not answer)))))

(rf/reg-event-db
 ::set-mortgage-repayment-amount
 (fn [db [_ amount]]
   (if amount
     (assoc-in db [:wizard :breakdown :mortgage-repayment-id :amount]
               (-> amount js/parseFloat Math/abs))
     db)))

(rf/reg-event-db
 ::set-mortgage-interest-amount
 (fn [db [_ amount]]
   (if amount
     (assoc-in db [:wizard :breakdown :mortgage-interest-id :amount]
               (-> amount js/parseFloat Math/abs))
     db)))

(rf/reg-event-db
 ::set-commission-amount
 (fn [db [_ amount]]
   (if amount
     (assoc-in db [:wizard :breakdown :agent-commission-id :amount]
               (-> amount js/parseFloat Math/abs))
     db)))

(rf/reg-event-db
 ::set-rent-charged-amount
 (fn [db [_ amount]]
   (if amount
     (let [parsed (-> amount js/parseFloat Math/abs)]
       (-> (assoc-in db [:wizard :breakdown :rent-charged-id :amount] parsed)
           (assoc-in [:wizard :breakdown :payment-received-id :amount] parsed)))
     db)))

(rf/reg-event-db
 ::set-charge
 (fn [db [_ selected? charge]]
   (let [rental-agent? (get-in db [:wizard :rental-agent?] false)]
     (if selected?
       (assoc-in db [:wizard :charges (:id charge)] {:who-pays-whom (if rental-agent? :aps :ops)})
       (update-in db [:wizard :charges] dissoc (:id charge))))))

(defn build-property [property-id db]
  (let [mortgage-payment (if (get-in db [:wizard :mortgage-payment])
                           {:mortgage-interest-id {:who-pays-whom :mi}
                            :mortgage-repayment-id {:who-pays-whom :opb}}
                           {})
        rental-agent (if (get-in db [:wizard :rental-agent])
                       {:agent-commission-id {:who-pays-whom :ac}
                        :payment-received-id {:who-pays-whom :apo}
                        :rent-charged-id {:who-pays-whom :oca}
                        :rates-taxes-id {:who-pays-whom :aps}
                        :levy-id {:who-pays-whom :aps}}
                       {:payment-received-id {:who-pays-whom :tpo}
                        :rent-charged-id {:who-pays-whom :oct}
                        :levy-id {:who-pays-whom :ops}
                        :rates-taxes-id {:who-pays-whom :ops}})]
    {:id property-id
     :name (-> db :wizard :property-name)
     :charges (merge mortgage-payment rental-agent)}))

(rf/reg-event-fx
 ::finish
 [(rf/inject-cofx ::shared/gen-id)] ; new property-id
 (fn [cofx _]
   (let [db (:db cofx)
         account-id (get-in db [:security :account])
         property (build-property (:id cofx) db)
         today (t/today)
         this-year (-> today t/year str keyword)
         this-month (-> today t/month tm/ordinal inc str keyword)
         this-breadown (-> db :wizard :breakdown
                           (dissoc :payment-received-id)
                           (assoc :payment-received-id {:note "Awaiting rent payment for this month! Please capture the amount received from the agent / tenant."}))
         this-ledger (shared/apply-breakdown
                      {:this-month {:breakdown this-breadown}}
                      js/parseFloat)
         charges (->> (:charges db)
                      vals
                      (filter #(not (:hidden %)))
                      (sort-by :name))
         charges-this-month (re/by-storage-type account-id property this-year this-month this-ledger charges account-id)]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (merge {:db              (-> (assoc-in db [:properties (:id property)] property)
                                  (assoc-in [:ledger (:id property) this-year this-month] (:data charges-this-month))
                                  (assoc-in [:site :active-panel] :reconcile-view)
                                  (dissoc :wizard))}
            (mm/save-crud-fx {:account-id account-id
                              :crud-type types/property
                              :id (:id property)
                              :item property
                              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})
            (mm/save-reconcile-fx {:account-id account-id
                                   :property-id (:id property)
                                   :year this-year
                                   :month this-month
                                   :charges-this-month charges-this-month})))))
