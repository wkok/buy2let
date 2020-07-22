(ns wkok.buy2let.settings.views
  (:require [re-frame.core :as rf]
            [wkok.buy2let.backend.events :as fe]
            [wkok.buy2let.backend.subs :as fs]
            [wkok.buy2let.site.events :as se]
            [clojure.walk :as w]))


(defn settings []
  (rf/dispatch [:set-fab-actions nil])
  [:div
   [:br]
   [:div [:a {:href "#/users"} "Delegate access"]]
   (let [auth (-> @(rf/subscribe [::fs/user]) :provider-data js->clj w/keywordize-keys)]
     [:div
      [:hr]
      [:div (if (some #(= (:providerId %) "google.com") auth)
              [:a {:href "javascript:void(0);" :on-click #(rf/dispatch [::fe/unlink :google])} "Unlink Google"]
              [:a {:href "javascript:void(0);" :on-click #(rf/dispatch [::fe/link :google])} "Link Google"])]
      [:br]
      [:div (if (some #(= (:providerId %) "facebook.com") auth)
              [:a {:href "javascript:void(0);" :on-click #(rf/dispatch [::fe/unlink :facebook])} "Unlink Facebook"]
              [:a {:href "javascript:void(0);" :on-click #(rf/dispatch [::fe/link :facebook])} "Link Facebook"])]
      [:br]
      [:div (if (some #(= (:providerId %) "twitter.com") auth)
              [:a {:href "javascript:void(0);" :on-click #(rf/dispatch [::fe/unlink :twitter])} "Unlink Twitter"]
              [:a {:href "javascript:void(0);" :on-click #(rf/dispatch [::fe/link :twitter])} "Link Twitter"])]
      [:br]
      [:div (if (some #(= (:providerId %) "github.com") auth)
              [:a {:href "javascript:void(0);" :on-click #(rf/dispatch [::fe/unlink :github])} "Unlink Github"]
              [:a {:href "javascript:void(0);" :on-click #(rf/dispatch [::fe/link :github])} "Link Github"])]
      [:hr]])
   [:div
    [:div [:a {:href "#" :on-click #(rf/dispatch [::fe/sign-out])} "Sign out"]]
    [:hr]
    [:br]
    [:div [:a.settings-link-danger {:href "javascript:void(0);" :on-click #(rf/dispatch [::se/dialog {:heading "Are you sure?"
                                                                                                      :message "This will delete all data associated with this account, for all users you've given access to, and is not recoverable!"
                                                                                                      :buttons {:left  {:text     "DELETE"
                                                                                                                        :on-click (fn [] (rf/dispatch [::fe/delete-account]))
                                                                                                                        :class    "dialog-btn-danger"}
                                                                                                                :right {:text "Cancel"}}}])} "Delete account"]]]])
