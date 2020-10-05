(ns wkok.buy2let.dashboard.views
  (:require [re-frame.core :as rf]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.charts :as charts]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.db.subs :as dbs]
            [tick.alpha.api :as t]
            [cljc.java-time.month :as tm]
            [reagent-material-ui.core.typography :refer [typography]]))

(defn dashboard []
  (rf/dispatch [:set-fab-actions nil])
  (let [today (t/- (t/today) (t/new-period 1 :months))
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
    [:div
     [typography {:variant :h6} "Monthly profit / loss"]
     (shared/select-property properties
                             #(rf/dispatch [::se/set-active-property (.. % -target -value)])
                             @(rf/subscribe [::ss/active-property])
                             "All properties")
     [charts/draw-chart
      "LineChart"
      data
      {:legend {:position :none}
       :chartArea {:width "80%"
                   :left  "15%"}
       :curveType :function}]]))

