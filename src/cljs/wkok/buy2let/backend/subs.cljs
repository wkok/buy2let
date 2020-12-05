(ns wkok.buy2let.backend.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::user
  (fn [db _]
    (get-in db [:security :auth])))

(rf/reg-sub
  ::local-user
  (fn [db _]
    (get-in db [:security :user])))

(rf/reg-sub
 ::claims
 (fn [db _]
   (get-in db [:security :claims])))

(rf/reg-sub
 ::error
 (fn [db _]
   (get-in db [:backend :error])))

