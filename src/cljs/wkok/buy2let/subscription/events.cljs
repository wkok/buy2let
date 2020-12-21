(ns wkok.buy2let.subscription.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::view-subscription
 (fn [db _]
   (-> (assoc-in db [:site :active-page] :subscription)
       (assoc-in [:site :active-panel] :subscription-view)
       (assoc-in [:site :heading] "Subscription"))))