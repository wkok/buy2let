(ns wkok.buy2let.db.events
  (:require [wkok.buy2let.db.db :as db]
            [re-frame.core :as rf]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.site.effects :as se]
            [clojure.walk :as w]
            [tick.alpha.api :as t]
            [wkok.buy2let.backend.impl :as impl]
            [wkok.buy2let.backend.protocol :as bp]))

(rf/reg-event-fx
  :initialize-db
  (fn [_ [_ seed]]
    {:db (-> (merge db/default-db seed)
             (assoc-in [:site :location :hash] (-> js/window .-location .-hash)))})) ;used for deep linking

(rf/reg-event-fx
  ::get-crud
  (fn [_ [_ account]]
    (bp/get-crud-fx impl/backend account)))

(rf/reg-event-db
  :load-users
  (fn [db [_ results]]
    (assoc db :users (shared/to-crud results))))

(rf/reg-event-db
  :load-charges
  (fn [db [_ results]]
    (assoc db :charges (merge (shared/to-crud results)
                              (:charges db/default-db)))))

(rf/reg-event-fx
  :load-properties
  (fn [cofx [_ results]]
    (rf/dispatch [::get-ledger-year])
    {:db                (-> (assoc (:db cofx) :properties (shared/to-crud results))
                            (assoc-in [:site :show-progress] false))
     ::se/location-hash (get-in (:db cofx) [:site :location :hash])}))

(rf/reg-event-db
  :load-ledger-year
  (fn [db [_ property year result]]
    (let [ledger (->> (map (fn [r] {(:id r) (:data r)}) (:docs result))
                      (into {})
                      w/keywordize-keys)]
      (-> (assoc-in db [:ledger property year] ledger)
          (assoc-in [:site :show-progress] false)))))

(rf/reg-event-fx
  ::get-ledger-year
  (fn [cofx _]
    (let [db (:db cofx)
          account-id (get-in db [:security :account])
          today (t/today)
          last (t/- today (t/new-period 1 :years))
          this-year (-> today t/year str keyword)
          last-year (-> last t/year str keyword)]
      (if-not (empty? (:properties db))
        (merge {:db             (assoc-in db [:site :show-progress] true)}
               (bp/get-ledger-year-fx impl/backend (:properties db) account-id this-year last-year))
        {:db db}))))
