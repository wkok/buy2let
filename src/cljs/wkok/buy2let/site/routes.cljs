(ns wkok.buy2let.site.routes
  (:require [secretary.core :as sec :refer-macros [defroute]]
            [goog.events :as events]
            [re-frame.core :as rf]
            [wkok.buy2let.profile.events :as pe]
            [wkok.buy2let.reconcile.events :as re]
            [wkok.buy2let.report.events :as repe]
            [wkok.buy2let.crud.types :as type]
            [wkok.buy2let.crud.events :as ce])
  (:import [goog History]
           [goog.history EventType]))


(defn app-routes []
  (set! (.-hash js/location) "/")
  (sec/set-config! :prefix "#")
  (defroute "/" [] (rf/dispatch [:set-active-page :dashboard "Dashboard"]))
  (defroute "/reconcile/:property-id/:month/:year" [property-id month year]
    (rf/dispatch [::re/view-reconcile (re/calc-options {:property-id property-id :year year :month month})]))
  (defroute "/reconcile/:property-id/:month/:year/edit" [property-id month year]
    (rf/dispatch [::re/edit-reconcile (re/calc-options {:property-id property-id :year year :month month})]))
  (defroute "/report/:property-id/:from-month/:from-year/:to-month/:to-year" [property-id
                                                                              from-month from-year
                                                                              to-month to-year]
    (rf/dispatch [::repe/view-report (repe/calc-options {:property-id property-id
                                                         :from-year from-year :from-month from-month
                                                         :to-year to-year :to-month to-month})]))
  (defroute "/properties" [] (rf/dispatch [::ce/list-crud type/property]))
  (defroute "/properties/add" [] (rf/dispatch [::ce/add-crud type/property]))
  (defroute "/properties/edit/:id" [id] (rf/dispatch [::ce/edit-crud (keyword id) type/property]))
  (defroute "/charges" [] (rf/dispatch [::ce/list-crud type/charge]))
  (defroute "/charges/add" [] (rf/dispatch [::ce/add-crud type/charge]))
  (defroute "/charges/edit/:id" [id] (rf/dispatch [::ce/edit-crud (keyword id) type/charge]))
  (defroute "/delegates" [] (rf/dispatch [::ce/list-crud type/delegate]))
  (defroute "/delegates/add" [] (rf/dispatch [::ce/add-crud type/delegate]))
  (defroute "/delegates/edit/:id" [id] (rf/dispatch [::ce/edit-crud (keyword id) type/delegate]))
  (defroute "/settings" [] (rf/dispatch [:set-active-page :settings "Settings"]))
  (defroute "/profile" [] (rf/dispatch [::pe/view-profile]))
  (defroute "/profile/edit" [] (rf/dispatch [::pe/edit-profile]))
  (defroute "/account" [] (rf/dispatch [:set-active-page :account "Account"])))


(doto (History.)
  (events/listen EventType.NAVIGATE #(sec/dispatch! (.-token ^js/goog.history.Event %)))
  (.setEnabled true))