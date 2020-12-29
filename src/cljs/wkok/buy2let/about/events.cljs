(ns wkok.buy2let.about.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::about
 (fn [db _]
   (-> (assoc-in db [:site :active-page] :about)
       (update-in [:site] dissoc :active-panel)
       (assoc-in [:site :heading] "About"))))
