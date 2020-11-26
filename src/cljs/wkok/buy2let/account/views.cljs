(ns wkok.buy2let.account.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.backend.events :as be]
            [wkok.buy2let.site.events :as se]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]
            [reagent-material-ui.core.list-subheader :refer [list-subheader]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.paper :refer [paper]]))


(defn account [props]
  (rf/dispatch [:set-fab-actions nil])
  [grid {:container true
         :direction :column
         :spacing 2}
   [grid {:item true}
    [paper {:class (get-in props [:classes :paper])}
     [list {:subheader (ra/as-element [list-subheader "Account settings"])}
      [list-item {:button true
                  :on-click #(js/window.location.assign "#/delegates")}
       [list-item-text {:primary "Invite users"}]]
      [list-item {:button true
                  :on-click #(rf/dispatch [::se/dialog {:heading "Delete account?"
                                                        :message "This will delete all data associated with this account, for all users you've given access to, and is not recoverable!"
                                                        :buttons {:left  {:text     "DELETE"
                                                                          :on-click (fn [] (rf/dispatch [::be/delete-account]))
                                                                          :color :secondary}
                                                                  :right {:text "Cancel"}}}])}
       [list-item-text {:primary "Delete account"
                        :primary-typography-props {:color :error}}]]]]]])


