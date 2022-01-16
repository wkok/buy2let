(ns wkok.buy2let.site.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [clojure.string :as str]
   [wkok.buy2let.site.styles :refer [classes custom-styles from-theme]]
   [wkok.buy2let.site.subs :as subs]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.site.dialog :as dialog]
   [wkok.buy2let.reconcile.views :as reconcile]
   [wkok.buy2let.reconcile.events :as re]
   [wkok.buy2let.report.views :as report]
   [wkok.buy2let.report.events :as repe]
   [wkok.buy2let.dashboard.views :as dashboard]
   [wkok.buy2let.about.views :as about]
   [wkok.buy2let.opensource.views :as opensource]
   [wkok.buy2let.subscription.views :as subscription]
   [wkok.buy2let.profile.views :as profile]
   [wkok.buy2let.account.views :as account]
   [wkok.buy2let.wizard.views :as wizard]
   [wkok.buy2let.crud.impl :as crud-impl]
   [wkok.buy2let.crud.subs :as cs]
   [wkok.buy2let.backend.events :as be]
   [wkok.buy2let.backend.subs :as bs]
   [wkok.buy2let.account.subs :as as]
   [wkok.buy2let.backend.multimethods :as mm]
   [reagent-mui.icons.account-circle :refer [account-circle]]
   [reagent-mui.icons.dashboard :refer [dashboard]]
   [reagent-mui.icons.receipt :refer [receipt]]
   [reagent-mui.icons.menu :as icons-menu]
   [reagent-mui.icons.category :refer [category]]
   [reagent-mui.icons.info :refer [info]]
   [reagent-mui.icons.assessment :refer [assessment]]
   [reagent-mui.icons.apartment :refer [apartment]]
   [reagent-mui.cljs-time-adapter :refer [cljs-time-adapter]]
   [reagent-mui.lab.localization-provider :refer [localization-provider]]
   [reagent-mui.colors :as colors]
   [reagent-mui.material.css-baseline :refer [css-baseline]]
   [reagent-mui.material.snackbar :refer [snackbar]]
   [reagent-mui.material.alert :refer [alert]]
   [reagent-mui.material.menu :refer [menu]]
   [reagent-mui.material.menu-item :refer [menu-item]]
   [reagent-mui.material.grid :refer [grid]]
   [reagent-mui.material.fab :refer [fab]]
   [reagent-mui.material.avatar :refer [avatar]]
   [reagent-mui.material.tooltip :refer [tooltip]]
   [reagent-mui.material.button :refer [button]]
   [reagent-mui.material.app-bar :refer [app-bar]]
   [reagent-mui.material.divider :refer [divider]]
   [reagent-mui.material.text-field :refer [text-field]]
   [reagent-mui.material.drawer :refer [drawer]]
   [reagent-mui.material.backdrop :refer [backdrop]]
   [reagent-mui.material.list :refer [list]]
   [reagent-mui.material.linear-progress :refer [linear-progress]]
   [reagent-mui.material.circular-progress :refer [circular-progress]]
   [reagent-mui.material.list-item :refer [list-item]]
   [reagent-mui.material.list-item-icon :refer [list-item-icon]]
   [reagent-mui.material.list-item-text :refer [list-item-text]]
   [reagent-mui.material.typography :refer [typography]]
   [reagent-mui.material.icon-button :refer [icon-button]]
   [reagent-mui.material.box :refer [box]]
   [reagent-mui.material.toolbar :refer [toolbar]]
   [reagent-mui.material.bottom-navigation :refer [bottom-navigation]]
   [reagent-mui.material.bottom-navigation-action :refer [bottom-navigation-action]]
   [reagent-mui.styles :as styles]
   [reagent-mui.material.dialog :refer [dialog]]
   [reagent-mui.material.dialog-title :refer [dialog-title]]
   [reagent-mui.material.dialog-content :refer [dialog-content]]
   [clojure.walk :as w])
  (:import (goog.i18n DateTimeSymbols_en_US)))

(defn build-reconcile-url []
  (let [options (re/calc-options {})]
    (str "#/reconcile/" (-> (:property-id options) name)
         "/" (-> (:month options) name)
         "/" (-> (:year options) name))))

(defn build-report-url []
  (let [options (repe/calc-options {})]
    (str "#/report/" (-> (:property-id options) name)
         "/" (-> (:from-month options) name)
         "/" (-> (:from-year options) name)
         "/" (-> (:to-month options) name)
         "/" (-> (:to-year options) name))))

(defn fab-button []
  (when-let [actions @(rf/subscribe [::subs/fab-actions])]
    [tooltip {:title (-> actions :left-1 :title)}
     [fab {:color :primary
           :class (:fab classes)
           :on-click (-> actions :left-1 :fn)}
      (-> actions :left-1 :icon)]]))

