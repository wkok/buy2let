(ns wkok.buy2let.account.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::remember-account
 (fn [db _]
   (get-in db [:site :account-selector :remember] false)))

(rf/reg-sub
 ::selected-account-id
 (fn [db _]
   (get-in db [:site :account-selector :account-id])))

(rf/reg-sub
 ::account
 (fn [db _]
   (get-in db [:security :account])))

(rf/reg-sub
 ::accounts
 (fn [db _]
   (get-in db [:security :accounts])))
