(ns wkok.buy2let.db.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::ledger
  (fn [db _]
    (get-in db [:ledger])))
