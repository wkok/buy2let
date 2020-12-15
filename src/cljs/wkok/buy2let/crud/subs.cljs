(ns wkok.buy2let.crud.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::show-hidden
  (fn [db _]
    (get-in db [:crud :show-hidden] false)))

(rf/reg-sub
  ::all-properties
  (fn [db]
    (->> (:properties db)
         vals
         (sort-by :name))))

(rf/reg-sub
  ::properties
  (fn [db]
    (->> (:properties db)
         vals
         (filter #(not (:hidden %)))
         (sort-by :name))))

(rf/reg-sub
  ::all-charges
  (fn [db]
    (->> (:charges db)
         vals
         (sort-by :name))))

(rf/reg-sub
 ::charges
 (fn [db]
   (->> (:charges db)
        vals
        (filter #(not (:hidden %)))
        (sort-by :name))))

(rf/reg-sub
 ::all-delegates
 (fn [db]
   (->> (:delegates db)
        vals
        (sort-by :name))))

(rf/reg-sub
  ::delegates
  (fn [db]
    (->> (:delegates db)
         vals
         (filter #(not (:hidden %)))
         (sort-by :name))))

