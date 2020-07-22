(ns wkok.buy2let.db.db
  (:require [tick.alpha.api :as t]
            [cljc.java-time.month :as tm]))

(def default-db
  (let [today (t/today)
        last (t/- today (t/new-period 2 :months))
        this-year (-> today t/year str keyword)
        this-month (-> today t/month tm/ordinal inc str keyword)
        last-year (-> last t/year str keyword)
        last-month (-> last t/month tm/ordinal inc str keyword)]
    {:site      {:heading      "Dashboard"
                 :show-progress true
                 :active-property      "--select--"
                 :active-page :dashboard}
     :reconcile {:year     this-year
                 :month    this-month}
     :report    {:from          {:year  last-year
                                 :month last-month}
                 :to            {:year  this-year
                                 :month this-month}
                 :show-invoices false}
     :charges   {:agent-opening-balance {:id       :agent-opening-balance
                                         :name     "Opening balance"
                                         :reserved true}}}))
