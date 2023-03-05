(ns wkok.buy2let.ui.react.view.subscription
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [wkok.buy2let.backend.subs :as bs]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.account.subs :as as]
            [wkok.buy2let.subscription.events :as subse]
            [wkok.buy2let.account.events :as ae]
            [wkok.buy2let.subscription.subs :as subss]
            [wkok.buy2let.backend.multimethods :as mm]
            [reagent-mui.icons.thumb-up :refer [thumb-up]]
            [reagent-mui.icons.card-membership :refer [card-membership]]
            [reagent-mui.material.text-field :refer [text-field]]
            [reagent-mui.material.card :refer [card]]
            [reagent-mui.material.card-content :refer [card-content]]
            [reagent-mui.material.card-actions :refer [card-actions]]
            [reagent-mui.material.button :refer [button]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.typography :refer [typography]]))


(defn edit-subscription []
  [:div])

(defn subscription-single-card [account]
  (let [min2-fn #(let [n (-> % js/parseInt Math/abs)]
                   (if (< n 2) 2 n))
        pos-fn #(-> % js/parseInt Math/abs)
        change-fn #(if (str/blank? %)
                     % (pos-fn %))
        blur-fn #(if (str/blank? %)
                   2 (min2-fn %))]
    [card
     [card-content
      [grid {:container true
             :direction :row
             :spacing 2
             :justify-content :center}
       [grid {:item true}
        [card-membership {:font-size :large}]]
       [grid {:item true
              :container true
              :direction :column
              :spacing 2}
        (when (= "cancelled" (get-in account [:subscription :status]))
          [grid {:item true}
           [typography
            "Your subscription to the Multi Property License has been cancelled."]])
        [grid {:item true}
         [typography
          "You are currently subscribed to the free Single Property License."]]
        [grid {:item true}
         [text-field {:variant :standard
                      :type        :number
                      :label       "Increase properties to"
                      :value       @(rf/subscribe [::subss/subscription-properties])
                      :on-change   #(rf/dispatch [::subse/set-subscription-properties (-> % .-target .-value change-fn)])
                      :on-blur   #(rf/dispatch [::subse/set-subscription-properties (-> % .-target .-value blur-fn)])
                      :step 1
                      :InputProps {:input-props {:min 2}}}]]]]]
     [card-actions
      [button {:color :secondary
               :disabled @(rf/subscribe [::ss/show-progress])
               :on-click #(rf/dispatch [::subse/upgrade-subscription])}
       "Upgrade"]
      [button {:color :primary
               :disabled @(rf/subscribe [::ss/show-progress])
               :component "a"
               :href (mm/pricing-url)
               :target "_blank"} "PRICING"]]]))

(defn subscription-multi-card [account]
  (let [subscribed-properties (get-in account [:subscription :properties] 1)
        property-s (if (> subscribed-properties 1)
                     " properties" " property")]
    [card
     [card-content
      [grid {:container true
             :direction :row
             :spacing 2
             :justify-content :center}
       [grid {:item true}
        [card-membership {:font-size :large}]]
       [grid {:item true
              :container true
              :direction :column
              :spacing 2}
        [grid {:item true}
         (if (= "cancelling" (get-in account [:subscription :status]))
           [typography
            "Your subscription to the Multi Property License will be cancelled at the end of this billing period,
             after which you will automatically revert to the free Single Property License"]
           [typography
            (str "You are currently subscribed to the Multi Property License for "
                 (get-in account [:subscription :properties])
                 property-s " (includes one free license)")])]]]]
     [card-actions
      [button {:color :primary
               :disabled @(rf/subscribe [::ss/show-progress])
               :on-click #(rf/dispatch [::subse/manage-subscription])} "Manage subscription"]]]))

(defn subscription-activated-card [account]
  (let [subscribed-properties (get-in account [:subscription :properties] 1)
        property-s (if (> subscribed-properties 1)
                     " properties" " property")]
    [card
     [card-content
      [grid {:container true
             :direction :row
             :spacing 2
             :justify-content :center}
       [grid {:item true}
        [thumb-up {:font-size :large}]]
       [grid {:item true
              :container true
              :direction :column
              :spacing 2}
        [grid {:item true}
         [typography
          "Thank you!"]]
        [grid {:item true}
         [typography
          (str "Your subscription has been successfully upgraded to the Multi Property License for "
               (get-in account [:subscription :properties])
               property-s " (includes one free license)")]]]]]
     [card-actions
      [button {:color :primary
               :disabled @(rf/subscribe [::ss/show-progress])
               :on-click #(rf/dispatch [::subse/manage-subscription])} "Manage subscription"]]]))

(defn save-subscription-status [account status]
  (rf/dispatch [::ae/save-account
                {:account (assoc-in account [:subscription :status] status)
                 :back false}]))

(defn view-subscription []
  (let [account-id @(rf/subscribe [::as/account])
        accounts @(rf/subscribe [::as/accounts])
        account (when account-id (account-id accounts))
        subscription-action @(rf/subscribe [::bs/subscription-action])]
    [grid {:container true
           :direction :row
           :spacing 2}
     [grid {:item true
            :xs 12 :md 6}
      (case subscription-action
       :activated (do (save-subscription-status account "active")
                      [subscription-activated-card account])
       (case (get-in account [:subscription :status])
         "active"    [subscription-multi-card account]
         "cancelling" [subscription-multi-card account]
         "cancelled" [subscription-single-card account]
         [subscription-single-card account]))]]))

(defn subscription []
  (rf/dispatch [:set-fab-actions nil])
  (case @(rf/subscribe [::ss/active-panel])
    :subscription-edit [edit-subscription]
    [view-subscription]))
