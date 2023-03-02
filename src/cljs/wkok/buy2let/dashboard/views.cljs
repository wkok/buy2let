(ns wkok.buy2let.dashboard.views
  (:require [re-frame.core :as rf]
            [clojure.string :as s]
            [reagent.core :as ra]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.charts :as charts]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.dashboard.events :as de]
            [wkok.buy2let.dashboard.subs :as ds]
            [wkok.buy2let.db.subs :as dbs]
            [tick.core :as t]
            [cljc.java-time.month :as tm]
            [reagent-mui.material.typography :refer [typography]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.text-field :refer [text-field]]
            [reagent-mui.material.card :refer [card]]
            [reagent-mui.material.box :refer [box]]
            [reagent-mui.material.switch :refer [switch]]
            [reagent-mui.material.form-control-label :refer [form-control-label]]
            [reagent-mui.material.card-actions :refer [card-actions]]
            [reagent-mui.material.menu-item :refer [menu-item]]
            [reagent-mui.material.card-content :refer [card-content]]))

(defn chart-data
  [{:keys [property-id months ledger properties this-month this-year]}]
  (->> months
       (map #(assoc % :profit (shared/calc-profit-total ledger % properties property-id)))
       (map (fn [m] [(shared/format-month (:date m))
                     (:profit m)
                     (if (neg? (:profit m)) "color: red" "color: blue")
                     (when (and (= (:month m) this-month)
                                (= (:year m) this-year))
                       (shared/format-money (:profit m)))]))
       (concat [["Month" "Profit / loss" {:type :string :role :style} {:type :string :role :annotation}]])))

(defn monthly-profit-card
  [{:keys [currency data properties incl-this-month today-month]}]
  [grid {:item true}
   [card
    [card-content
     [grid {:container true
            :direction :row
            :justify-content :space-between}
      [grid {:item true}
       [typography {:color :textSecondary} (if (= :none currency)
                                             "Monthly profit / loss"
                                             (str "Monthly profit / loss (" currency ")"))]]
      [grid {:item true}
       [text-field {:select true
                    :variant :standard
                    :label ""
                    :field     :list
                    :on-change #(rf/dispatch [::de/set-active-property (keyword currency) (.. % -target -value)])
                    :value     (or @(rf/subscribe [::ds/active-property (keyword currency)]) :all)}
        [menu-item {:value :all} "All properties"]
        (map (fn [property]
               ^{:key property}
               [menu-item {:value (:id property)}
                (:name property)]) properties)]]]
     [charts/draw-chart
      "LineChart"
      data
      {:legend {:position :none}
       :chartArea {:width "80%"
                   :left  "15%"}
       :curveType :function}]]
    [card-actions
     [grid {:container true
            :justify-content :flex-end}
      [grid {:item true}
       [box {:mr 1}
        [form-control-label
         {:control (ra/as-element
                    [switch {:color :primary
                             :on-change #(rf/dispatch [::de/incl-this-month (not incl-this-month)])
                             :checked incl-this-month}])
          :label (ra/as-element
                  [typography {:variant :body2}
                   (str "Include " today-month)])
          :label-placement :start}]]]]]]])

(defn filter-ledger [ledger properties]
  (select-keys ledger (map #(:id %) properties)))

(defn dashboard []
  (rf/dispatch [:set-fab-actions nil])
  (let [incl-this-month @(rf/subscribe [::ds/incl-this-month])
        today (t/<< (t/today) (t/new-period (if incl-this-month 0 1) :months))
        today-month (-> (t/today) t/month str (subs 0 3) s/capitalize)
        last (t/<< today (t/new-period 11 :months))
        this-year (-> today t/year str keyword)
        this-month (-> today t/month tm/ordinal inc str keyword)
        last-year (-> last t/year str keyword)
        last-month (-> last t/month tm/ordinal inc str keyword)
        months (shared/month-range {:year last-year :month last-month}
                                   {:year this-year :month this-month})
        properties-by-currency (group-by :currency @(rf/subscribe [::cs/properties]))
        ledger @(rf/subscribe [::dbs/ledger])
        data-by-currency (->> (map (fn [[currency properties]]
                                     {:currency (or currency :none)
                                      :properties properties
                                      :data (chart-data {:property-id @(rf/subscribe [::ds/active-property (or (keyword currency) :none)])
                                                         :months months
                                                         :ledger (filter-ledger ledger properties)
                                                         :properties properties
                                                         :this-month this-month
                                                         :this-year this-year})})
                                   properties-by-currency)
                              doall)]

    [grid {:container true
           :direction :column
           :spacing 2}
     (for [data data-by-currency]
       ^{:key (or (:currency data) :none)}
       [grid {:item true}
        [monthly-profit-card {:currency (or (:currency data) :none)
                              :data (:data data)
                              :properties (:properties data)
                              :incl-this-month incl-this-month
                              :today-month today-month}]])]))
