(ns wkok.buy2let.site.events
  (:require
    [re-frame.core :as rf]))

(rf/reg-event-db
  ::set-active-property
  (fn [db [_ p]]
    (if (not (= "--select--" p))
      (assoc-in db [:site :active-property] (keyword p))
      (assoc-in db [:site :active-property] p))))

(rf/reg-event-db
  :set-active-page
  (fn [db [_ page heading]]
    (-> (assoc-in db [:site :active-page] page)
        (assoc-in [:site :heading] heading))))

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
  ::show-nav-menu
  (fn [db [_ show]]
    (assoc-in db [:site :nav :show-menu] show)))


