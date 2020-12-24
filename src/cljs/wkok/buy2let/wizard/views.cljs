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
   [reagent-material-ui.core.box :refer [box]]
   [reagent-material-ui.core.typography :refer [typography]]
   [reagent-material-ui.core.text-field :refer [text-field]]
   [reagent-material-ui.core.radio :refer [radio]]
   [reagent-material-ui.core.checkbox :refer [checkbox]]
   [reagent-material-ui.core.form-control :refer [form-control]]
   [reagent-material-ui.core.form-control-label :refer [form-control-label]]
   [reagent-material-ui.core.radio-group :refer [radio-group]]
   [wkok.buy2let.wizard.subs :as ws]
   [wkok.buy2let.crud.subs :as cs]
   [wkok.buy2let.currencies :as currencies]
   [wkok.buy2let.shared :as shared]
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

(defn step-property-name [property-name]
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
                   :on-change #(rf/dispatch-sync [::we/set-property-name (-> % .-target .-value)])
                   :value property-name}]]]
    [grid {:container true
           :direction :row
           :spacing 2}
     [grid {:item true}
      [button {:disabled true
               :variant :outlined
               :on-click #(rf/dispatch [::we/navigate :back])} "Back"]]
     [grid {:item true}
      [button {:variant :contained
               :color :primary
               :disabled (str/blank? property-name)
               :on-click #(rf/dispatch [::we/navigate :next])}
       "Next"]]]]])

(defn step-property-currency [currency]
  [step
   [step-label "Currency"]
   [step-content
    [grid {:container true
           :direction :column
           :spacing 2}
     [grid {:item true}
      [typography "Which currency does this property operate in?"]]
     [grid {:item true}
      [currencies/select-currency {:value currency
                                   :on-change #(rf/dispatch-sync [::we/set-property-currency %])}]]]
    [grid {:container true
           :direction :row
           :spacing 2}
     [grid {:item true}
      [button {:variant :outlined
               :on-click #(rf/dispatch [::we/navigate :back])} "Back"]]
     [grid {:item true}
      [button {:variant :contained
               :color :primary
               :disabled (str/blank? currency)
               :on-click #(rf/dispatch [::we/navigate :next])}
       "Next"]]]]])

(defn step-rent-charged []
  [step
   [step-label "Rent charged"]
   [step-content
    [grid {:container true
           :direction :column
           :spacing 2}
     [grid {:item true}
      [typography "What is the monthly rent amount charged to the tenant?"]]
     [grid {:item true}
      [text-field {:type        :number
                   :label       "Rent charged"
                   :value       @(rf/subscribe [::ws/wizard-rent-charged-amount])
                   :on-change   #(rf/dispatch-sync [::we/set-rent-charged-amount (-> % .-target .-value)])
                   :on-blur     #(rf/dispatch-sync [::we/set-rent-charged-amount (-> % .-target .-value shared/format-money)])
                   :min         0 :step "0.01"
                   :placeholder "0.00"
                   :InputLabelProps {:shrink true}}]]
     [grid {:container true
            :item true
            :direction :row
            :spacing 2}
      [grid {:item true}
       [button {:variant :outlined
                :on-click #(rf/dispatch [::we/navigate :back])} "Back"]]
      [grid {:item true}
       [button {:variant :contained
                :color :primary
                :on-click #(rf/dispatch [::we/navigate :next])}
        "Next"]]]]]])

(defn step-rental-agent [rental-agent?]
  [step
   [step-label "Rental agent"]
   [step-content
    [grid {:container true
           :direction :column}
     [grid {:item true}
      [typography "Do you employ a rental agent to manage this property?"]]
     [grid {:item true}
      [form-control
       [radio-group
        [grid {:container true
               :direction :row
               :spacing 1}
         [grid {:item true}
          [form-control-label {:value :yes? :label "Yes" :control (ra/as-element [radio {:color :primary}])
                               :on-change #(rf/dispatch [::we/set-rental-agent :yes? (-> % .-target .-checked)])
                               :checked (if (nil? rental-agent?)
                                          false
                                          rental-agent?)}]]
         [grid {:container true
                :direction :row
                :item true
                :spacing 2
                :style {:margin-left "2em"}}
          [grid {:item true
                 :style {:display (when (not rental-agent?) :none)}}
           [text-field {:type        :number
                        :label       "Monthly commission"
                        :value       @(rf/subscribe [::ws/wizard-commission-amount])
                        :on-change   #(rf/dispatch-sync [::we/set-commission-amount (-> % .-target .-value)])
                        :on-blur     #(rf/dispatch-sync [::we/set-commission-amount (-> % .-target .-value shared/format-money)])
                        :min         0 :step "0.01"
                        :placeholder "0.00"
                        :InputLabelProps {:shrink true}}]]]]
        [grid {:item true}
         [form-control-label {:value :no? :label "No" :control (ra/as-element [radio {:color :primary}])
                              :on-change #(rf/dispatch [::we/set-rental-agent :no? (-> % .-target .-checked)])
                              :checked (if (nil? rental-agent?)
                                         false
                                         (not rental-agent?))}]]]]]]
    [grid {:container true
           :direction :row
           :spacing 2}
     [grid {:item true}
      [button {:variant :outlined
               :on-click #(rf/dispatch [::we/navigate :back])} "Back"]]
     [grid {:item true}
      [button {:variant :contained
               :color :primary
               :disabled (nil? rental-agent?)
               :on-click #(rf/dispatch [::we/navigate :next])}
       "Next"]]]]])

(defn step-charges []
  (let [selected-charges @(rf/subscribe [::ws/wizard-charges])]
    [step
     [step-label "Charges"]
     [step-content
      [grid {:container true
             :direction :column
             :spacing 1}
       [grid {:item true}
        [:div
         [typography "Do any of these charges apply to the property from time to time?"]
         [typography {:variant :body2} "(Tip: you can add more later under the 'Charges' menu)"]]]
       [grid {:item true
              :container true
              :direction :row}
        (for [charge (filter #(not (or (= :rent-charged-id (:id %))
                                       (= :agent-commission-id (:id %))
                                       (= :mortgage-interest-id (:id %))
                                       (= :mortgage-repayment-id (:id %))
                                       (= :payment-received-id (:id %))
                                       (= :tenant-opening-balance (:id %))
                                       (= :agent-opening-balance (:id %))
                                       (= :bank-charges-id (:id %))))
                             @(rf/subscribe [::cs/charges]))]
          ^{:key (:id charge)}
          [grid {:item true}
           [form-control-label
            {:control (ra/as-element
                       [checkbox {:checked (if ((:id charge) selected-charges) true false)
                                  :color :primary
                                  :on-change #(rf/dispatch [::we/set-charge (-> % .-target .-checked) charge])}])
             :label (:name charge)}]])]]
      [grid {:container true
             :direction :row
             :spacing 2}
       [grid {:item true}
        [button {:variant :outlined
                 :on-click #(rf/dispatch [::we/navigate :back])} "Back"]]
       [grid {:item true}
        [button {:variant :contained
                 :color :primary
                 :on-click #(rf/dispatch [::we/navigate :next])}
         "Next"]]]]]))

(defn step-mortgage-payment [mortgage-payment?]
  [step
   [step-label "Mortgage payment"]
   [step-content
    [grid {:container true
           :direction :column
           :spacing 2}
     [grid {:item true}
      [typography "Do you have a monthly mortgage payment on this property?"]]
     [grid {:item true}
      [form-control
       [radio-group
        [grid {:container true
               :direction :row
               :spacing 1}
         [grid {:item true}
          [form-control-label {:value :yes? :label "Yes" :control (ra/as-element [radio {:color :primary}])
                               :on-change #(rf/dispatch [::we/set-mortgage-payment :yes? (-> % .-target .-checked)])
                               :checked (if (nil? mortgage-payment?)
                                          false
                                          mortgage-payment?)}]]
         [grid {:container true
                :direction :row
                :item true
                :spacing 2
                :style {:margin-left "2em"}}
          [grid {:item true
                 :style {:display (when (not mortgage-payment?) :none)}}
           [text-field {:type        :number
                        :label       "Mortgage repayment"
                        :value       @(rf/subscribe [::ws/wizard-mortgage-repayment-amount])
                        :on-change   #(rf/dispatch-sync [::we/set-mortgage-repayment-amount (-> % .-target .-value)])
                        :on-blur     #(rf/dispatch-sync [::we/set-mortgage-repayment-amount (-> % .-target .-value shared/format-money)])
                        :min         0 :step "0.01"
                        :placeholder "0.00"
                        :InputLabelProps {:shrink true}}]]
          [grid {:item true
                 :style {:display (when (not mortgage-payment?) :none)}}
           [text-field {:type        :number
                        :label       "Mortgage interest"
                        :value       @(rf/subscribe [::ws/wizard-mortgage-interest-amount])
                        :on-change   #(rf/dispatch-sync [::we/set-mortgage-interest-amount (-> % .-target .-value)])
                        :on-blur     #(rf/dispatch-sync [::we/set-mortgage-interest-amount (-> % .-target .-value shared/format-money)])
                        :min         0 :step "0.01"
                        :placeholder "0.00"
                        :InputLabelProps {:shrink true}}]]]]
        [grid {:item true}
         [form-control-label {:value :no? :label "No" :control (ra/as-element [radio {:color :primary}])
                              :on-change #(rf/dispatch [::we/set-mortgage-payment :no? (-> % .-target .-checked)])
                              :checked (if (nil? mortgage-payment?)
                                         false
                                         (not mortgage-payment?))}]]]]]]
    [grid {:container true
           :direction :row
           :spacing 2}
     [grid {:item true}
      [button {:variant :outlined
               :on-click #(rf/dispatch [::we/navigate :back])} "Back"]]
     [grid {:item true}
      [button {:variant :contained
               :color :primary
               :disabled (nil? mortgage-payment?)
               :on-click #(rf/dispatch [::we/finish])}
       "Finish"]]]]])

(defn wizard []
  (rf/dispatch [:set-fab-actions nil])
  (let [properties @(rf/subscribe [::cs/properties])
        active-step @(rf/subscribe [::ws/wizard-active-step])
        property-name @(rf/subscribe [::ws/wizard-property-name])
        property-currency @(rf/subscribe [::ws/wizard-property-currency])
        rental-agent? @(rf/subscribe [::ws/wizard-rental-agent?])
        mortgage-payment? @(rf/subscribe [::ws/wizard-mortgage-payment?])]
    [paper
     [grid {:container true
            :direction :column}
      [grid {:item true}
       [stepper {:orientation :vertical
                 :active-step active-step}
        (step-property-name property-name)
        (step-property-currency property-currency)
        (step-rent-charged)
        (step-rental-agent rental-agent?)
        (step-charges)
        (step-mortgage-payment mortgage-payment?)]]
      [box {:p 2
            :visibility (if (empty? properties) :hidden :visible)}
       [grid {:container true
              :item true
              :justify :flex-end}
        [button {:color :primary
                 :on-click #(rf/dispatch [::we/skip])} "Skip"]]]]]))

