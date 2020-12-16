(ns wkok.buy2let.wizard.views
  (:require 
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [reagent-material-ui.core.paper :refer [paper]]
   [reagent-material-ui.core.stepper :refer [stepper]]
   [reagent-material-ui.core.step :refer [step]]
   [reagent-material-ui.core.button :refer [button]]
   [reagent-material-ui.core.step-label :refer [step-label]]
   [reagent-material-ui.core.step-content :refer [step-content]]
   [reagent-material-ui.core.grid :refer [grid]]
   [reagent-material-ui.core.typography :refer [typography]]
   [reagent-material-ui.core.text-field :refer [text-field]]
   [reagent-material-ui.core.radio :refer [radio]]
   [reagent-material-ui.core.form-control :refer [form-control]]
   [reagent-material-ui.core.form-control-label :refer [form-control-label]]
   [reagent-material-ui.core.radio-group :refer [radio-group]]
   [wkok.buy2let.wizard.subs :as ws]
   [wkok.buy2let.wizard.events :as we]))

(defn yes-no [event answer]
  [form-control
   [radio-group
    [form-control-label {:value :yes? :label "Yes" :control (ra/as-element [radio {:color :primary}])
                         :on-change #(rf/dispatch [event :yes? (-> % .-target .-checked)])
                         :checked (if (nil? answer)
                                    false
                                    answer)}]
    [form-control-label {:value :no? :label "No" :control (ra/as-element [radio {:color :primary}])
                         :on-change #(rf/dispatch [event :no? (-> % .-target .-checked)])
                         :checked (if (nil? answer)
                                    false
                                    (not answer))}]]])

(defn step-property-name [property-name classes]
  [step
   [step-label "Property name"]
   [step-content
    [grid {:container true
           :direction :column}
     [grid {:item true}
      [typography "Enter a short name for the property, for example, the first line of its address"]]
     [grid {:item true}
      [text-field {:label "Name"
                   :margin :normal
                   :auto-focus true
                   :helper-text "For example: 123 Hill Street"
                   :on-change #(rf/dispatch [::we/set-property-name (-> % .-target .-value)])
                   :value property-name}]]]
    [grid {:container true
           :direction :row
           :spacing 2
           :class (:wizard-actions classes)}
     [grid {:item true}
      [button {:disabled true
               :on-click #(rf/dispatch [::we/navigate :back])} "Back"]]
     [grid {:item true}
      [button {:variant :contained
               :color :primary
               :disabled (str/blank? property-name)
               :on-click #(rf/dispatch [::we/navigate :next])}
       "Next"]]]]])

(defn step-mortgage-payment [mortgage-payment? classes]
  [step
   [step-label "Mortgage payment"]
   [step-content
    [grid {:container true
           :direction :column}
     [grid {:item true}
      [typography "Do you have a monthly mortgage payment on this property?"]]
     [grid {:item true}
      [yes-no ::we/set-mortgage-payment mortgage-payment?]]]
    [grid {:container true
           :direction :row
           :spacing 2
           :class (:wizard-actions classes)}
     [grid {:item true}
      [button {:on-click #(rf/dispatch [::we/navigate :back])} "Back"]]
     [grid {:item true}
      [button {:variant :contained
               :color :primary
               :disabled (nil? mortgage-payment?)
               :on-click #(rf/dispatch [::we/navigate :next])}
       "Next"]]]]])

(defn step-rental-agent [rental-agent? classes]
  [step
   [step-label "Rental agent"]
   [step-content
    [grid {:container true
           :direction :column}
     [grid {:item true}
      [typography "Do you employ a rental agent to manage this property?"]]
     [grid {:item true}
      [yes-no ::we/set-rental-agent rental-agent?]]]
    [grid {:container true
           :direction :row
           :spacing 2
           :class (:wizard-actions classes)}
     [grid {:item true}
      [button {:on-click #(rf/dispatch [::we/navigate :back])} "Back"]]
     [grid {:item true}
      [button {:variant :contained
               :color :primary
               :disabled (nil? rental-agent?)
               :on-click #(rf/dispatch [::we/finish])}
       "Finish"]]]]])

(defn wizard [{:keys [classes]}]
  (rf/dispatch [:set-fab-actions nil])
  (let [active-step @(rf/subscribe [::ws/wizard-active-step])
        property-name @(rf/subscribe [::ws/wizard-property-name])
        rental-agent? @(rf/subscribe [::ws/wizard-rental-agent?])
        mortgage-payment? @(rf/subscribe [::ws/wizard-mortgage-payment?])]
    [paper {:class (:paper classes)}
     [grid {:container true
            :direction :row
            :align-items :center}
      [grid {:item true
             :xs 12}
       [stepper {:orientation :vertical
                 :active-step active-step}
        (step-property-name property-name classes)
        (step-mortgage-payment mortgage-payment? classes)
        (step-rental-agent rental-agent? classes)]]]]))

