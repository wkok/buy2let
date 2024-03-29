(ns wkok.buy2let.shared
  (:require
   [cemerick.url :as url]
   [cljc.java-time.month :as tm]
   [clojure.string :as s]
   [goog.string :as gstring]
   goog.string.format ; https://clojurescript.org/reference/google-closure-library#requiring-a-function
   [nano-id.core :as nid]
   [re-frame.core :as rf]
   [tick.alpha.interval :as t.i]
   [tick.core :as t]
   [wkok.buy2let.account.subs :as as]
   [wkok.buy2let.security :as sec]
   [wkok.buy2let.backend.multimethods :as mm]
   [wkok.buy2let.backend.subs :as bs]
   [wkok.buy2let.period :as period]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.spec :as spec]))

(def dev-mode?
  ^boolean goog.DEBUG)

(def default-cal
  (let [today (t/today)
        last (t/<< today (t/new-period 2 :months))]

    {:today today
     :last last
     :this-year (-> today t/year str keyword)
     :this-month (-> today t/month tm/ordinal inc str keyword)
     :last-year (-> last t/year str keyword)
     :last-month (-> last t/month tm/ordinal inc str keyword)}))

(defn gen-id []
  (-> (nid/nano-id) keyword))

(rf/reg-cofx
 ::gen-id
 (fn [cofx _]
   (assoc cofx :id (gen-id))))

(defn by-id [id coll]
  (some #(when (= (:id %) id) %) coll))


(defn format-money [amount]
  (if (not (nil? amount))
    (gstring/format "%.2f" amount)
    "0.00"))

(defn to-money [amount]
  (-> (format-money amount)
      js/parseFloat))

(defn month-range [from to]
  (when (and (:year from) (:month from))
    (let [from (t/new-date (-> (:year from) name js/parseInt)
                           (-> (:month from) name js/parseInt) 15)
          to (t/new-date (-> (:year to) name js/parseInt)
                         (-> (:month to) name js/parseInt) 15)
          interval (when (t/>= to from)
                     (t.i/new-interval from to))]
      (when (not (nil? interval))
        (->> (t/range (t/beginning interval)
                      (t/end interval)
                      (t/new-period 1 :months))
             (map (fn [d] {:month (-> (t/month d) tm/ordinal inc str keyword)
                           :year  (-> (t/year d) str keyword)
                           :date  d})))))))

(defn format-month [date]
  (-> date
      t/month
      str
      (subs 0 3)
      s/capitalize))

(defn calc-profit-property [property-ledger m]
  (-> (+ (get-in property-ledger [(:year m) (:month m) :totals :owner])
         (get-in property-ledger [(:year m) (:month m) :totals :agent-current])
         (get-in property-ledger [(:year m) (:month m) :totals :tenant]))
      to-money))

(defn hidden? [property-id properties]
  (-> (by-id property-id properties)
      (get :hidden false)))

(defn calc-profit-total [ledger m properties property-id]
  (apply + (map #(calc-profit-property ((first %) ledger) m)
                (filter #(and (not (hidden? (key %) properties))
                              (if (or (nil? property-id) (= :all property-id))
                                true
                                (= (key %) property-id))) ledger))))

(defn blob-key [account property year month charge-id]
  (str "data/" (name account) "/ledger/" (name property) "/" (name year) "/" (name month) "/" charge-id))

(rf/reg-event-fx
 ::view-attachment
 (fn [cofx [_ collection id]]
   (let [db (:db cofx)
         account-id (get-in db [:security :account])
         path (str "data/" (name account-id) "/" (name collection) "/" (name id))]

     (mm/blob-url-fx {:path path
                      :on-success #(js/window.open %)
                      :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                            :message %}])}))))


