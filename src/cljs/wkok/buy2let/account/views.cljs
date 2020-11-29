(ns wkok.buy2let.account.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [clojure.string :as s]
            [fork.re-frame :as fork]
            [clojure.walk :as w]
            [wkok.buy2let.backend.events :as be]
            [wkok.buy2let.backend.subs :as bs]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.site.subs :as ss]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.core.card-actions :refer [card-actions]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]
            [reagent-material-ui.core.list-subheader :refer [list-subheader]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.paper :refer [paper]]))

(defn validate-name [values]
  (when (s/blank? (get values "name"))
    {"name" "Name is required"}))

(defn build-input
  [field {:keys [values errors touched handle-change handle-blur]}]
  (let [field-name (-> field :name name)
        error? (and (touched field-name)
                    (not (s/blank? (get errors field-name))))]
    ^{:key field-name}
    [grid {:item true}
     [text-field {:name       field-name
                  :label      (-> field-name s/capitalize)
                  :margin      :normal
                  :type       (:type field)
                  :disabled   (:disabled field)
                  :value      (values field-name "")
                  :on-change  handle-change
                  :on-blur    handle-blur
                  :error      error?
                  :helper-text (when error? (get errors field-name))}]]))

(defn edit-account [props]
  [fork/form {:form-id            "id"
              :path               :form
              :prevent-default?   true
              :clean-on-unmount?  true
              :validation         #(validate-name %)
              :on-submit-response {400 "client error"
                                   500 "server error"}
              :on-submit          #(rf/dispatch [::be/save-account (w/keywordize-keys (:values %))])
              :initial-values     (w/stringify-keys (:account @(rf/subscribe [:form-old])))}
   (fn [{:keys [form-id submitting? handle-submit] :as options}]
     [:form {:id form-id :on-submit handle-submit}
      [paper {:class (get-in props [:classes :paper])}
       [grid {:container true
              :direction :column}
        [build-input {:name :name} options]
        [grid {:container true
               :direction :row
               :justify :flex-start
               :spacing 1
               :class (get-in props [:classes :buttons])}
         [grid {:item true}
          [button {:variant :contained
                   :color :primary
                   :type :submit
                   :disabled submitting?}
           "Save"]]
         [grid {:item true}
          [button {:variant :outlined
                   :type :button
                   :on-click #(js/window.history.back)}
           "Cancel"]]]]]])])

(defn view-account [props]
  (rf/dispatch [:set-fab-actions nil])
  (let [account-id @(rf/subscribe [::bs/account])
        accounts @(rf/subscribe [::bs/accounts])
        account (when account-id (account-id accounts))]
    [grid {:container true
           :direction :row
           :spacing 2}
     [grid {:item true
            :xs 12 :md 6}
      [card
       [card-content
        [grid {:container true
               :direction :row
               :spacing 2
               :justify :center}
         [grid {:item true
                :container true
                :direction :column
                :align-items :center}
          [grid {:item true}
           [typography {:variant :h5}
            (:name account)]]]]]
       [card-actions
        [button {:color :primary
                 :on-click #(js/window.location.assign "#/account/edit")} "Edit"]]]]
     [grid {:item true
            :xs 12 :md 6}
      [grid {:container true
             :direction :column
             :spacing 2}
       [grid {:item true}
        [paper {:class (get-in props [:classes :paper])}
         [list {:subheader (ra/as-element [list-subheader "Account settings"])}
          [list-item {:button true
                      :on-click #(js/window.location.assign "#/delegates")}
           [list-item-text {:primary "Invite users"}]]
          (if (:deleteToken account)
            [list-item {:button true
                        :on-click #(rf/dispatch [::be/save-account (dissoc account :deleteToken)])}
             [list-item-text {:primary "Cancel account deletion"
                              :primary-typography-props {:color :error}}]]
            [list-item {:button true
                        :on-click #(rf/dispatch [::se/dialog {:heading "Delete account?"
                                                              :message "This will delete all data associated with this account, and is not recoverable!"
                                                              :buttons {:left  {:text     "DELETE"
                                                                                :on-click (fn [] (rf/dispatch [::be/delete-account]))
                                                                                :color :secondary}
                                                                        :right {:text "Cancel"}}}])}
             [list-item-text {:primary "Delete account"
                              :primary-typography-props {:color :error}}]])]]]]]]))


(defn account [props]
  (rf/dispatch [:set-fab-actions nil])
  (case @(rf/subscribe [::ss/active-panel])
    :account-edit [edit-account props]
    [view-account props]))