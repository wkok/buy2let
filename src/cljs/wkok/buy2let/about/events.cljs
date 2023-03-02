(ns wkok.buy2let.about.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.security :as sec]))

(rf/reg-event-db
 ::about
 (fn [db [_ role]]
   (sec/with-authorisation role db
     #(-> (assoc-in db [:site :active-page] :about)
          (update-in [:site] dissoc :active-panel)
          (assoc-in [:site :heading] "About")))))
