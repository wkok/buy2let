(ns wkok.buy2let.reconcile.events
  (:require
   [re-frame.core :as rf]
   [clojure.set :as set]
   [wkok.buy2let.shared :as shared]
   [wkok.buy2let.period :as period]
   [wkok.buy2let.crud.subs :as cs]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.backend.multimethods :as mm]
   [wkok.buy2let.site.subs :as ss]
   [wkok.buy2let.reconcile.subs :as rs]
   [wkok.buy2let.account.subs :as as]))


(defn download-month? [month db property year]
  (if (not (contains? (get-in db [:ledger property year]) month))
    [month]
    []))

(defn load-ledger-fx [db property year month]
  (if (not-any? nil? [property year month])
    (let [account-id @(rf/subscribe [::as/account])
          prev (period/prev-month month year)
          months (concat (download-month? month db property year)
                         (download-month? (:month prev) db property (:year prev)))]
      (if (empty? months)
        {:db db}
        (merge {:db             db}
               (mm/get-ledger-month-fx {                  :property property 
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
     (if (= "--select--" property)
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


(defn blob-key [account property year month charge-id]
  (str "data/" (name account) "/ledger/" (name property) "/" (name year) "/" (name month) "/" charge-id))

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
      :tpa {:agent-current {charge-id amount}
            :tenant        {charge-id (* -1 amount)}}
      :tpo {:owner  {charge-id amount}
            :tenant {charge-id (* -1 amount)}}
      {})))

(defn calc-accounting [property charges]
  (->> (map #(calc-double-entries property (key %) (:amount (val %))) charges)
       (reduce #(merge-with set/union %1 %2))))

(defn calc-totals [accounting]
  (into {} (map #(hash-map (first %) (shared/to-money (apply + (vals (second %))))) accounting)))

(defn add-opening-balances [breakdown prev-month]
  (assoc-in breakdown [:agent-opening-balance :amount] (or (get-in prev-month [:totals :agent-current]) 
                                                           0)))

(defn as-data [property values]
  (let [this-month-breakdown (-> (get-in values [:this-month :breakdown])
                                 (add-opening-balances (:prev-month values)))
        this-month-accounting (calc-accounting property this-month-breakdown)]
    {:accounting this-month-accounting
     :totals     (calc-totals this-month-accounting)
     :breakdown  (calc-breakdown this-month-breakdown)}))

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

(defn by-storage-type [account property year month values charges account-id]
  (let [this-month (:this-month values)
        this-month-breakdown (:breakdown this-month)]
    {:data (as-data property values)
     :blobs (as-blobs account property year month this-month-breakdown charges account-id)}))

(rf/reg-event-db
 ::edit-reconcile
 (fn [db [_ options]]
   (let [{:keys [property-id year month]} options]
     (-> (assoc-in db [:reconcile :month] month)
         (assoc-in [:reconcile :year] year)
         (assoc-in [:site :active-property] property-id)
         (assoc-in [:site :active-page] :reconcile)
         (assoc-in [:site :active-panel] :reconcile-edit)
         (assoc-in [:site :heading] "Reconcile")))))

(rf/reg-event-fx
 ::view-reconcile
 (fn [cofx [_ options]]
   (let [{:keys [property-id year month]} options
         db (-> (assoc-in (:db cofx) [:reconcile :month] month)
                (assoc-in [:reconcile :year] year)
                (assoc-in [:site :active-property] property-id)
                (assoc-in [:site :active-page] :reconcile)
                (assoc-in [:site :active-panel] :reconcile-view)
                (assoc-in [:site :heading] "Reconcile"))]
     (if (not (= :--select-- property-id))
       (load-ledger-fx db property-id year month)
       {:db db}))))

(rf/reg-event-fx
 ::save-reconcile
 (fn [cofx [_ values]]
   (let [db (:db cofx)
         account-id @(rf/subscribe [::as/account])
         charges @(rf/subscribe [::cs/charges])
         properties @(rf/subscribe [::cs/properties])
         property (shared/by-id (get-in db [:site :active-property]) properties)
         year (-> (get-in db [:reconcile :year]) keyword)
         month (-> (get-in db [:reconcile :month]) keyword)
         charges-this-month (by-storage-type account-id property year month values charges account-id)]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (if (not-any? nil? [(:id property) year month charges-this-month])
       (merge {:db              (-> (assoc-in db [:ledger (:id property) year month] (:data charges-this-month))
                                    (assoc-in [:site :active-panel] :reconcile-view))}
              (mm/save-reconcile-fx {               :account-id account-id
                                     :property-id (:id property)
                                     :year year
                                     :month month
                                     :charges-this-month charges-this-month}))
       {:db db}))))


(defn calc-options
  [{:keys [property-id year month]}]
  (let [properties @(rf/subscribe [::cs/properties])
        active-property (let [ap @(rf/subscribe [::ss/active-property])]
                          (if (= "--select--" ap) nil ap))
        reconcile-year @(rf/subscribe [::rs/reconcile-year])
        reconcile-month @(rf/subscribe [::rs/reconcile-month])]
    {:property-id (or (-> property-id keyword)
                      active-property
                      (->> properties first :id)
                      :--select--)
     :year (or (-> year keyword)
               reconcile-year
               (:this-year shared/default-cal))
     :month (or (-> month keyword)
                reconcile-month
                (:this-month shared/default-cal))}))