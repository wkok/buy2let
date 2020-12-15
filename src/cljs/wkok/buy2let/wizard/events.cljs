(ns wkok.buy2let.wizard.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.crud.events :as ce]
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
 ::finish
 (fn [db _]
   (let [mortgage-payment (if (get-in db [:wizard :mortgage-payment])
                            {:mortgage-interest {:who-pays-whom :mi}
                             :mortgage-repayment {:who-pays-whom :opb}}
                            {})
         rental-agent (if (get-in db [:wizard :rental-agent])
                        {:agent-commission {:who-pays-whom :ac}
                         :agent-inspection {:who-pays-whom :ac}
                         :payment-received {:who-pays-whom :apo}
                         :rent-charged {:who-pays-whom :oca}
                         :rates-taxes {:who-pays-whom :aps}
                         :levy {:who-pays-whom :aps}}
                        {:payment-received {:who-pays-whom :tpo}
                         :rent-charged {:who-pays-whom :oct}
                         :levy {:who-pays-whom :ops}
                         :rates-taxes {:who-pays-whom :ops}})
         property {:name (-> db :wizard :property-name)
                   :charges (merge mortgage-payment rental-agent)}]
     (rf/dispatch [::ce/save-crud types/property property])
     (dissoc db :wizard))))

