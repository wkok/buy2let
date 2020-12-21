(ns wkok.buy2let.dashboard.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::incl-this-month
 (fn [db _]
   (get-in db [:dashboard :incl-this-month] true)))
