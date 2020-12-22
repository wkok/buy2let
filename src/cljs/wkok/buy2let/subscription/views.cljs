(ns wkok.buy2let.subscription.views
  (:require [re-frame.core :as rf]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.account.subs :as as]
            [wkok.buy2let.account.events :as ae]
            [reagent-material-ui.icons.card-membership :refer [card-membership]]
            [reagent-material-ui.core.box :refer [box]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.card-actions :refer [card-actions]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.typography :refer [typography]]))

(defn edit-subscription []
  [:div])

(defn subscription-card [account]
  [card
   [card-content
    [grid {:container true
           :direction :row
           :spacing 2
           :justify :center}
     [grid {:item true}
      [card-membership {:font-size :large}]]
     [grid {:item true
            :container true
            :direction :column
            :spacing 1}
      [grid {:item true}
       [typography
        "Your current subscription includes 1 property, free forever."]]
      [grid {:item true}
       [typography
        "Depending on user demand, we might offer paid subscriptions in future, allowing you to add more properties."]]
      [grid {:item true}
       (if (:keep-me-informed account)
         [typography {:component :div}
          [box {:font-style :italic}
           "You've already indicated that you're interested. We'll let you know when this feature is available."]]
         [typography
          "Please register your interest by clicking below & we'll let you know!"])]]]]
   [card-actions
    (when (not (:keep-me-informed account))
      [button {:color :secondary
               :on-click #(rf/dispatch [::ae/save-account
                                        (assoc account :keep-me-informed true)])} "Keep me informed"])]])

(defn view-subscription []
  (let [account-id @(rf/subscribe [::as/account])
        accounts @(rf/subscribe [::as/accounts])
        account (when account-id (account-id accounts))]
    [grid {:container true
           :direction :row
           :spacing 2}
     [grid {:item true
            :xs 12 :md 6}
      [subscription-card account]]]))

(defn subscription []
  (rf/dispatch [:set-fab-actions nil])
  (case @(rf/subscribe [::ss/active-panel])
    :subscription-edit [edit-subscription]
    [view-subscription]))
