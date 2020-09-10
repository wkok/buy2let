(ns wkok.buy2let.period
  (:require [tick.alpha.api :as t]
            [cljc.java-time.month :as tm]))

(defn prev-month [month year]
  (let [this-month (-> (name month) js/parseInt)
        as-date (t/new-date (-> year name js/parseInt) this-month 1)
        prev (t/- as-date (t/new-period 1 :months))]
    {:month (-> prev t/month tm/ordinal inc str keyword)
     :year  (-> prev t/year str keyword)}))