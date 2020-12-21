(ns wkok.buy2let.dashboard.views
  (:require [re-frame.core :as rf]
            [clojure.string :as s]
            [reagent.core :as ra]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.charts :as charts]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.dashboard.events :as de]
            [wkok.buy2let.dashboard.subs :as ds]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.db.subs :as dbs]
            [tick.alpha.api :as t]
            [cljc.java-time.month :as tm]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.box :refer [box]]
            [reagent-material-ui.core.switch-component :refer [switch]]
            [reagent-material-ui.core.form-control-label :refer [form-control-label]]
            [reagent-material-ui.core.card-actions :refer [card-actions]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.card-content :refer [card-content]]))

(defn dashboard []
  (rf/dispatch [:set-fab-actions nil])
  (let [incl-this-month @(rf/subscribe [::ds/incl-this-month])
        today (t/- (t/today) (t/new-period (if incl-this-month 0 1) :months))
        today-month (-> (t/today) t/month str (subs 0 3) s/capitalize)
        last (t/- today (t/new-period 11 :months))
        this-year (-> today t/year str keyword)
        this-month (-> today t/month tm/ordinal inc str keyword)
        last-year (-> last t/year str keyword)
        last-month (-> last t/month tm/ordinal inc str keyword)
        months (shared/month-range {:year last-year :month last-month}
                                   {:year this-year :month this-month})
        properties @(rf/subscribe [::cs/properties])
        property (shared/by-id @(rf/subscribe [::ss/active-property]) properties)
        ledger @(rf/subscribe [::dbs/ledger])
        data (->> months
                  (map #(assoc % :profit (shared/calc-profit-total ledger % properties property)))
                  (map (fn [m] [(shared/format-month (:date m))
                                (:profit m)
                                (if (neg? (:profit m)) "color: red" "color: blue")
                                (when (and (= (:month m) this-month)
                                           (= (:year m) this-year))
                                  (shared/format-money (:profit m)))]))
                  (concat [["Month" "Profit / loss" {:type :string :role :style} {:type :string :role :annotation}]]))]
    [card
     [card-content
      [grid {:container true
             :direction :row
             :justify :space-between}
       [grid {:item true}
        [typography {:color :textSecondary} "Monthly profit / loss"]]
       [grid {:item true}
        [text-field {:select true
                     :label ""
                     :field     :list
                     :on-change #(rf/dispatch [::se/set-active-property (.. % -target -value)])
                     :value     (or @(rf/subscribe [::ss/active-property]) :all)}
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
             :justify :flex-end}
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
           :label-placement :start}]]]]]]))

