(ns wkok.buy2let.site.subs
  (:require
   [re-frame.core :as rf]))


(rf/reg-sub
  ::active-property
  (fn [db _]
    (get-in db [:site :active-property])))

(rf/reg-sub
  ::active-panel
  (fn [db _]
    (get-in db [:site :active-panel])))

(rf/reg-sub
  ::active-page
  (fn [db _]
    (get-in db [:site :active-page])))

(rf/reg-sub
  ::show-progress
  (fn [db _]
    (get-in db [:site :show-progress] false)))

(rf/reg-sub
  ::dialog
  (fn [db _]
    (get-in db [:site :dialog])))

(rf/reg-sub
  ::splash
  (fn [db _]
    (get-in db [:site :splash])))

(rf/reg-sub
  ::heading
  (fn [db _]
    (get-in db [:site :heading])))

(rf/reg-sub
  ::fab-actions
  (fn [db _]
    (get-in db [:site :fab-actions])))

(rf/reg-sub
  :form-old
  (fn [db _]
    (get-in db [:form :old])))

(rf/reg-sub
  ::nav-menu-show
  (fn [db _]
    (get-in db [:site :nav :show-menu])))

(rf/reg-sub
  ::profile-menu-show
  (fn [db _]
    (get-in db [:site :profile :show-menu])))

(rf/reg-sub
  ::security
  (fn [db _]
    (get db :security)))






