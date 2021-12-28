(ns wkok.buy2let.settings.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.backend.events :as be]
            [wkok.buy2let.backend.subs :as bs]
            [wkok.buy2let.site.events :as se]
            [clojure.walk :as w]
            [reagent-mui.material.list :refer [list]]
            [reagent-mui.material.list-item :refer [list-item]]
            [reagent-mui.material.list-item-text :refer [list-item-text]]
            [reagent-mui.material.list-subheader :refer [list-subheader]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.paper :refer [paper]]))


(defn settings []
  (rf/dispatch [:set-fab-actions nil])
  (let [auth (-> @(rf/subscribe [::bs/user]) :provider-data js->clj w/keywordize-keys)]
    [grid {:container true
           :direction :column
           :spacing 2}
     [grid {:item true}
      [paper {:class (:paper classes)}
       [list {:subheader (ra/as-element [list-subheader "Profile settings"])}
        (if (some #(= (:providerId %) "google.com") auth)
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/unlink :google])}
           [list-item-text {:primary "Unlink Google"}]]
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/link :google])}
           [list-item-text {:primary "Link Google"}]])
        (if (some #(= (:providerId %) "facebook.com") auth)
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/unlink :facebook])}
           [list-item-text {:primary "Unlink Facebook"}]]
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/link :facebook])}
           [list-item-text {:primary "Link Facebook"}]])
        [list-item {:button true
                    :on-click #(rf/dispatch [:sign-out])}
         [list-item-text {:primary "Sign out"}]]]]]
     [grid {:item true}
      [paper {:class (:paper classes)}
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
                          :primary-typography-props {:color :error}}]]]]]]))
