(ns wkok.buy2let.about.views
  (:require [re-frame.core :as rf]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]))

(defn about 
  [{:keys [classes]}]
  (rf/dispatch [:set-fab-actions nil])
  [paper {:class (:paper classes)}
   [grid {:container true
          :direction :column}
    [grid {:item true}
     [list
      [list-item {:button true}
       [list-item-text {:primary "Version" :secondary "0.0.1"}]]
      [list-item {:button true}
       [list-item-text {:primary "Terms of service"}]]
      [list-item {:button true}
       [list-item-text {:primary "Privacy policy"}]]
      [list-item {:button true
                  :on-click #(js/window.location.assign "#/opensource")}
       [list-item-text {:primary "Open source"}]]]]]])