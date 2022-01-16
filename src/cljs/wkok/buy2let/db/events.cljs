(ns wkok.buy2let.db.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.site.effects :as sfx]
            [tick.core :as t]
            [wkok.buy2let.db.default :as ddb]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.backend.multimethods :as mm]
            [wkok.buy2let.spec :as spec]
            [wkok.buy2let.subscription.events :as subse]))

(rf/reg-event-fx
  :initialize-db
  (fn [_ [_ seed]]
    {:db (-> (merge ddb/default-db seed)
             (assoc-in [:site :location :hash] (shared/url-hash)))})) ;used for deep linking

(rf/reg-event-fx
  ::get-crud
  (fn [_ [_ account]]
    (mm/get-crud-fx {:account account
                     :on-success-delegates #(rf/dispatch [:load-delegates %])
                     :on-success-charges #(rf/dispatch [:load-charges %])
                     :on-success-properties #(rf/dispatch [:load-properties %])})))

(rf/reg-event-db
  :load-delegates
  (fn [db [_ input]]
    (assoc db :delegates (spec/conform ::spec/delegates input))))

(rf/reg-event-db
 :load-charges
 (fn [db [_ input]]
   (let [charges (spec/conform ::spec/crud-charges input)]
     (assoc db :charges (merge charges
                               (:charges ddb/default-db))))))

(rf/reg-event-fx
 :load-properties
 (fn [cofx [_ input]]
   (let [db (:db cofx)
         account-id (get-in db [:security :account])
         all-properties (spec/conform ::spec/properties input)
         properties (filter #(not (:hidden %)) (vals all-properties))
         updated-db (-> (assoc db :properties all-properties)
                        (assoc-in [:site :show-progress] false)
                        (assoc-in [:site :splash] false))]
     (if (empty? properties)
       (do (js/window.location.assign "#/properties/add") ; start wizard
           {:db                 (assoc-in updated-db [:wizard :active-page] :wizard)})
       (do (rf/dispatch [::get-ledger-year])
           (merge {:db                 updated-db
                   ::sfx/location-hash (get-in db [:site :location :hash])}
                  (mm/refresh-subscription {:account-id account-id
                                            :on-next #(rf/dispatch [::subse/validate-subscription %])})))))))

(rf/reg-event-db
  :load-invoices
  (fn [db [_ input]]
    (let [invoices (spec/conform ::spec/invoices input)]
      (update db :invoices merge invoices))))

(rf/reg-event-db
  :load-ledger-year
  (fn [db [_ input]]
    (let [{:keys [property-id year ledger-months]} (spec/conform ::spec/ledger-year input)]
      (-> (assoc-in db [:ledger property-id year] ledger-months)
          (assoc-in [:site :show-progress] false)))))

(rf/reg-event-fx
  ::get-ledger-year
  (fn [cofx _]
    (let [db (:db cofx)
          account-id (get-in db [:security :account])
          today (t/today)
          last (t/<< today (t/new-period 1 :years))
          this-year (-> today t/year str keyword)
          last-year (-> last t/year str keyword)]
      (if-not (empty? (:properties db))
        (merge {:db             (assoc-in db [:site :show-progress] true)}
               (mm/get-ledger-year-fx {:properties (:properties db)
                                       :account-id account-id
                                       :this-year this-year
                                       :last-year last-year
                                       :on-success #(rf/dispatch [:load-ledger-year %])}))
        {:db db}))))