(defn get-ledger-fx [db property months]
  (let [account-id (get-in db [:security :account])
        downloaded? #(contains? (get-in db [:ledger property %1]) %2)
        m (filter #(not (downloaded? (:year %) (:month %))) months)]
    (when (seq m)
      (mm/get-ledger-fx {:property property
                         :account-id account-id
                         :months m
                         :on-success #(rf/dispatch [:load-ledger-month %])}))))

(defn add-breakdown-amounts [left right]
  {:amount (-> (+ ((fnil identity 0) (:amount left))
                  ((fnil identity 0) (:amount right)))
               to-money)})

(defn add-accounting-amounts [left right]
  (-> (+ ((fnil identity 0) left)
         ((fnil identity 0) right))
      to-money))

(defn add-breakdown-charges [left right]
  (merge-with add-breakdown-amounts left right))

(defn add-accounting-totals [left right]
  (merge-with add-accounting-amounts left right))

(defn calc-accounting-totals [db property months]
  (->> (map #(get-in db [:ledger property (:year %) (:month %) :totals]) months)
       (reduce add-accounting-totals)))

(defn calc-breakdown-totals [db property months]
  (->> (map #(get-in db [:ledger property (:year %) (:month %) :breakdown]) months)
       (reduce add-breakdown-charges)))

(defn calc-totals [db]
  (let [months (get-in db [:report :result :months])
        property (get-in db [:site :active-property])]
    (-> (assoc-in db [:report :result :totals :breakdown] (calc-breakdown-totals db property months))
        (assoc-in [:report :result :totals :accounting] (calc-accounting-totals db property months)))))


(rf/reg-event-db
 :load-ledger-month
 (fn [db [_ input]]
   (let [{:keys [property-id year month ledger]} (spec/conform ::spec/ledger-month input)]
     (if (not (nil? ledger))
       (-> (assoc-in db [:ledger property-id year month] ledger)
           (assoc-in [:site :show-progress] false)
           calc-totals)
       (assoc-in db [:site :show-progress] false)))))

(defn has-role [role]
  (let [claims @(rf/subscribe [::bs/claims])
        account-id @(rf/subscribe [::as/account])]
    (sec/has-role role claims account-id)))

(defn accounts-from [roles]
  (->> (map #(val %) roles)
       (reduce concat)
       distinct))

(defn apply-breakdown [ledger applicator]
  (let [breakdown (-> ledger :this-month :breakdown)
        applicated (->> (map (fn [[charge-id {:keys [amount] :as detail}]]
                               (if amount
                                 {charge-id (update detail :amount applicator amount)}
                                 {charge-id (assoc detail :amount 0)})) breakdown)
                        (into {}))]
    (assoc-in ledger [:this-month :breakdown] applicated)))

(defn select-property-val [active-property properties]
  (case active-property
    :all ""
    nil ""
    (if (empty? (filter #(= active-property (:id %)) properties))
      ""
      active-property)))

(defn select-default-property [active-property properties event]
  (when (and (or (= :all active-property)
                 (not active-property)
                 (empty? (filter #(= active-property (:id %)) properties)))
             (seq properties))
    (rf/dispatch [event (-> properties first :id)])))

(defn validate-file-size [file max-bytes]
  (let [file-bytes (.-size file)
        file-mega-bytes (-> file-bytes (/ 1000) (/ 1000))
        file-mega-bytes-str (gstring/format "%.3f" file-mega-bytes)
        max-mega-bytes (-> max-bytes (/ 1000) (/ 1000))
        max-mega-bytes-str (gstring/format "%.0f" max-mega-bytes)]
    (if (> file-bytes max-bytes)
      (do (rf/dispatch [::se/dialog {:heading   "File too large"
                                     :message   (str "Please keep the file size below "
                                                     max-mega-bytes-str " MB. "
                                                     "(This file is " file-mega-bytes-str " MB)")
                                     :buttons   {:middle {:text     "OK"
                                                          :on-click #(rf/dispatch [::se/dialog])}}
                                     :closeable false}])
          false)
      true)))

(defn url-location []
  (-> js/window .-location))

(defn url-host []
  (let [location (url-location)]
    (str (-> location .-protocol) "//"
         (-> location .-host))))

(defn url-full-href []
  (-> (url-location) .-href))

(defn url-full []
  (-> (url-full-href) url/url))

(defn url-hash []
  (-> (url-location) .-hash))

(defn url-param
  ([param]
   (url-param param nil))
  ([param default]
   (get-in (url-full) [:query param] default)))

(rf/reg-event-fx
 ::log-analytics
 (fn [_ [_ options]]
   (mm/log-analytics options)))

(defn filter-charge-invoices
  [db options]
  (let [property-id (-> db :site :active-property)
        year (or (:year options) (-> db :reconcile :year))
        month (or (:month options) (-> db :reconcile :month))
        charge-id (or (:charge-id options) (-> db :reconcile :charge-id))]
    (->> (filter (fn [[_k v]]
                   (and (= property-id (:property-id v))
                        (= year (:year v))
                        (= month (:month v))
                        (= charge-id (:charge-id v))))
                 (:invoices db)))))


;; Temporary to access legacy attachments, remove in 2023
(rf/reg-event-fx
 ::view-invoice
 (fn [cofx [_ property-id year month charge]]
   (let [db (:db cofx)
         account-id (get-in db [:security :account])
         path (blob-key account-id property-id year month (name (:id charge)))]

     (mm/blob-url-fx {:path path
                      :on-success #(js/window.open %)
                      :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                            :message %}])}))))

(defn calc-yield
  [breakdown initial-amount]
  (let [rental-income (or (get-in breakdown [:rent-charged-id :amount])
                          (get-in breakdown [:tSRksQUGeo3LagAPtH7gg :amount]))
        expense-keys (->> breakdown
                          keys
                          (remove #{:agent-opening-balance :mortgage-interest-id :payment-received-id :rent-charged-id
                                    :ngOTXdFnbN2HBaryJUSTR :aBm7Q87rGIqccqmZfWTu8 :tSRksQUGeo3LagAPtH7gg}))
        expenses-total (->> (select-keys breakdown expense-keys)
                            (map #(-> % val :amount))
                            (reduce +))
        net-income (- rental-income expenses-total)]
    (if (pos? initial-amount)
      (-> (/ net-income initial-amount)
          (* 100))
      0)))

(defn calc-breakdown-total-last-12-months
  [db property-id year month end-month-breakdown]
  (let [prev (period/prev-month month year)
        months-range (period/last-11-months (:year prev) (:month prev))]
    (add-breakdown-charges
     (calc-breakdown-totals db property-id months-range)
     end-month-breakdown)))
