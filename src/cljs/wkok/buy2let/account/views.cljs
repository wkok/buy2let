(ns wkok.buy2let.account.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [clojure.string :as s]
            [fork.re-frame :as fork]
            [clojure.walk :as w]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.account.events :as ae]
            [wkok.buy2let.account.subs :as as]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.checkbox :refer [checkbox]]
            [reagent-material-ui.core.form-control-label :refer [form-control-label]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.core.card-actions :refer [card-actions]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]
            [reagent-material-ui.core.list-item-secondary-action :refer [list-item-secondary-action]]
            [reagent-material-ui.core.list-subheader :refer [list-subheader]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.tooltip :refer [tooltip]]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.icons.person-add :refer [person-add]]
            [reagent-material-ui.icons.card-membership :refer [card-membership]]
            [reagent-material-ui.icons.cancel :refer [cancel]]
            [reagent-material-ui.icons.delete-icon :refer [delete]]
            [reagent-material-ui.core.paper :refer [paper]]))

(defn select-account []
  (let [accounts @(rf/subscribe [::as/accounts])]
    [text-field {:select true
                 :label "Account"
                 :field     :list
                 :on-change #(rf/dispatch [:select-account (-> % .-target .-value keyword)])
                 :value     (if (empty? accounts)
                              ""
                              @(rf/subscribe [::as/selected-account-id]))}
     (for [account accounts]
       ^{:key (key account)}
       [menu-item {:value (key account)}
        (-> account val :name)])]))

(defn remember-account-box []
  [form-control-label
   {:control (ra/as-element
              [checkbox {:checked @(rf/subscribe [::as/remember-account])
                         :color :primary
                         :on-change #(rf/dispatch [::ae/remember-account (-> % .-target .-checked)])}])
    :label "Remember my choice"}])

(defn account-dialog [user]
  {:heading   "Which account?"
   :message   "Please choose the account you want to access"
   :panel     [grid {:container true
                     :direction :column
                     :spacing 2}
               [grid {:item true}
                [select-account]]
               [grid {:item true}
                [remember-account-box]]]
   :buttons   {:middle {:text     "Continue"
                        :on-click #(let [remember-account @(rf/subscribe [::as/remember-account])
                                         selected-account-id @(rf/subscribe [::as/selected-account-id])]
                                     (rf/dispatch [::ae/save-default-account user remember-account selected-account-id])
                                     (rf/dispatch [:set-active-account selected-account-id])
                                     (rf/dispatch [::se/dialog]))}}
   :closeable false})

(rf/reg-event-db
 ::choose-account
 (fn [db [_ _]]
   (let [user (get-in db [:security :user])
         account-id (get-in db [:security :account])
         accounts (get-in db [:security :accounts])
         role-accounts (-> db :security :claims :roles shared/accounts-from)
         selected-account-id (get-in db [:site :account-selector :account-id])
         default-value (or selected-account-id
                           account-id
                           (when (not (empty? accounts))
                             (-> accounts first key))
                           (first role-accounts))]
     (rf/dispatch [::se/dialog (account-dialog user)])
     (if (not (get-in db [:site :account-selector :account-id]))
       (-> (assoc-in db [:site :account-selector :account-id] default-value)
           (assoc-in [:site :account-selector :remember] (if (:default-account-id user)
                                                           true false)))
       db))))

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

(defn avatar-upload
  [avatar-url-temp avatar-url {:keys [classes]} {:keys [handle-blur]}]
  [:div
   [:input {:id        :avatar
            :name      "avatar"
            :type      :file
            :accept    "image/*"
            :style     {:display :none}
            :on-change #(let [file (-> % .-target .-files (aget 0))]
                          (when (shared/validate-file-size file 1000000)
                            (rf/dispatch [::ae/upload-avatar (-> % .-target .-files (aget 0)) :temp])))
            :on-blur   handle-blur}]
   [:label {:html-for :avatar}
    [tooltip {:title "Upload account image"}
     (if avatar-url-temp
       [avatar {:src avatar-url-temp :variant :rounded :class (:avatar-large classes)} ":)"]
       [avatar {:src avatar-url :variant :rounded :class (:avatar-large classes)} ":)"])]]])

