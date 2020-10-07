(ns wkok.buy2let.site.dialog
  (:require [re-frame.core :as rf]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.site.subs :as subs]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.dialog :refer [dialog]]
            [reagent-material-ui.core.dialog-title :refer [dialog-title]]
            [reagent-material-ui.core.dialog-content :refer [dialog-content]]
            [reagent-material-ui.core.dialog-content-text :refer [dialog-content-text]]
            [reagent-material-ui.core.dialog-actions :refer [dialog-actions]]
            ))

(defn make-button [btn]
  [button {:color (or (:color btn) :primary)
           :on-click #(do (when-let [f (:on-click btn)] (f))
                          (rf/dispatch [::se/dialog]))} (:text btn)])

(defn create-dialog []
  (let [dlg @(rf/subscribe [::subs/dialog])]
    [dialog {:open (not (nil? dlg))
             :disable-backdrop-click (not (:closeable dlg true))
             :disable-escape-key-down (not (:closeable dlg true))
             :on-close #(rf/dispatch [::se/dialog])}
     (when-let [heading (:heading dlg)]
       [dialog-title heading])
     (when-let [message (:message dlg)]
       [dialog-content [dialog-content-text message]])
     (when-let [panel (:panel dlg)]
       [dialog-content panel])
     (when-let [buttons (:buttons dlg)]
       [dialog-actions
        (when-let [btn (:left buttons)]
          [make-button btn])
        (when-let [btn (:middle buttons)]
          [make-button btn])
        (when-let [btn (:right buttons)]
          [make-button btn])])]))

