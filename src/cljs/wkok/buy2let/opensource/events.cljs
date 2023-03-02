(ns wkok.buy2let.opensource.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.security :as sec]))

(rf/reg-event-db
 ::opensource
 (fn [db [_ role]]
   (sec/with-authorisation role db
     #(-> (assoc-in db [:site :active-page] :opensource)
          (update-in [:site] dissoc :active-panel)
          (assoc-in [:site :heading] "Open source")))))
