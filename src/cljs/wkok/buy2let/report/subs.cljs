(ns wkok.buy2let.report.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::report
  (fn [db _]
    (get-in db [:report])))

(rf/reg-sub
  ::report-year
  (fn [db [_ type]]
    (get-in db [:report type :year])))

(rf/reg-sub
  ::report-month
  (fn [db [_ type]]
    (get-in db [:report type :month])))

(rf/reg-sub
  :ledger-property
  (fn [db [_ property]]
    (get-in db [:ledger property])))

(rf/reg-sub
 ::report-show-invoices
 (fn [db _]
   (get-in db [:report :show-invoices])))