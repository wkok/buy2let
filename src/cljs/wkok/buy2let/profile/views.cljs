(ns wkok.buy2let.profile.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.backend.events :as be]
            [wkok.buy2let.backend.subs :as bs]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.profile.events :as pe]
            [clojure.walk :as w]
            [clojure.string :as s]
            [fork.re-frame :as fork]
            [reagent-material-ui.icons.account-circle :refer [account-circle]]
            [reagent-material-ui.icons.link :refer [link]]
            [reagent-material-ui.icons.cloud-upload :refer [cloud-upload]]
            [reagent-material-ui.icons.link-off :refer [link-off]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.tooltip :refer [tooltip]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.card-actions :refer [card-actions]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-item-icon :refer [list-item-icon]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]
            [reagent-material-ui.core.list-subheader :refer [list-subheader]]
            [reagent-material-ui.core.avatar :refer [avatar]]
            [reagent-material-ui.core.badge :refer [badge]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.paper :refer [paper]]))

(defn avatar-upload 
  [avatar-url-temp avatar-url {:keys [classes]} {:keys [state handle-blur]}]
  [:div
   [:input {:id        :avatar
            :name      "avatar"
            :type      :file
            :accept    "image/*"
            :style     {:display :none}
            :on-change #(rf/dispatch [::pe/upload-avatar (-> % .-target .-files (aget 0)) :temp])
            :on-blur   handle-blur}]
   [:label {:html-for :avatar}
    [tooltip {:title "Upload profile image"}
     (if avatar-url-temp
       [avatar {:src avatar-url-temp :class (:avatar-large classes)}]
       (if avatar-url
         [avatar {:src avatar-url :class (:avatar-large classes)}
          [avatar {:src avatar-url :class (:avatar-small classes)}]]
         [account-circle {:class (:avatar-large classes)}]))]]])

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

(defn validate-name [values]
  (when (s/blank? (get values "name"))
    {"name" "Name is required"}))

(defn validate-email [values]
  (when (s/blank? (get values "email"))
    {"email" "Email is required"}))

(defn edit-profile [props]
  (let [user @(rf/subscribe [::bs/local-user])
        avatar-url-temp @(rf/subscribe [::ss/avatar-url-temp])]
    [fork/form {:form-id            "id"
                :path               :form
                :prevent-default?   true
                :clean-on-unmount?  true
                :validation         #(merge (validate-name %) (validate-email %))
                :on-submit-response {400 "client error"
                                     500 "server error"}
                :on-submit          #(rf/dispatch [::pe/save-profile (w/keywordize-keys (:values %))])
                :initial-values     (w/stringify-keys (:profile @(rf/subscribe [:form-old])))}
     (fn [{:keys [form-id submitting? handle-submit] :as options}]
       [:form {:id form-id :on-submit handle-submit}
        [paper {:class (get-in props [:classes :paper])}
         [grid {:container true
                :direction :column}
          [avatar-upload avatar-url-temp (:avatar-url user) props options]
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
                     :on-click #(do (rf/dispatch [::pe/clear-temp-avatar])
                                    (js/window.history.back))}
             "Cancel"]]]]]])]))

(defn view-profile [{:keys [classes]}]
  (rf/dispatch [:set-fab-actions nil])
  (let [user @(rf/subscribe [::bs/user])
        local-user @(rf/subscribe [::bs/local-user])
        provider-data (-> user :provider-data js->clj w/keywordize-keys)]
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
         [grid {:item true}
          (if (:avatar-url local-user)
            [avatar {:src (:avatar-url local-user) :class (:avatar-large classes)}]
            [account-circle {:class (:avatar-large classes)}])]
         [grid {:item true
                :container true
                :direction :column
                :align-items :center}
          [grid {:item true}
           [typography {:variant :h5} 
            (:name local-user)]]
          [grid {:item true}
           [typography (:email local-user)]]]]]
       [card-actions
        [button {:color :primary
                 :on-click #(js/window.location.assign "#/profile/edit")} "Edit"]]]]
     [grid {:item true
            :xs 12 :md 6}
      [paper {:class (:paper classes)}
       [list {:subheader (ra/as-element [list-subheader "Sign in providers"])}
        (if (some #(= (:providerId %) "google.com") provider-data)
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/unlink :google])}
           [list-item-icon
            [link]]
           [list-item-text {:primary "Unlink Google"}]]
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/link :google])}
           [list-item-icon
            [link-off]]
           [list-item-text {:primary "Link Google"}]])
        (if (some #(= (:providerId %) "facebook.com") provider-data)
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/unlink :facebook])}
           [list-item-icon
            [link]]
           [list-item-text {:primary "Unlink Facebook"}]]
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/link :facebook])}
           [list-item-icon
            [link-off]]
           [list-item-text {:primary "Link Facebook"}]])]]]]))


(defn profile [props]
  (rf/dispatch [:set-fab-actions nil])
  (case @(rf/subscribe [::ss/active-panel])
    :profile-edit [edit-profile props]
    [view-profile props]))


