(ns wkok.buy2let.settings.views
  (:require [re-frame.core :as rf]
            [wkok.buy2let.backend.events :as be]
            [wkok.buy2let.backend.subs :as bs]
            [wkok.buy2let.site.events :as se]
            [clojure.walk :as w]
            [wkok.buy2let.shared :as shared]))


(defn settings []
  (rf/dispatch [:set-fab-actions nil])
  [:div
   [:br]
   [:div [:a {:href "#/delegates"} "Invite users"]]
   (let [auth (-> @(rf/subscribe [::bs/user]) :provider-data js->clj w/keywordize-keys)]
     [:div
      [:hr]
      [:div (if (some #(= (:providerId %) "google.com") auth)
              (shared/anchor #(rf/dispatch [::be/unlink :google]) "Unlink Google")
              (shared/anchor #(rf/dispatch [::be/link :google]) "Link Google"))]
      [:div (if (some #(= (:providerId %) "facebook.com") auth)
              (shared/anchor #(rf/dispatch [::be/unlink :facebook]) "Unlink Facebook")
              (shared/anchor #(rf/dispatch [::be/link :facebook]) "Link Facebook"))]
      [:hr]])
   [:div
    [:div [:a {:href "#" :on-click #(rf/dispatch [:sign-out])} "Sign out"]]
    [:hr]
    [:br]
    [:div 
     (shared/anchor #(rf/dispatch [::se/dialog {:heading "Are you sure?"
                                                :message "This will delete all data associated with this account, for all users you've given access to, and is not recoverable!"
                                                :buttons {:left  {:text     "DELETE"
                                                                  :on-click (fn [] (rf/dispatch [::be/delete-account]))
                                                                  :class    "dialog-btn-danger"}
                                                          :right {:text "Cancel"}}}]) 
                    "Delete account"
                    "settings-link-danger")]]])
