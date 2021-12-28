(ns wkok.buy2let.site.dialog
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [clojure.string :as str]
            [clojure.set :as set]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.site.subs :as subs]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.subscription.events :as subse]
            [wkok.buy2let.subscription.subs :as subss]
            [wkok.buy2let.account.subs :as as]
            [reagent-mui.material.button :refer [button]]
            [reagent-mui.material.dialog :refer [dialog]]
            [reagent-mui.material.dialog-title :refer [dialog-title]]
            [reagent-mui.material.dialog-content :refer [dialog-content]]
            [reagent-mui.material.dialog-content-text :refer [dialog-content-text]]
            [reagent-mui.material.dialog-actions :refer [dialog-actions]]
            [reagent-mui.material.form-control :refer [form-control]]
            [reagent-mui.material.input-label :refer [input-label]]
            [reagent-mui.material.select :refer [select]]
            [reagent-mui.material.checkbox :refer [checkbox]]
            [reagent-mui.material.box :refer [box]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.menu-item :refer [menu-item]]
            [reagent-mui.material.list-item-text :refer [list-item-text]]
            [reagent-mui.material.typography :refer [typography]]
            [reagent-mui.material.form-control-label :refer [form-control-label]]))

(defn make-button [btn]
  [button {:color (or (:color btn) :primary)
           :on-click #(do (when-let [f (:on-click btn)] (f))
                          (rf/dispatch [::se/dialog]))} (:text btn)])

(defn create-dialog []
  (let [dlg @(rf/subscribe [::subs/dialog])]
    [dialog {:open (not (nil? dlg))
             :on-close (fn [_event reason]
                         (if (#{"backdropClick" "escapeKeyDown"} reason)
                           (when (:closeable dlg true)
                             (rf/dispatch [::se/dialog]))
                           (rf/dispatch [::se/dialog])))}
     (when-let [heading (:heading dlg)]
       [dialog-title heading])
     (when-let [message (:message dlg)]
       [dialog-content [dialog-content-text message]])
     (when-let [panel (:panel dlg)]
       [dialog-content panel])
     (when-let [buttons (:buttons dlg)]
       [dialog-actions
        (when-let [btn (:left buttons)]
          [make-button btn])
        (when-let [btn (:middle buttons)]
          [make-button btn])
        (when-let [btn (:right buttons)]
          [make-button btn])])]))

(defn active-properties-dialog []
  (let [properties @(rf/subscribe [::cs/all-properties])
        active-properties (or @(rf/subscribe [::subss/subscription-active-properties])
                              (map #(-> % :id name) properties))
        acknowledged @(rf/subscribe [::subss/inactive-delete-ack])
        account-id @(rf/subscribe [::as/account])
        accounts @(rf/subscribe [::as/accounts])
        account (when account-id (account-id accounts))
        subscribed-properties (get-in account [:subscription :properties])
        a (map (fn [p] (-> p :id name)) properties)]
    [dialog {:open @(rf/subscribe [::subss/show-active-properties-dialog])
             :on-close (fn [_event reason]
                         (when (#{"backdropClick" "escapeKeyDown"} reason)
                           "do nothing?"
                           nil))}
     [dialog-title "Subscription changed"]
     [dialog-content
      [grid {:container true
             :direction :column
             :spacing 1}
       [grid {:item true}
        [typography
         "Please select the properties that should remain active on your account."]]
       [grid {:item true}
        [typography {:variant :body2}
          "IMPORTANT - Any unselected properties will be deleted, including all data associated with them, and is not
          recoverable. Review your subscription settings if you're unsure."]]
       [grid {:item true}
        [box {:border 1
              :border-radius 16
              :border-color "primary.main"
              :padding 2}
         [grid {:container true
                :direction :column
                :spacing 1}
          [grid {:item true}
           [typography {:variant :body2} "Subscribed: " subscribed-properties]]
          [grid {:item true}
           [typography {:variant :body2
                        :color (if (> (count active-properties) subscribed-properties)
                                 :secondary :primary)} "Selected: " (count active-properties)]]]]]
       [grid {:item true}
        [form-control {:margin :normal :full-width true}
         [input-label "Active properties"]
         [select
          {:multiple true
           :value active-properties
           :render-value #(str (count %) " selected")
          ;;  :render-value #(->> (map (fn [s] (-> (shared/by-id (keyword s) properties) :name)) %)
          ;;                      (str/join ", "))
           :on-change #(rf/dispatch [::subse/set-active-subscription-properties (-> % .-target .-value js->clj)])}
          (for [property properties]
            ^{:key (:id property)}
            [menu-item {:value (:id property)}
             [checkbox {:color :primary
                        :checked (not (nil? (some #{(-> property :id name)} active-properties)))}]
             [list-item-text {:primary (:name property)}]])]]]
       [grid {:item true}
        [form-control-label
         {:control (ra/as-element
                    [checkbox {:checked acknowledged
                               :color :secondary
                               :on-change #(rf/dispatch [::subse/ack-inactive-delete (-> % .-target .-checked)])}])
          :label (ra/as-element
                  [typography {:variant :caption}
                   "I understand that all unselected properties & their data will be deleted."])}]]]]
     [dialog-actions
      [button {:color :secondary
               :disabled (or (zero? (count active-properties))
                             (> (count active-properties) subscribed-properties)
                             (not acknowledged))
               :on-click #(rf/dispatch [::subse/downgrade-subscription
                                        (set/difference (-> (map (fn [p] (:id p)) properties) set)
                                                        (-> (map (fn [p] (keyword p)) active-properties) set))])} "Apply"]
      [button {:color :primary
               :disabled @(rf/subscribe [::subs/show-progress])
               :on-click #(rf/dispatch [::subse/manage-subscription])} "Manage Subscription"]]]))
