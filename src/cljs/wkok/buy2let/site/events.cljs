(ns wkok.buy2let.site.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]))

(rf/reg-event-db
  ::set-active-property
  (fn [db [_ p]]
    (assoc-in db [:site :active-property] (keyword p))))

(rf/reg-event-db
  :set-active-page
  (fn [db [_ page heading]]
    (-> (assoc-in db [:site :active-page] page)
        (assoc-in [:site :heading] heading)
        (update-in [:site] dissoc :active-panel))))

(rf/reg-event-db
  :set-fab-actions
  (fn [db [_ actions]]
    (assoc-in db [:site :fab-actions] actions)))

(rf/reg-event-db
  ::show-progress
  (fn [db [_ show]]
    (assoc-in db [:site :show-progress] show)))

(rf/reg-event-db
  ::dialog
  (fn [db [_ dialog]]
    (assoc-in db [:site :dialog] dialog)))

(rf/reg-event-db
  ::splash
  (fn [db [_ show]]
    (assoc-in db [:site :splash] show)))

(rf/reg-event-db
  ::show-nav-menu
  (fn [db [_ show]]
    (assoc-in db [:site :nav :show-menu] show)))

(rf/reg-event-db
  ::toggle-profile-menu
  (fn [db [_ target]]
    (assoc-in db [:site :profile :show-menu] target)))

(rf/reg-event-db
 ::set-snack-error
 (fn [db [_ error]]
   (assoc-in db [:site :snack :error] error)))

(rf/reg-event-fx
 ::nothing
 (fn [_ _]
   {}))

(rf/reg-event-db
 ::load-location
 (fn [db [_ location]]
   (-> (assoc-in db [:site :location] location)
       (assoc-in [:site :show-progress] false))))

(rf/reg-event-db
 ::location-failure
 (fn [db [_ failure]]
   (-> (assoc-in db [:site :location-failure] failure)
       (assoc-in [:site :location :currency] "USD")
       (assoc-in [:site :show-progress] false))))

(rf/reg-event-fx
 ::detect-location
 (fn [{:keys [db]} _]
   {:db   (assoc-in db [:site :show-progress] true)
    :http-xhrio {:method          :get
                 :uri             "https://ipapi.co/json/"
                 :timeout         5000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::load-location]
                 :on-failure      [::location-failure]}}))

