(ns wkok.buy2let.crud.impl
  (:require [wkok.buy2let.crud.types :as type]
            [wkok.buy2let.ui.react.view.crud :as crud]
            [wkok.buy2let.site.subs :as subs]
            [re-frame.core :as rf]))


(defn properties []
  (case @(rf/subscribe [::subs/active-panel])
    :properties-edit [crud/edit-panel type/property]
    [crud/list-panel type/property]))

(defn charges []
  (case @(rf/subscribe [::subs/active-panel])
    :charges-edit [crud/edit-panel type/charge]
    [crud/list-panel type/charge]))

(defn invoices []
  (case @(rf/subscribe [::subs/active-panel])
    :invoices-edit [crud/edit-panel type/invoice]
    [crud/list-panel type/invoice]))

(defn delegates []
  (case @(rf/subscribe [::subs/active-panel])
    :delegates-edit [crud/edit-panel type/delegate]
    [crud/list-panel type/delegate]))
