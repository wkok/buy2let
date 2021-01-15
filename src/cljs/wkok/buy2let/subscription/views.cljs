(ns wkok.buy2let.subscription.views
  (:require [re-frame.core :as rf]
            [cemerick.url :as url]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.account.subs :as as]
            [wkok.buy2let.subscription.events :as subse]
            [wkok.buy2let.account.events :as ae]
            [wkok.buy2let.subscription.subs :as subss]
            [wkok.buy2let.backend.multimethods :as mm]
            [reagent-material-ui.icons.thumb-up :refer [thumb-up]]
            [reagent-material-ui.icons.card-membership :refer [card-membership]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.card-actions :refer [card-actions]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.typography :refer [typography]]))


(defn edit-subscription []
  [:div])

(defn subscription-single-card [account]
  (let [pos-fn #(if (< % 2) 2 %)]
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
              :spacing 2}
        (when (= "cancelled" (get-in account [:subscription :status]))
          [grid {:item true}
           [typography
            "Your subscription to the Multi Property License has been cancelled."]])
        [grid {:item true}
         [typography
          "You are currently subscribed to the free Single Property License."]]
        [grid {:item true}
         [text-field {:type        :number
                      :label       "Increase properties to"
                      :value       @(rf/subscribe [::subss/subscription-properties])
                      :on-change   #(rf/dispatch [::subse/set-subscription-properties (-> % .-target .-value js/parseInt Math/abs pos-fn)])
                      :step 1}]]]]]
     [card-actions
      [button {:color :secondary
               :disabled @(rf/subscribe [::ss/show-progress])
               :on-click #(rf/dispatch [::subse/upgrade-subscription])} "Upgrade"]
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
             :justify :center}
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
             :justify :center}
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

(defn subscription-cancelled-card []
  [card
   [card-content
    [grid {:container true
           :direction :row
           :spacing 2
           :justify :center}
     [grid {:item true}
      [thumb-up {:font-size :large}]]
     [grid {:item true
            :container true
            :direction :column
            :spacing 2}
      [grid {:item true}
       [typography
        "Your Multi Property License subscription has been successfully cancelled."]]
      [grid {:item true}
       [typography
        "You are now on the Single Property License which includes one free license"]]]]]
   [card-actions
    [button {:color :primary
             :disabled @(rf/subscribe [::ss/show-progress])
             :on-click #(rf/dispatch [::subse/manage-subscription])} "Resubscribe"]]])

(defn subscription-actioned? []
  (case (-> js/window .-location .-href
            url/url
            :anchor)
    "/subscription?action=activated" :activated
    "/subscription?action=cancelled" :cancelled
    :unchanged))

(defn save-subscription-status [account status]
  (rf/dispatch [::ae/save-account
                {:account (assoc-in account [:subscription :status] status)
                 :back false}]))

(defn view-subscription []
  (let [account-id @(rf/subscribe [::as/account])
        accounts @(rf/subscribe [::as/accounts])
        account (when account-id (account-id accounts))]
    [grid {:container true
           :direction :row
           :spacing 2}
     [grid {:item true
            :xs 12 :md 6}
      (case (subscription-actioned?)
        :activated (do (save-subscription-status account "active")
                       [subscription-activated-card account])
        ; :cancelled (do (save-subscription-status account "cancelled")
                      ;  [subscription-cancelled-card])
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
