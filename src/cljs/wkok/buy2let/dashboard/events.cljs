(ns wkok.buy2let.dashboard.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::incl-this-month
 (fn [db [_ incl]]
   (assoc-in db [:dashboard :incl-this-month] incl)))
