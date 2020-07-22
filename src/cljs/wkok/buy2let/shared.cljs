(ns wkok.buy2let.shared
  (:require [nano-id.core :as nid]
            [re-frame.core :as rf]
            [clojure.walk :as w]
            [clojure.string :as s]
            [goog.string :as gstring]
            [cljc.java-time.month :as tm]
            [tick.alpha.api :as t]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.backend.protocol :as bp]
            [wkok.buy2let.backend.impl :as impl]
            [reagent.core :as ra]))

(defn gen-id []
  (-> (nid/nano-id) keyword))

(rf/reg-cofx
 ::gen-id
 (fn [cofx _]
   (assoc cofx :id (gen-id))))

(defn keywordize-id [data]
  (if (not (nil? (:id data)))
    (assoc data :id (-> (:id data) keyword))
    data))

(defn keywordize [result]
  (let [data (:data result)]
    (when (not (nil? data))
      (-> data
          w/keywordize-keys
          (keywordize-id)))))


(defn to-crud [results]
  (->> (map #(keywordize %) (:docs results))
       (group-by :id)
       (w/walk (fn [x] {(key x) (into {} (val x))}) identity)))


(defn keywordize-col [crud col-key]
  (->> (map (fn [x] (let [v (val x)
                          k (key x)]
                      {k (assoc v col-key
                                (map #(keyword %) (col-key v)))})) crud)
       (into {})))

(defn by-id [id coll]
  (some #(when (= (:id %) id) %) coll))


(defn format-money [amount]
  (if (not (nil? amount))
    (gstring/format "%.2f" amount)
    "0.00"))

(defn to-money [amount]
  (-> (format-money amount)
      js/parseFloat))

(defn prev-month [month year]
  (let [this-month (-> (name month) js/parseInt)
        as-date (t/new-date (-> year name js/parseInt) this-month 1)
        prev (t/- as-date (t/new-period 1 :months))]
    {:month (-> prev t/month tm/ordinal inc str keyword)
     :year  (-> prev t/year str keyword)}))

(defn select-property [properties on-change value select-text]
  [:label [:strong "Property:"]
   [:select {:field     :list
             :on-change on-change
             :value     value}
    [:option {:value "--select--"} select-text]
    (->> (filter #(not (:hidden %)) properties)
         (map (fn [property]
                ^{:key property}
                [:option {:value (:id property)}
                 (:name property)])))]])

(defn select-year [on-change value]
  (let [second-last-year (-> t/today t/year js/parseInt dec dec str keyword)
        last-year (-> t/today t/year js/parseInt dec str keyword)
        this-year (-> t/today t/year str keyword)
        next-year (-> t/today t/year js/parseInt inc str keyword)]
    [:select {:field     :list
              :on-change on-change
              :value     value}
     [:option {:value second-last-year} second-last-year]
     [:option {:value last-year} last-year]
     [:option {:value this-year} this-year]
     [:option {:value next-year} next-year]]))

(defn select-month [on-change value]
  [:select {:field     :list
            :on-change on-change
            :value     value}
   [:option {:value 1} "Jan"]
   [:option {:value 2} "Feb"]
   [:option {:value 3} "Mar"]
   [:option {:value 4} "Apr"]
   [:option {:value 5} "May"]
   [:option {:value 6} "Jun"]
   [:option {:value 7} "Jul"]
   [:option {:value 8} "Aug"]
   [:option {:value 9} "Sep"]
   [:option {:value 10} "Oct"]
   [:option {:value 11} "Nov"]
   [:option {:value 12} "Dec"]])


(defn month-range [from to]
  (let [from (t/new-date (-> (:year from) name js/parseInt)
                         (-> (:month from) name js/parseInt) 1)
        to (t/new-date (-> (:year to) name js/parseInt)
                       (-> (:month to) name js/parseInt) 1)
        interval (when (>= to from)
                   (t/new-interval from to))]
    (when (not (nil? interval))
      (->> (t/range (t/beginning interval)
                    (t/end interval)
                    (t/new-period 1 :months))
           (map (fn [d] {:month (-> (t/month d) tm/ordinal inc str keyword)
                         :year  (-> (t/year d) str keyword)
                         :date  d}))))))

(defn format-month [date]
  (-> date
      t/month
      str
      (subs 0 3)
      s/capitalize))

(defn calc-profit-property [property-ledger m]
  (-> (+ (get-in property-ledger [(:year m) (:month m) :totals :owner])
         (get-in property-ledger [(:year m) (:month m) :totals :agent-current]))
      to-money))

(defn hidden? [property-id properties]
  (-> (by-id property-id properties)
      (get :hidden false)))

(defn calc-profit-total [ledger m properties property]
  (apply + (map #(calc-profit-property ((first %) ledger) m)
                (filter #(and (not (hidden? (key %) properties))
                              (if property
                                (= (key %) (:id property))
                                true)) ledger))))

(defn blob-key [account property year month charge-id]
  (str "data/" (name account) "/ledger/" (name property) "/" (name year) "/" (name month) "/" charge-id))

(rf/reg-event-fx
 ::view-invoice
 (fn [cofx [_ property-id year month charge]]
   (let [db (:db cofx)
         account-id (get-in db [:security :account])
         path (blob-key account-id property-id year month (name (:id charge)))]
     
     (bp/blob-url-fx impl/backend path
                         #(js/window.open %)
                         #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                     :message %}])))))


(defn get-ledger-fx [db property months]
  (let [account-id (get-in db [:security :account])
        downloaded? #(contains? (get-in db [:ledger property %1]) %2)
        m (filter #(not (downloaded? (:year %) (:month %))) months)]
    (when (not (empty? m))
      (bp/get-ledger-fx impl/backend property account-id m))))

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
 (fn [db [_ property year month result]]
   (if (not (nil? (:data result)))
     (-> (assoc-in db [:ledger property year month] (keywordize result))
         (assoc-in [:site :show-progress] false)
         calc-totals)
     (assoc-in db [:site :show-progress] false))))


; Some backends calls set-backend-user twice
; This atom effectively allows processing of only one call when user successfully authenticated
(defonce set-backend-user-called? (ra/atom false))

(defn authenticated? [auth]
  (not (nil? auth)))

(rf/reg-event-db
 :set-backend-user
 (fn [db [_ auth]]
   (when (and (authenticated? auth)
              (not @set-backend-user-called?))
     (reset! set-backend-user-called? true)
     (rf/dispatch [:get-user auth]))
   (assoc-in db [:security :auth] auth)))