(defn edit-account [props]
  (let [account-id @(rf/subscribe [::as/account])
        accounts @(rf/subscribe [::as/accounts])
        account (when account-id (account-id accounts))
        avatar-url-temp @(rf/subscribe [::ss/account-avatar-url-temp])]
    [fork/form {:form-id            "id"
                :path               :form
                :prevent-default?   true
                :clean-on-unmount?  true
                :validation         #(validate-name %)
                :on-submit-response {400 "client error"
                                     500 "server error"}
                :on-submit          #(rf/dispatch [::ae/save-account (w/keywordize-keys (:values %))])
                :initial-values     (w/stringify-keys (:account @(rf/subscribe [:form-old])))}
     (fn [{:keys [form-id submitting? handle-submit] :as options}]
       [:form {:id form-id :on-submit handle-submit}
        [paper {:class (get-in props [:classes :paper])}
         [grid {:container true
                :direction :column}
          [avatar-upload avatar-url-temp (or (:avatar-url account) "images/icon/icon-128.png") props options]
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
                     :on-click #(do (rf/dispatch [::ae/clear-temp-avatar])
                                    (js/window.history.back))}
             "Cancel"]]]]]])]))

(defn account-card [account accounts classes]
  [card
   [card-content
    [grid {:container true
           :direction :row
           :spacing 2
           :justify :center}
     [grid {:item true}
      [avatar {:src (or (:avatar-url account) "images/icon/icon-128.png")
               :variant :rounded
               :class (:avatar-large classes)} ":)"]]
     [grid {:item true
            :container true
            :direction :column
            :align-items :center}
      [grid {:item true}
       [typography {:variant :h5}
        (:name account)]]]]]
   [card-actions
    (when (shared/has-role :owner)
      [button {:color :primary
               :on-click #(js/window.location.assign "#/account/edit")} "Edit"])
    (when (second accounts) ; More that one account
      [button {:color :primary
               :on-click #(rf/dispatch [::choose-account])} "Switch"])]])

(defn account-settings [account classes]
  (let [subs-properties (get-in account [:subscription :properties] 1)
        on-delegate #(js/window.location.assign "#/delegates")
        on-subscription #(js/window.location.assign "#/subscription")
        on-delete-cancel #(rf/dispatch [::ae/save-account (dissoc account :deleteToken)])
        on-delete #(rf/dispatch [::se/dialog {:heading "Delete account?"
                                              :message "This will delete all data associated with this account, and is not recoverable!"
                                              :buttons {:left  {:text     "DELETE"
                                                                :on-click (fn [] (rf/dispatch [::ae/delete-account]))
                                                                :color :secondary}
                                                        :right {:text "Cancel"}}}])]
    [paper {:class (:paper classes)}
     [list {:subheader (ra/as-element [list-subheader "Account settings"])}
      [list-item {:button true
                  :on-click on-delegate}
       [list-item-text {:primary "Delegate access"}]
       [list-item-secondary-action
        [icon-button {:edge :end
                      :on-click on-delegate}
         [person-add]]]]
      [list-item {:button true
                  :on-click on-subscription}
       [list-item-text {:primary "Subscription"
                        :secondary (str subs-properties
                                        (if (> subs-properties 1)
                                          " properties" " property"))}]
       [list-item-secondary-action
        [icon-button {:edge :end
                      :on-click on-subscription}
         [card-membership]]]]
      (if (:deleteToken account)
        [list-item {:button true
                    :on-click on-delete-cancel}
         [list-item-text {:primary "Cancel account deletion"
                          :primary-typography-props {:color :error}}]
         [list-item-secondary-action
          [icon-button {:edge :end
                        :on-click on-delete-cancel}
           [cancel]]]]
        [list-item {:button true
                    :on-click on-delete}
         [list-item-text {:primary "Delete account"
                          :primary-typography-props {:color :error}}]
         [list-item-secondary-action
          [icon-button {:edge :end
                        :on-click on-delete}
           [delete]]]])]]))

(defn view-account [{:keys [classes]}]
  (rf/dispatch [:set-fab-actions nil])
  (let [account-id @(rf/subscribe [::as/account])
        accounts @(rf/subscribe [::as/accounts])
        account (when account-id (account-id accounts))]
    [grid {:container true
           :direction :row
           :spacing 2}
     [grid {:item true
            :xs 12 :md 6}
      [account-card account accounts classes]]
     (when (shared/has-role :owner)
       [grid {:item true
              :xs 12 :md 6}
        [grid {:container true
               :direction :column
               :spacing 2}
         [grid {:item true}
          [account-settings account classes]]]])]))


(defn account [props]
  (rf/dispatch [:set-fab-actions nil])
  (case @(rf/subscribe [::ss/active-panel])
    :account-edit [edit-account props]
    [view-account props]))