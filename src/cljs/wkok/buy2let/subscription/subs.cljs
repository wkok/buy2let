(ns wkok.buy2let.subscription.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::subscription-properties
 (fn [db _]
   (get-in db [:site :subscription :properties])))

(rf/reg-sub
 ::subscription-active-properties
 (fn [db _]
   (get-in db [:site :subscription :active-properties])))

(rf/reg-sub
 ::inactive-delete-ack
 (fn [db _]
   (get-in db [:site :subscription :inactive-delete-ack] false)))

(rf/reg-sub
 ::show-active-properties-dialog
 (fn [db _]
   (get-in db [:site :subscription :show-active-properties-dialog] false)))


