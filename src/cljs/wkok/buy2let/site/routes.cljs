(ns wkok.buy2let.site.routes
  (:require [secretary.core :as sec :refer-macros [defroute]]
            [re-frame.core :as rf]
            [wkok.buy2let.profile.events :as pe]
            [wkok.buy2let.about.events :as abe]
            [wkok.buy2let.opensource.events :as ose]
            [wkok.buy2let.account.events :as ae]
            [wkok.buy2let.subscription.events :as sbse]
            [wkok.buy2let.reconcile.events :as re]
            [wkok.buy2let.report.events :as repe]
            [wkok.buy2let.crud.types :as type]
            [wkok.buy2let.crud.events :as ce]))

(defn app-routes []
  (set! (.-hash js/location) "/")
  (sec/set-config! :prefix "#")
  (defroute "/" [] (rf/dispatch [:set-active-page :viewer :dashboard "Dashboard"]))
  (defroute "/reconcile" []
    (rf/dispatch [::re/view-reconcile :viewer {}]))
  (defroute "/reconcile/:property-id/:month/:year" [property-id month year]
    (rf/dispatch [::re/view-reconcile :viewer {:property-id property-id :year year :month month}]))
  (defroute "/reconcile/:property-id/:month/:year/edit" [property-id month year]
    (rf/dispatch [::re/edit-reconcile :editor {:property-id property-id :year year :month month}]))
  (defroute "/reconcile/:property-id/:month/:year/:charge-id/invoices" [property-id month year charge-id]
    (rf/dispatch [::ce/list-crud :editor type/invoice {:property-id property-id :year year :month month :charge-id charge-id}]))
  (defroute "/reconcile/:property-id/:month/:year/:charge-id/invoices/add" [property-id month year charge-id]
    (rf/dispatch [::ce/add-crud :editor type/invoice {:property-id property-id :year year :month month :charge-id charge-id}]))
  (defroute "/reconcile/:property-id/:month/:year/:charge-id/invoices/edit/:id" [property-id month year charge-id id]
    (rf/dispatch [::ce/edit-crud :editor (keyword id) type/invoice {:property-id property-id :year year :month month :charge-id charge-id :id id}]))
  (defroute "/report" []
    (rf/dispatch [::repe/view-report :viewer {}]))
  (defroute "/report/:property-id/:from-month/:from-year/:to-month/:to-year" [property-id
                                                                              from-month from-year
                                                                              to-month to-year]
    (rf/dispatch [::repe/view-report :viewer {:property-id property-id
                                        :from-year from-year :from-month from-month
                                      :to-year to-year :to-month to-month}]))
  (defroute "/properties" [] (rf/dispatch [::ce/list-crud :viewer type/property]))
  (defroute "/properties/add" [] (rf/dispatch [::ce/add-crud :editor type/property]))
  (defroute "/properties/edit/:id" [id] (rf/dispatch [::ce/edit-crud :editor (keyword id) type/property]))
  (defroute "/charges" [] (rf/dispatch [::ce/list-crud :viewer type/charge]))
  (defroute "/charges/add" [] (rf/dispatch [::ce/add-crud :editor type/charge]))
  (defroute "/charges/edit/:id" [id] (rf/dispatch [::ce/edit-crud :editor (keyword id) type/charge]))
  (defroute "/delegates" [] (rf/dispatch [::ce/list-crud :owner type/delegate]))
  (defroute "/delegates/add" [] (rf/dispatch [::ce/add-crud :owner type/delegate]))
  (defroute "/delegates/edit/:id" [id] (rf/dispatch [::ce/edit-crud :owner (keyword id) type/delegate]))
  (defroute "/settings" [] (rf/dispatch [:set-active-page :editor :settings "Settings"]))
  (defroute "/profile" [] (rf/dispatch [::pe/view-profile :viewer]))
  (defroute "/profile/edit" [] (rf/dispatch [::pe/edit-profile :viewer]))
  (defroute "/subscription" [] (rf/dispatch [::sbse/view-subscription :owner]))
  (defroute "/account" [] (rf/dispatch [::ae/view-account :viewer]))
  (defroute "/account/edit" [] (rf/dispatch [::ae/edit-account :owner]))
  (defroute "/about" [] (rf/dispatch [::abe/about :viewer]))
  (defroute "/opensource" [] (rf/dispatch [::ose/opensource :viewer])))
