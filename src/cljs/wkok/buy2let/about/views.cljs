(ns wkok.buy2let.about.views
  (:require [re-frame.core :as rf]
            [wkok.buy2let.backend.multimethods :as mm]
            [wkok.buy2let.site.styles :refer [classes]]
            [reagent-mui.material.paper :refer [paper]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.list :refer [list]]
            [reagent-mui.material.list-item :refer [list-item]]
            [reagent-mui.material.list-item-text :refer [list-item-text]]))

(defn about
  []
  (rf/dispatch [:set-fab-actions nil])
  [paper {:class (:paper classes)}
   [grid {:container true
          :direction :column}
    [grid {:item true}
     [list
      [list-item {:button true}
       [list-item-text {:primary "Version" :secondary "0.3.0"}]]
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
