(ns wkok.buy2let.db.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::ledger
 (fn [db _]
   (->> (:ledger db)
        (filter #(let [property-id (key %)
                       property (get-in db [:properties property-id])]
                   (not (:hidden property))))
        (into {}))))