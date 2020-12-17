(ns wkok.buy2let.wizard.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::wizard-active-step
 (fn [db _]
   (get-in db [:wizard :active-step] 0)))

(rf/reg-sub
 ::wizard-property-name
 (fn [db _]
   (get-in db [:wizard :property-name])))

(rf/reg-sub
 ::wizard-mortgage-payment?
 (fn [db _]
   (get-in db [:wizard :mortgage-payment])))

(rf/reg-sub
 ::wizard-rental-agent?
 (fn [db _]
   (get-in db [:wizard :rental-agent])))

(rf/reg-sub
 ::wizard-mortgage-repayment-amount
 (fn [db _]
   (get-in db [:wizard :breakdown :mortgage-repayment-id :amount])))

(rf/reg-sub
 ::wizard-mortgage-interest-amount
 (fn [db _]
   (get-in db [:wizard :breakdown :mortgage-interest-id :amount])))

(rf/reg-sub
 ::wizard-commission-amount
 (fn [db _]
   (get-in db [:wizard :breakdown :agent-commission-id :amount])))

(rf/reg-sub
 ::wizard-rent-charged-amount
 (fn [db _]
   (get-in db [:wizard :breakdown :rent-charged-id :amount])))
