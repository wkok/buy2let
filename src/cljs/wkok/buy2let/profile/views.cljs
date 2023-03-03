(ns wkok.buy2let.profile.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.backend.events :as be]
            [wkok.buy2let.backend.subs :as bs]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.profile.events :as pe]
            [wkok.buy2let.site.styles :refer [classes]]
            [clojure.walk :as w]
            [clojure.string :as s]
            [fork.re-frame :as fork]
            [reagent-mui.icons.account-circle :refer [account-circle]]
            [reagent-mui.icons.link :refer [link]]
            [reagent-mui.icons.link-off :refer [link-off]]
            [reagent-mui.material.text-field :refer [text-field]]
            [reagent-mui.material.list :refer [list]]
            [reagent-mui.material.tooltip :refer [tooltip]]
            [reagent-mui.material.card :refer [card]]
            [reagent-mui.material.typography :refer [typography]]
            [reagent-mui.material.button :refer [button]]
            [reagent-mui.material.card-content :refer [card-content]]
            [reagent-mui.material.card-actions :refer [card-actions]]
            [reagent-mui.material.list-item :refer [list-item]]
            [reagent-mui.material.list-item-icon :refer [list-item-icon]]
            [reagent-mui.material.list-item-text :refer [list-item-text]]
            [reagent-mui.material.list-subheader :refer [list-subheader]]
            [reagent-mui.material.avatar :refer [avatar]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.paper :refer [paper]]))

(defn avatar-upload
  [avatar-url-temp avatar-url {:keys [handle-blur]}]
  [:div
   [:input {:id        :avatar
            :name      "avatar"
            :type      :file
            :accept    "image/*"
            :style     {:display :none}
            :on-change #(let [file (-> % .-target .-files (aget 0))]
                          (when (shared/validate-file-size file 1000000)
                            (rf/dispatch [::pe/upload-avatar (-> % .-target .-files (aget 0)) :temp])))
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
     [text-field {:variant :standard
                  :name       field-name
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

(defn edit-profile []
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
        [paper {:class (:paper classes)}
         [grid {:container true
                :direction :column}
          [avatar-upload avatar-url-temp (:avatar-url user) options]
          [build-input {:name :name} options]
          [build-input {:name :email
                        :type :email} options]
          [grid {:container true
                 :direction :row
                 :justify-content :flex-start
                 :spacing 1
                 :class (:buttons classes)}
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

(defn view-profile []
  (rf/dispatch [:set-fab-actions nil])
  (let [claims @(rf/subscribe [::bs/claims])
        local-user @(rf/subscribe [::bs/local-user])
        providers (set @(rf/subscribe [::bs/providers]))]
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
               :justify-content :center}
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
          [grid {:item true
                 :container true
                 :justify-content :center
                 :spacing 1}
           [grid {:item true}
            [typography (:email local-user)]]
           (when (not= (:email claims) (:email local-user))
             [grid {:item true}
              [typography {:color :secondary} "(UNVERIFIED - Check your email)"]])]]]]
       [card-actions
        [button {:color :primary
                 :on-click #(js/window.location.assign "#/profile/edit")} "Edit"]]]]
     [grid {:item true
            :xs 12 :md 6}
      [paper {:class (:paper classes)}
       [list {:subheader (ra/as-element [list-subheader "Sign in providers"])}
        (if (providers :google.com)
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/unlink :google.com])}
           [list-item-icon
            [link]]
           [list-item-text {:primary "Unlink Google"}]]
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/link :google.com])}
           [list-item-icon
            [link-off]]
           [list-item-text {:primary "Link Google"}]])
        (if (providers :facebook.com)
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/unlink :facebook.com])}
           [list-item-icon
            [link]]
           [list-item-text {:primary "Unlink Facebook"}]]
          [list-item {:button true
                      :on-click #(rf/dispatch [::be/link :facebook.com])}
           [list-item-icon
            [link-off]]
           [list-item-text {:primary "Link Facebook"}]])]]]]))


(defn profile []
  (rf/dispatch [:set-fab-actions nil])
  (case @(rf/subscribe [::ss/active-panel])
    :profile-edit [edit-profile]
    [view-profile]))
