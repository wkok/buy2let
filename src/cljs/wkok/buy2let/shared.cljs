(ns wkok.buy2let.shared
  (:require [nano-id.core :as nid]
            [re-frame.core :as rf]
            [clojure.string :as s]
            [goog.string :as gstring]
            goog.string.format   ; https://clojurescript.org/reference/google-closure-library#requiring-a-function
            [cljc.java-time.month :as tm]
            [tick.alpha.api :as t]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.backend.protocol :as bp]
            [wkok.buy2let.backend.impl :as impl]
            [wkok.buy2let.spec :as spec]))


(def default-cal
  (let [today (t/today)
        last (t/- today (t/new-period 2 :months))]

    {:today today
     :last last
     :this-year (-> today t/year str keyword)
     :this-month (-> today t/month tm/ordinal inc str keyword)
     :last-year (-> last t/year str keyword)
     :last-month (-> last t/month tm/ordinal inc str keyword)}))


(def default-db
  {:site      {:heading      "Dashboard"
               :show-progress true
               :active-property      "--select--"
               :active-page :dashboard
               :splash true}
   :report    {:show-invoices false}
   :charges   {:agent-opening-balance {:id       :agent-opening-balance
                                       :name     "Opening balance"
                                       :reserved true}}})


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

     (bp/blob-url-fx impl/backend
                     {:path path
                      :on-success #(js/window.open %)
                      :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                            :message %}])}))))


(defn get-ledger-fx [db property months]
  (let [account-id (get-in db [:security :account])
        downloaded? #(contains? (get-in db [:ledger property %1]) %2)
        m (filter #(not (downloaded? (:year %) (:month %))) months)]
    (when (seq m)
      (bp/get-ledger-fx impl/backend 
                        {:property property 
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


; See: https://github.com/facebook/react/issues/16382
(defn anchor
  ([on-click label]
   [:a {:href "#" :on-click #(do (.preventDefault %)
                                 (on-click))}
    [:label label]])
  ([on-click label class]
   [:a {:class class
        :href "#" :on-click #(do (.preventDefault %)
                                 (on-click))}
    [:label label]]))


