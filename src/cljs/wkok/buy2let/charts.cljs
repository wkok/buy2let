(ns wkok.buy2let.charts
  (:require [reagent.core :as ra]))

(defonce ready?
         (ra/atom false))

(defonce initialize
         (do
           (js/google.charts.load (clj->js {:packages ["corechart"]}))
           (js/google.charts.setOnLoadCallback
             (fn google-visualization-loaded []
               (reset! ready? true)))))

(defn data-table [data]
  (cond
    (map? data) (js/google.visualization.DataTable. (clj->js data))
    (string? data) (js/google.visualization.Query. data)
    (seqable? data) (js/google.visualization.arrayToDataTable (clj->js data))))

(defn draw-chart [chart-type data options]
  (if @ready?
    [:div
     {:ref
      (fn [this]
        (when this
          (.draw (new (aget js/google.visualization chart-type) this)
                 (data-table data)
                 (clj->js options))))}]
    [:div "Loading..."]))