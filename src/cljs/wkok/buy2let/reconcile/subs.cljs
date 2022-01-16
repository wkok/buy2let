(ns wkok.buy2let.reconcile.subs
  (:require [re-frame.core :as rf]
            [wkok.buy2let.period :as period]))

(rf/reg-sub
  ::reconcile-year
  (fn [db _]
    (get-in db [:reconcile :year])))

(rf/reg-sub
  ::reconcile-month
  (fn [db _]
    (get-in db [:reconcile :month])))

(rf/reg-sub
  ::reconcile-charge-id
  (fn [db _]
    (get-in db [:reconcile :charge-id])))

(rf/reg-sub
  ::reconcile-view-toggle
  (fn [db _]
    (get-in db [:reconcile :view])))


(rf/reg-sub
  :ledger-months
  (fn [db [_ property year month]]
    (let [prev (period/prev-month month year)]
      {:this-month (get-in db [:ledger property year month])
       :prev-month (get-in db [:ledger property (:year prev) (:month prev)])})))

