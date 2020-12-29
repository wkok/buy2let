(ns wkok.buy2let.opensource.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::opensource
 (fn [db _]
   (-> (assoc-in db [:site :active-page] :opensource)
       (update-in [:site] dissoc :active-panel)
       (assoc-in [:site :heading] "Open source"))))
