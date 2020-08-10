(ns wkok.buy2let.site.routes
  (:require [secretary.core :as sec :refer-macros [defroute]]
            [goog.events :as events]
            [re-frame.core :as rf]
            [wkok.buy2let.reconcile.events :as re]
            [wkok.buy2let.crud.types :as type]
            [wkok.buy2let.crud.events :as ce])
  (:import [goog History]
           [goog.history EventType]))


(defn app-routes []
  (set! (.-hash js/location) "/")
  (sec/set-config! :prefix "#")
  (defroute "/" [] (rf/dispatch [:set-active-page :dashboard "Dashboard"]))
  (defroute "/reconcile" [] (rf/dispatch [::re/view-reconcile]))
  (defroute "/reconcile/edit" [] (rf/dispatch [::re/edit-reconcile]))
  (defroute "/report" [] (rf/dispatch [:set-active-page :report "Report"]))
  (defroute "/properties" [] (rf/dispatch [::ce/list-crud type/property]))
  (defroute "/properties/add" [] (rf/dispatch [::ce/add-crud type/property]))
  (defroute "/properties/edit/:id" [id] (rf/dispatch [::ce/edit-crud (keyword id) type/property]))
  (defroute "/charges" [] (rf/dispatch [::ce/list-crud type/charge]))
  (defroute "/charges/add" [] (rf/dispatch [::ce/add-crud type/charge]))
  (defroute "/charges/edit/:id" [id] (rf/dispatch [::ce/edit-crud (keyword id) type/charge]))
  (defroute "/delegates" [] (rf/dispatch [::ce/list-crud type/delegate]))
  (defroute "/delegates/add" [] (rf/dispatch [::ce/add-crud type/delegate]))
  (defroute "/delegates/edit/:id" [id] (rf/dispatch [::ce/edit-crud (keyword id) type/delegate]))
  (defroute "/settings" [] (rf/dispatch [:set-active-page :settings "Settings"])))


(doto (History.)
  (events/listen EventType.NAVIGATE #(sec/dispatch! (.-token ^js/goog.history.Event %)))
  (.setEnabled true))