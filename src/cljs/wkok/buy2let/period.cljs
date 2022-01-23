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

(defn last-12-months
  [year month]
  (let [end-date (-> (t/new-date (-> year name js/parseInt)
                                 (-> month name js/parseInt) 15)
                     (t/at "11:00")
                     (t/>> (t/new-period 1 :months)))
        start-date (t/<< end-date
                         (t/new-period 12 :months))
        range (t/range start-date end-date (t/new-period 1 :months))]
    (map (fn [m]
           {:month (-> (t/month m) t/fields :month-of-year str keyword)
            :year (-> (t/year m) str keyword)}) range)))
