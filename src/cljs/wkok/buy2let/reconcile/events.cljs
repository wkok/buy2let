(ns wkok.buy2let.reconcile.events
  (:require
   [clojure.set :as set]
   [re-frame.core :as rf]
   [wkok.buy2let.backend.multimethods :as mm]
   [wkok.buy2let.db.default :as ddb]
   [wkok.buy2let.period :as period]
   [wkok.buy2let.shared :as shared]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.security :as sec]))


(defn download-month? [month db property year]
  (if (not (contains? (get-in db [:ledger property year]) month))
    [month]
    []))

(defn load-ledger-fx [db property year month]
  (if (not-any? nil? [property year month])
    (let [account-id (get-in db [:security :account])
          prev (period/prev-month month year)
          months (concat (download-month? month db property year)
                         (download-month? (:month prev) db property (:year prev)))]
      (if (empty? months)
        {:db db}
        (merge {:db             db}
               (mm/get-ledger-month-fx {:property property
                                        :account-id account-id
                                        :this-year year
                                        :this-month month
                                        :prev-year (:year prev)
                                        :prev-month (:month prev)
                                        :on-success-this #(rf/dispatch [:load-ledger-month %])
                                        :on-success-prev #(rf/dispatch [:load-ledger-month %])}))))
    {:db db}))


(rf/reg-event-db
 ::reconcile-view-toggle
 (fn [db [_ _]]
   (case (get-in db [:reconcile :view])
     :accounting (assoc-in db [:reconcile :view] :overview)
     (assoc-in db [:reconcile :view] :accounting))))


(rf/reg-event-fx
 ::reconcile-nav
 (fn [cofx [_ _]]
   (let [db (:db cofx)
         property (-> (get-in db [:site :active-property]) name)
         year (-> (get-in db [:reconcile :year]) name)
         month (-> (get-in db [:reconcile :month]) name)]
     (if (or (= :all property) (not property))
       (js/window.location.assign (str "#/reconcile"))
       (js/window.location.assign (str "#/reconcile/" property "/" month "/" year))))))


(rf/reg-event-db
 ::reconcile-set-property
 (fn [db [_ p]]
   (rf/dispatch [::reconcile-nav])
   (assoc-in db [:site :active-property] (keyword p))))


(rf/reg-event-db
 ::reconcile-set-year
 (fn [db [_ y]]
   (rf/dispatch [::reconcile-nav])
   (assoc-in db [:reconcile :year] (keyword y))))


(rf/reg-event-db
 ::reconcile-set-month
 (fn [db [_ m]]
   (rf/dispatch [::reconcile-nav])
   (assoc-in db [:reconcile :month] (keyword m))))

; Removes the invoice file blob as it is not stored in the database
; Sets the invoiced flag to true, only if invoiced is currently false & file was uploaded
(defn calc-breakdown [charges]
  (into {} (map #(hash-map (key %)
                           (let [tmp (dissoc (val %) :invoice :invoice-deleted)]
                             (if (:invoiced (val %))        ;Already uploaded
                               tmp
                               (assoc tmp :invoiced (not (nil? (:invoice (val %)))))))) charges)))

(defn calc-double-entries [property charge-id amount]
  (if (= :agent-opening-balance charge-id)
    {:agent-current {charge-id amount}
     :owner         {charge-id (* -1 amount)}}
    (if (= :tenant-opening-balance charge-id)
      {:tenant {charge-id amount}
       :owner  {charge-id (* -1 amount)}}
      (case (get-in (:charges property) [charge-id :who-pays-whom])
        :opa {:agent-current {charge-id amount}
              :owner         {charge-id (* -1 amount)}}
        :ac {:agent-commission {charge-id amount}
             :agent-current    {charge-id (* -1 amount)}}
        :apo {:owner         {charge-id amount}
              :agent-current {charge-id (* -1 amount)}}
        :aps {:supplier      {charge-id amount}
              :agent-current {charge-id (* -1 amount)}}
        :mi {:bank-interest {charge-id amount}
             :bank-current  {charge-id (* -1 amount)}}
        :opb {:bank-current {charge-id amount}
              :owner        {charge-id (* -1 amount)}}
        :ops {:supplier {charge-id amount}
              :owner    {charge-id (* -1 amount)}}
        :oct {:owner-control  {charge-id (* -1 amount)}
              :tenant  {charge-id amount}}
        :oca {:owner-control  {charge-id (* -1 amount)}
              :agent-current  {charge-id amount}}
        :tpa {:agent-current {charge-id amount}
              :tenant        {charge-id (* -1 amount)}}
        :tpo {:owner  {charge-id amount}
              :tenant {charge-id (* -1 amount)}}
        {}))))

(defn calc-accounting [property charges]
  (->> (map #(calc-double-entries property (key %) (:amount (val %))) charges)
       (reduce #(merge-with set/union %1 %2))))

(defn calc-totals [accounting]
  (into {} (map #(hash-map (first %) (shared/to-money (apply + (vals (second %))))) accounting)))

(defn add-opening-balances [breakdown prev-month]
  (-> (assoc-in breakdown [:agent-opening-balance :amount] (or (get-in prev-month [:totals :agent-current]) 0))
      (assoc-in [:tenant-opening-balance :amount] (or (get-in prev-month [:totals :tenant]) 0))))

(defn calc-yield-to-date
  [db property year month this-month-breakdown]
  (let [breakdown-total (shared/calc-breakdown-total-last-12-months db (:id property) year month this-month-breakdown)]
    {:net (shared/calc-yield breakdown-total (:purchase-price property))
     :roi (shared/calc-yield breakdown-total (:cash-invested property))}))

(defn as-data [db property year month values]
  (let [this-month-breakdown (-> (get-in values [:this-month :breakdown])
                                 (add-opening-balances (:prev-month values)))
        this-month-accounting (calc-accounting property this-month-breakdown)]
    {:accounting this-month-accounting
     :totals     (calc-totals this-month-accounting)
     :breakdown  (calc-breakdown this-month-breakdown)
     :yield (calc-yield-to-date db property year month this-month-breakdown)}))

; Transforms into a list of file blobs to upload to an object store (or delete from an object store)
(defn as-blobs [account property year month this-month-breakdown charges account-id]
  (->> (map (fn [charge] {:path       (shared/blob-key account (:id property) year month (-> (key charge) name))
                          :file       (:invoice (val charge))
                          :action     (if (:invoice (val charge))
                                        :put
                                        (when (:invoice-deleted (val charge))
                                          :delete))
                          ;; :on-progress #(.log js/console (str "Upload is " % "%"))
                          ;; :on-success #()
                          :on-error   #(rf/dispatch [::se/dialog {:heading "Oops!"
                                                                  :message (str "Error uploading invoice for: "
                                                                                (:name (shared/by-id (key charge) charges))
                                                                                " \nDetail: " %)}])
                          :metadata {:customMetadata {"accountId" account-id}}}) this-month-breakdown)
       (filter #(not (nil? (:action %))))))

(defn by-storage-type [db account property year month values charges account-id]
  (let [this-month (:this-month values)
        this-month-breakdown (:breakdown this-month)]
    {:data (as-data db property year month values)
     :blobs (as-blobs account property year month this-month-breakdown charges account-id)}))

(rf/reg-event-db
 ::edit-reconcile
 (fn [db [_ role options]]
   (sec/with-authorisation role db
     #(let [{:keys [property-id year month]} (ddb/calc-options-db db options)]
        (-> (assoc-in db [:reconcile :month] month)
            (assoc-in [:reconcile :year] year)
            (assoc-in [:site :active-property] property-id)
            (assoc-in [:site :active-page] :reconcile)
            (assoc-in [:site :active-panel] :reconcile-edit)
            (assoc-in [:site :heading] "Reconcile"))))))

(rf/reg-event-fx
 ::view-reconcile
 (fn [{:keys [db]} [_ role options]]
   (sec/with-authorisation role db
     #(let [{:keys [property-id year month]} (ddb/calc-options-db db options)
            new-db (-> (assoc-in db [:reconcile :month] month)
                       (assoc-in [:reconcile :year] year)
                       (assoc-in [:site :active-property] property-id)
                       (assoc-in [:site :active-page] :reconcile)
                       (assoc-in [:site :active-panel] :reconcile-view)
                       (assoc-in [:site :heading] "Reconcile"))]
        (if (or (= :all property-id) (not property-id))
          {:db new-db}
          (load-ledger-fx new-db property-id year month))))))

(rf/reg-event-fx
 ::save-reconcile
 (fn [cofx [_ ledger]]
   (let [db (:db cofx)
         account-id (get-in db [:security :account])
         charges (->> (:charges db)
                      vals
                      (filter #(not (:hidden %)))
                      (sort-by :name))
         properties (->> (:properties db)
                         vals
                         (filter #(not (:hidden %)))
                         (sort-by :name))
         property (shared/by-id (get-in db [:site :active-property]) properties)
         year (-> (get-in db [:reconcile :year]) keyword)
         month (-> (get-in db [:reconcile :month]) keyword)
         values (shared/apply-breakdown ledger js/parseFloat)
         charges-this-month (by-storage-type db account-id property year month values charges account-id)]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (if (not-any? nil? [(:id property) year month charges-this-month])
       (merge {:db              (-> (assoc-in db [:ledger (:id property) year month] (:data charges-this-month))
                                    (assoc-in [:site :active-panel] :reconcile-view))}
              (mm/save-reconcile-fx {:account-id account-id
                                     :property-id (:id property)
                                     :year year
                                     :month month
                                     :charges-this-month charges-this-month}))
       {:db db}))))
