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

