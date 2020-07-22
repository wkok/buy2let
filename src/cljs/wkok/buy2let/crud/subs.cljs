(ns wkok.buy2let.crud.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::show-hidden
  (fn [db _]
    (get-in db [:crud :show-hidden] false)))

(rf/reg-sub
  ::properties
  (fn [db]
    (->> (:properties db)
         vals
         (sort-by :name))))

(rf/reg-sub
  ::charges
  (fn [db]
    (->> (:charges db)
         vals
         (sort-by :name))))

(rf/reg-sub
  ::users
  (fn [db]
    (->> (:users db)
         vals
         (sort-by :name))))

