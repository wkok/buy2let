(ns wkok.buy2let.ui.react.component.invoice
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.shared :as shared]
            [reagent-mui.material.tooltip :refer [tooltip]]
            [reagent-mui.material.list-item-secondary-action :refer [list-item-secondary-action]]
            [reagent-mui.material.icon-button :refer [icon-button]]
            [reagent-mui.icons.visibility-outlined :refer [visibility-outlined]]
            [reagent-mui.icons.attach-file :refer [attach-file]]))

(defn invoices-button
  [charge {:keys [property year month]} size color]
  [tooltip {:title "Invoices"}
   [icon-button {:color color
                 :size size
                 :on-click #(js/window.location.assign (str "#/reconcile/" (-> property :id name)
                                                            "/" (-> month name)
                                                            "/" (-> year name)
                                                            "/" (-> charge :id name)
                                                            "/invoices"))}
    [attach-file {:font-size size}]]])

(defn attachment-button
  [invoice]
  (when (:attached invoice)
    (ra/as-element
     [list-item-secondary-action
      [tooltip {:title "View"}
       [icon-button {:edge :end
                     :color :primary
                     :on-click #(rf/dispatch [::shared/view-attachment :invoices (:id invoice)])}
        [visibility-outlined]]]])))