(defn splash []
  [backdrop {:open @(rf/subscribe [::subs/splash])
             :class (:splash classes)}
   [circular-progress]])

(defn handle-drawer-toggle []
  (let [mobile-open @(rf/subscribe [::subs/nav-menu-show])]
    (rf/dispatch [::se/show-nav-menu (not mobile-open)])))

(defn progress-bar []
  (if @(rf/subscribe [::subs/show-progress])
    [linear-progress {:variant :indeterminate}]
    [linear-progress {:variant :determinate
                      :value 100}]))

(defn profile []
  (let [target @(rf/subscribe [::subs/profile-menu-show])
        handle-close #(rf/dispatch [::se/toggle-profile-menu nil])
        profile-menu-fn #(rf/dispatch [::se/toggle-profile-menu (.-currentTarget %)])]
    [:div
     (if-let [avatar-url (:avatar-url @(rf/subscribe [::bs/local-user]))]
       [icon-button {:on-click profile-menu-fn}
        [avatar {:src avatar-url
                 :class (:avatar-small classes)}]]
       [icon-button {:color :inherit
                     :on-click profile-menu-fn}
        [account-circle]])
     [menu {:open (if target true false)
            :on-close handle-close
            :anchor-el target}
      [menu-item {:on-click #(do (js/window.location.assign "#/profile")
                                 (handle-close))} "My profile"]
      [menu-item {:on-click #(do (js/window.location.assign "#/account")
                                 (handle-close))} "Account settings"]
      [divider]
      [menu-item {:on-click #(rf/dispatch [:sign-out])} "Sign out"]]]))

(defn header []
  [app-bar {:position :fixed
            :class (:app-bar classes)}
   [progress-bar]
   [toolbar {:variant :dense
             :class (:toolbar classes)}
    [icon-button {:edge :start
                  :color :inherit
                  :class (:menu-button classes)
                  :on-click handle-drawer-toggle}
     [icons-menu/menu]]
    [typography {:variant :h5
                 :no-wrap true
                 :class (:title classes)} @(rf/subscribe [::subs/heading])]
    [profile]]])

(defn navigate [view]
  (let [properties @(rf/subscribe [::cs/properties])
        hash (if (empty? properties)
               "#/properties/add"
               (case view
                 :dashboard "#/"
                 :reconcile (build-reconcile-url)
                 :report (build-report-url)
                 :properties "#/properties"
                 :charges "#/charges"
                 :about "#/about"))]
    (js/window.location.assign hash)
    (rf/dispatch [::se/show-nav-menu false])))

(defn brand []
  (let [account-id  @(rf/subscribe [::as/account])
        accounts @(rf/subscribe [::as/accounts])
        account (when (and account-id accounts)
                  (account-id accounts))]
    [grid {:container true
           :direction :row
           :wrap :nowrap
           :align-items :center}
     [grid {:item true
            :xs 4
            :class (:brand-logo classes)
            :sx {:padding #((from-theme % :spacing) 1)}}
      [avatar {:src (or (:avatar-url account) "images/icon/icon-128.png")
               :variant :rounded
               :class (:avatar-medium classes)
               :sx {:width #((from-theme % :spacing) 7)
                    :height #((from-theme % :spacing) 7)}} ":)"]]
     [grid {:item true
            :xs 8
            :container true
            :direction :column
            :class (:brand-name classes)
            :sx {:padding #((from-theme % :spacing) 1)}}
      [grid {:item true}
       [typography {:variant :h4 :color :primary} (mm/app-name)]]
      [grid {:item true}
       [typography {:variant :caption} (:name account)]]]]))

(defn nav []
  (let [drawer_ [:div
                 [brand]
                 [divider]
                 [list
                  [list-item {:button true
                              :on-click #(navigate :dashboard)}
                   [list-item-icon [dashboard]]
                   [list-item-text {:primary "Dashboard"}]]
                  [list-item {:button true
                              :on-click #(navigate :reconcile)}
                   [list-item-icon [receipt]]
                   [list-item-text {:primary "Reconcile"}]]
                  [list-item {:button true
                              :on-click #(navigate :report)}
                   [list-item-icon [assessment]]
                   [list-item-text {:primary "Report"}]]
                  [list-item {:button true
                              :on-click #(navigate :properties)}
                   [list-item-icon [apartment]]
                   [list-item-text {:primary "Properties"}]]
                  [list-item {:button true
                              :on-click #(navigate :charges)}
                   [list-item-icon [category]]
                   [list-item-text {:primary "Charges"}]]
                  [divider]
                  [list-item {:button true
                              :on-click #(navigate :about)}
                   [list-item-icon [info]]
                   [list-item-text {:primary "About"}]]]]]
    [:nav {:class (:drawer classes)}
     [drawer {:variant :temporary
              :anchor :left
              :open (or @(rf/subscribe [::subs/nav-menu-show]) false)
              :on-close handle-drawer-toggle
              :class (:drawer-paper classes)
              :sx {:display {:xs :block :sm :none}}}
      drawer_]
     [drawer {:class (:drawer-paper classes)
              :variant :permanent
              :open true
              :sx {:display {:xs :none :sm :block}}}
      drawer_]]))

(defn bottom-nav []
  (let [active-page @(rf/subscribe [::subs/active-page])
        active-panel @(rf/subscribe [::subs/active-panel])]
    [box {:position :fixed
          :bottom 0
          :width "100%"
          :display {:xs :block :sm :none}
          :border-top 0.1
          :border-color "text.disabled"
          :visibility (if (or (and active-panel (str/ends-with? (name active-panel) "-edit"))
                              (= :wizard active-page))
                        :hidden :visible)}
     [bottom-navigation {:show-labels true
                         :value active-page
                         :on-change (fn [_ val]
                                      (case (keyword val)
                                        :reconcile (navigate :reconcile)
                                        :report (navigate :report)
                                        (navigate :dashboard)))}
      [bottom-navigation-action {:label "Dashboard"
                                 :icon (ra/as-element [dashboard])
                                 :value :dashboard}]
      [bottom-navigation-action {:label "Reconcile"
                                 :icon (ra/as-element [receipt])
                                 :value :reconcile}]
      [bottom-navigation-action {:label "Report"
                                 :icon (ra/as-element [assessment])
                                 :value :report}]]]))

(defn main []
  [:main {:class (:content classes)}
   (when-let [active-page @(rf/subscribe [::subs/active-page])]
     (condp = active-page
       :wizard [wizard/wizard]
       :dashboard [dashboard/dashboard]
       :reconcile [reconcile/reconcile]
       :report [report/report]
       :properties [crud-impl/properties]
       :charges [crud-impl/charges]
       :invoices [crud-impl/invoices]
       :delegates [crud-impl/delegates]
       :subscription [subscription/subscription]
       :profile [profile/profile]
       :account [account/account]
       :about [about/about]
       :opensource [opensource/opensource]))])

(defn sign-in-form* [{:keys [class-name]}]
  [:div {:class [class-name (:root classes)]}
   [dialog {:open true
            :on-close (fn [_event reason]
                        (when (#{"backdropClick" "escapeKeyDown"} reason)
                          #_"Do Nothing"))}
    [dialog-title "Sign in"]
    [dialog-content
     [grid {:container true
            :direction :column
            :align-items :center
            :spacing 2}
      [grid {:item true}
       [text-field {:variant :standard :label "Email" :type :email :value "demo@email.com" :on-change #()}]]
      [grid {:item true}
       [text-field {:variant :standard :label "Password" :type :password :value "***********" :on-change #()}]]
      [grid {:item true}
       [button {:variant :contained :color :primary :on-click #(rf/dispatch [::be/sign-in :google])} "Sign in"]]]]]])

(def sign-in-form (styles/styled sign-in-form* custom-styles))

(defn sign-in-panel []
  [:div
   [css-baseline]
   [styles/theme-provider (styles/create-theme {:palette {:primary colors/blue}})
    [sign-in-form]]])

(defn error-snack []
  (let [error @(rf/subscribe [::subs/snack-error])]
    [snackbar {:open (if error true false)
               :anchor-origin {:vertical :bottom
                               :horizontal :center}}
     [alert {:severity :error
             :on-close #(rf/dispatch [::se/set-snack-error])} error]]))

(defn main-form* [{:keys [class-name]}]
  [:div {:class [class-name (:root classes)]}
   [error-snack]
   [splash]
   [fab-button]
   [header]
   [nav]
   [main]
   [bottom-nav]
   [dialog/create-dialog]
   [dialog/active-properties-dialog]])

(def main-form (styles/styled main-form* custom-styles))

(defn main-panel []
  (let [mode @(rf/subscribe [::bs/mode])]
    [:div
     [css-baseline]
     [localization-provider {:date-adapter cljs-time-adapter
                             :locale DateTimeSymbols_en_US}
      [styles/theme-provider (-> (styles/create-theme {:palette {:primary (case mode
                                                                            :live colors/blue
                                                                            :test colors/pink)
                                                                 :secondary (case mode
                                                                              :live colors/pink
                                                                              :test colors/blue)}})
                               (styles/responsive-font-sizes))
       [grid {:container true
              :direction :row
              :justify-content :flex-start}
        [grid {:item true
               :xs   12}
         [main-form]]]]]]))
