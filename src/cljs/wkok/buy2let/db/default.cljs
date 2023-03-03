(ns wkok.buy2let.db.default
  (:require
   [re-frame.core :as rf]
   [wkok.buy2let.crud.subs :as cs]
   [wkok.buy2let.shared :as shared]
   [wkok.buy2let.reconcile.subs :as rs]
   [wkok.buy2let.report.subs :as reps]
   [wkok.buy2let.site.subs :as ss]))

(def default-db
  {:site      {:heading      "Dashboard"
               :show-progress true
               :active-page :dashboard
               :splash true}
   :report    {:show-invoices false}
   :charges   {:agent-opening-balance {:id       :agent-opening-balance
                                       :name     "Opening balance"
                                       :reserved true}
               :tenant-opening-balance {:id      :tenant-opening-balance
                                       :name     "Opening balance"
                                       :reserved true}}})

(defn calc-report-options-db
  [db {:keys [property-id from-month from-year to-month to-year]}]
  (let [properties (cs/get-properties db)
        active-property (get-in db [:site :active-property])
        report-from-year (get-in db [:report :from :year])
        report-from-month (get-in db [:report :from :month])
        report-to-year (get-in db [:report :to :year])
        report-to-month (get-in db [:report :to :month])]
    {:property-id (or (-> property-id keyword)
                      active-property
                      (->> properties first :id)
                      "")
     :from-year (or (-> from-year keyword)
                    report-from-year
                    (:last-year shared/default-cal))
     :from-month (or (-> from-month keyword)
                     report-from-month
                     (:last-month shared/default-cal))
     :to-year (or (-> to-year keyword)
                  report-to-year
                  (:this-year shared/default-cal))
     :to-month (or (-> to-month keyword)
                   report-to-month
                   (:this-month shared/default-cal))}))

(defn calc-report-options
  [{:keys [property-id from-month from-year to-month to-year]}]
  (let [properties @(rf/subscribe [::cs/properties])
        active-property @(rf/subscribe [::ss/active-property])
        report-from-year @(rf/subscribe [::reps/report-year :from])
        report-from-month @(rf/subscribe [::reps/report-month :from])
        report-to-year @(rf/subscribe [::reps/report-year :to])
        report-to-month @(rf/subscribe [::reps/report-month :to])]
    {:property-id (or (-> property-id keyword)
                      active-property
                      (->> properties first :id)
                      "")
     :from-year (or (-> from-year keyword)
                    report-from-year
                    (:last-year shared/default-cal))
     :from-month (or (-> from-month keyword)
                     report-from-month
                     (:last-month shared/default-cal))
     :to-year (or (-> to-year keyword)
                  report-to-year
                  (:this-year shared/default-cal))
     :to-month (or (-> to-month keyword)
                   report-to-month
                   (:this-month shared/default-cal))}))

(defn calc-options-db
  [db {:keys [property-id year month charge-id]}]
  (let [properties (cs/get-properties db)
        active-property (get-in db [:site :active-property])
        reconcile-year (get-in db [:reconcile :year])
        reconcile-month (get-in db [:reconcile :month])]
    {:property-id (or (-> property-id keyword)
                      active-property
                      (->> properties first :id)
                      "")
     :year (or (-> year keyword)
               reconcile-year
               (:this-year shared/default-cal))
     :month (or (-> month keyword)
                reconcile-month
                (:this-month shared/default-cal))
     :charge-id (keyword charge-id)}))


(defn calc-options
  [{:keys [property-id year month charge-id]}]
  (let [properties @(rf/subscribe [::cs/properties])
        active-property @(rf/subscribe [::ss/active-property])
        reconcile-year @(rf/subscribe [::rs/reconcile-year])
        reconcile-month @(rf/subscribe [::rs/reconcile-month])]
    {:property-id (or (-> property-id keyword)
                      active-property
                      (->> properties first :id)
                      "")
     :year (or (-> year keyword)
               reconcile-year
               (:this-year shared/default-cal))
     :month (or (-> month keyword)
                reconcile-month
                (:this-month shared/default-cal))
     :charge-id (keyword charge-id)}))
