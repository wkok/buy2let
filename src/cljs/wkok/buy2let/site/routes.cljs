(ns wkok.buy2let.site.routes
  (:require [secretary.core :as sec :refer-macros [defroute]]
            [goog.events :as events]
            [re-frame.core :as rf]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.profile.events :as pe]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.account.events :as ae]
            [wkok.buy2let.reconcile.events :as re]
            [wkok.buy2let.report.events :as repe]
            [wkok.buy2let.crud.types :as type]
            [wkok.buy2let.crud.events :as ce])
  (:import [goog History]
           [goog.history EventType]))


(defn dispatch-role [event role]
  (if (shared/has-role role)
    (rf/dispatch event)
    (rf/dispatch [::se/nothing])))

(defn app-routes []
  (set! (.-hash js/location) "/")
  (sec/set-config! :prefix "#")
  (defroute "/" [] (dispatch-role [:set-active-page :dashboard "Dashboard"] :viewer))
  (defroute "/reconcile/:property-id/:month/:year" [property-id month year]
    (dispatch-role [::re/view-reconcile (re/calc-options {:property-id property-id :year year :month month})] :viewer))
  (defroute "/reconcile/:property-id/:month/:year/edit" [property-id month year]
    (dispatch-role [::re/edit-reconcile (re/calc-options {:property-id property-id :year year :month month})] :editor))
  (defroute "/report/:property-id/:from-month/:from-year/:to-month/:to-year" [property-id
                                                                              from-month from-year
                                                                              to-month to-year]
    (dispatch-role [::repe/view-report (repe/calc-options {:property-id property-id
                                                           :from-year from-year :from-month from-month
                                                           :to-year to-year :to-month to-month})] :viewer))
  (defroute "/properties" [] (dispatch-role [::ce/list-crud type/property] :viewer))
  (defroute "/properties/add" [] (dispatch-role [::ce/add-crud type/property] :editor))
  (defroute "/properties/edit/:id" [id] (dispatch-role [::ce/edit-crud (keyword id) type/property] :editor))
  (defroute "/charges" [] (dispatch-role [::ce/list-crud type/charge] :viewer))
  (defroute "/charges/add" [] (dispatch-role [::ce/add-crud type/charge] :editor))
  (defroute "/charges/edit/:id" [id] (dispatch-role [::ce/edit-crud (keyword id) type/charge] :editor))
  (defroute "/delegates" [] (dispatch-role [::ce/list-crud type/delegate] :owner))
  (defroute "/delegates/add" [] (dispatch-role [::ce/add-crud type/delegate] :owner))
  (defroute "/delegates/edit/:id" [id] (dispatch-role [::ce/edit-crud (keyword id) type/delegate] :owner))
  (defroute "/settings" [] (dispatch-role [:set-active-page :settings "Settings"] :editor))
  (defroute "/profile" [] (dispatch-role [::pe/view-profile] :viewer))
  (defroute "/profile/edit" [] (dispatch-role [::pe/edit-profile] :viewer))
  (defroute "/account" [] (dispatch-role [::ae/view-account] :viewer))
  (defroute "/account/edit" [] (dispatch-role [::ae/edit-account] :owner)))


(doto (History.)
  (events/listen EventType.NAVIGATE #(sec/dispatch! (.-token ^js/goog.history.Event %)))
  (.setEnabled true))