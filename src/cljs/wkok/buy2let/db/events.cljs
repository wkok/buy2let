(ns wkok.buy2let.db.events
  (:require [wkok.buy2let.db.db :as db]
            [re-frame.core :as rf]
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
    (bp/get-crud-fx impl/backend 
                    {:account account
                     :on-success-users #(rf/dispatch [:load-users %])
                     :on-success-charges #(rf/dispatch [:load-charges %])
                     :on-success-properties #(rf/dispatch [:load-properties %])})))

(rf/reg-event-db
  :load-users
  (fn [db [_ users]]
    (assoc db :users users)))

(rf/reg-event-db
  :load-charges
  (fn [db [_ charges]]
    (assoc db :charges (merge charges
                              (:charges db/default-db)))))

(rf/reg-event-fx
  :load-properties
  (fn [cofx [_ properties]]
    (rf/dispatch [::get-ledger-year])
    {:db                (-> (assoc (:db cofx) :properties properties)
                            (assoc-in [:site :show-progress] false))
     ::se/location-hash (get-in (:db cofx) [:site :location :hash])}))

(rf/reg-event-db
  :load-ledger-year
  (fn [db [_ property year l]]
    (let [ledger (->> (map (fn [r] {(:id r) (:data r)}) l)
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
               (bp/get-ledger-year-fx impl/backend 
                                      {:properties (:properties db) 
                                       :account-id account-id 
                                       :this-year this-year 
                                       :last-year last-year
                                       :on-success #(rf/dispatch [:load-ledger-year %1 %2 %3])}))
        {:db db}))))
