(ns wkok.buy2let.period
  (:require [tick.core :as t]
            [cljc.java-time.month :as tm]))

(defn prev-month [month year]
  (let [this-month (-> (name month) js/parseInt)
        as-date (t/new-date (-> year name js/parseInt) this-month 15)
        prev (t/<< as-date (t/new-period 1 :months))]
    {:month (-> prev t/month tm/ordinal inc str keyword)
     :year  (-> prev t/year str keyword)}))

(defn year-month->inst
  [year month]
  (-> (t/new-date (name year) (name month) 15)
    (t/at "00:00")
    (t/in "UTC")
    t/inst))

(defn date->month
  [date]
  (-> date .getTime t/instant t/month t/fields :month-of-year str keyword))

(defn date->year
  [date]
  (-> date .getTime t/instant t/year str keyword))
