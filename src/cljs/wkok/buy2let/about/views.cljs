(ns wkok.buy2let.about.views
  (:require [re-frame.core :as rf]
            [wkok.buy2let.backend.multimethods :as mm]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]))

;; (defn show-ipsum-dialog [classes text]
;;   #(rf/dispatch [::se/dialog {:panel [text-field {:multiline true
;;                                                   :value text
;;                                                   :disabled true
;;                                                   :rows-max 1000
;;                                                   :style {:width "800px"}
;;                                                   :InputProps {:classes
;;                                                                {:input (:legal classes)}}}]
;;                               :buttons   {:middle {:text     "Close"
;;                                                    :on-click (fn [] (rf/dispatch [::se/dialog]))}}}]))

(defn about 
  [{:keys [classes]}]
  (rf/dispatch [:set-fab-actions nil])
  [paper {:class (:paper classes)}
   [grid {:container true
          :direction :column}
    [grid {:item true}
     [list
      [list-item {:button true}
       [list-item-text {:primary "Version" :secondary "0.0.5"}]]
      [list-item {:button true
                  :component "a"
                  :href (mm/terms-of-service-url)
                  :target "_blank"}
       [list-item-text {:primary "Terms of service"}]]
      [list-item {:button true
                  :component "a"
                  :href (mm/privacy-policy-url)
                  :target "_blank"}
       [list-item-text {:primary "Privacy policy"}]]
      [list-item {:button true
                  :on-click #(js/window.location.assign "#/opensource")}
       [list-item-text {:primary "Open source"}]]]]]])