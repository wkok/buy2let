(ns wkok.buy2let.crud.subs
  (:require [re-frame.core :as rf]
            [wkok.buy2let.shared :as shared]))

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
 ::hidden-properties
 (fn [db]
   (->> (:properties db)
        vals
        (filter #(:hidden %))
        (sort-by :name))))

(defn get-properties
  [db]
  (->> (:properties db)
       vals
       (filter #(not (:hidden %)))
       (sort-by :name)))

(rf/reg-sub
  ::properties
  #(get-properties %))

(rf/reg-sub
  ::all-charges
  (fn [db]
    (->> (:charges db)
         vals
         (sort-by :name))))

(rf/reg-sub
 ::hidden-charges
 (fn [db]
   (->> (:charges db)
        vals
        (filter #(:hidden %))
        (sort-by :name))))

(rf/reg-sub
 ::charges
 (fn [db]
   (->> (:charges db)
        vals
        (filter #(not (:hidden %)))
        (sort-by :name))))

(rf/reg-sub
 ::all-invoices
 (fn [db]
   (->> (shared/filter-charge-invoices db {})
        (into {})
        vals
        (sort-by :name))))

(rf/reg-sub
 ::hidden-invoices
 (fn [db]
   (->> (shared/filter-charge-invoices db {})
        (into {})
        vals
        (filter :hidden)
        (sort-by :name))))

(rf/reg-sub
 ::invoices
 (fn [db]
   (->> (shared/filter-charge-invoices db {})
        (into {})
        vals
        (filter #(not (:hidden %)))
        (sort-by :name))))

(rf/reg-sub
 ::invoices-for
 (fn [db [_ options]]
   (->> options
        (shared/filter-charge-invoices db)
        (into {})
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
 ::hidden-delegates
 (fn [db]
   (->> (:delegates db)
        vals
        (filter #(:hidden %))
        (sort-by :name))))

(rf/reg-sub
  ::delegates
  (fn [db]
    (->> (:delegates db)
         vals
         (filter #(not (:hidden %)))
         (sort-by :name))))
