(ns wkok.buy2let.site.views
  (:require
   [re-frame.core :as rf]
   [wkok.buy2let.site.subs :as subs]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.site.dialog :as dialog]
   [wkok.buy2let.reconcile.views :as reconcile]
   [wkok.buy2let.reconcile.events :as re]
   [wkok.buy2let.report.views :as report]
   [wkok.buy2let.report.events :as repe]
   [wkok.buy2let.dashboard.views :as dashboard]
   [wkok.buy2let.settings.views :as settings]
   [wkok.buy2let.profile.views :as profile]
   [wkok.buy2let.account.views :as account]
   [wkok.buy2let.wizard.views :as wizard]
   [wkok.buy2let.crud.impl :as crud-impl]
   [wkok.buy2let.crud.subs :as cs]
   [wkok.buy2let.backend.events :as be]
   [wkok.buy2let.backend.subs :as bs]
   [wkok.buy2let.account.subs :as as]
   [wkok.buy2let.backend.multimethods :as mm]
   [reagent-material-ui.icons.account-circle :refer [account-circle]]
   [reagent-material-ui.icons.dashboard :refer [dashboard]]
   [reagent-material-ui.icons.receipt :refer [receipt]]
   [reagent-material-ui.icons.menu :as icons-menu]
   [reagent-material-ui.icons.category :refer [category]]
   [reagent-material-ui.icons.assessment :refer [assessment]]
   [reagent-material-ui.icons.apartment :refer [apartment]]
   [reagent-material-ui.cljs-time-utils :refer [cljs-time-utils]]
   [reagent-material-ui.colors :as colors]
   [reagent-material-ui.core.css-baseline :refer [css-baseline]]
   [reagent-material-ui.core.snackbar :refer [snackbar]]
   [reagent-material-ui.lab.alert :refer [alert]]
   [reagent-material-ui.core.menu :refer [menu]]
   [reagent-material-ui.core.menu-item :refer [menu-item]]
   [reagent-material-ui.core.grid :refer [grid]]
   [reagent-material-ui.core.fab :refer [fab]]
   [reagent-material-ui.core.avatar :refer [avatar]]
   [reagent-material-ui.core.tooltip :refer [tooltip]]
   [reagent-material-ui.core.button :refer [button]]
   [reagent-material-ui.core.app-bar :refer [app-bar]]
   [reagent-material-ui.core.divider :refer [divider]]
   [reagent-material-ui.core.hidden :refer [hidden]]
   [reagent-material-ui.core.text-field :refer [text-field]]
   [reagent-material-ui.core.drawer :refer [drawer]]
   [reagent-material-ui.core.backdrop :refer [backdrop]]
   [reagent-material-ui.core.list :refer [list]]
   [reagent-material-ui.core.linear-progress :refer [linear-progress]]
   [reagent-material-ui.core.circular-progress :refer [circular-progress]]
   [reagent-material-ui.core.list-item :refer [list-item]]
   [reagent-material-ui.core.list-item-icon :refer [list-item-icon]]
   [reagent-material-ui.core.list-item-text :refer [list-item-text]]
   [reagent-material-ui.core.typography :refer [typography]]
   [reagent-material-ui.core.icon-button :refer [icon-button]]
   [reagent-material-ui.core.toolbar :refer [toolbar]]
   [reagent-material-ui.pickers.mui-pickers-utils-provider :refer [mui-pickers-utils-provider]]
   [reagent-material-ui.styles :as styles]
   [reagent-material-ui.core.dialog :refer [dialog]]
   [reagent-material-ui.core.dialog-title :refer [dialog-title]]
   [reagent-material-ui.core.dialog-content :refer [dialog-content]])
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

(defn fab-button [props]
  (when-let [actions @(rf/subscribe [::subs/fab-actions])]
    [tooltip {:title (-> actions :left-1 :title)}
     [fab {:color :primary
           :class (get-in props [:classes :fab])
           :on-click (-> actions :left-1 :fn)}
      (-> actions :left-1 :icon)]]))

(defn splash [props]
  [backdrop {:open @(rf/subscribe [::subs/splash])
             :class (get-in props [:classes :splash])}
   [circular-progress]])

(def custom-theme
  {:palette {:primary   colors/blue}})

(defn custom-styles [{:keys [spacing breakpoints z-index palette]}]
  (let [up (:up breakpoints)
        drawer-width 200]
    {:root {:display :flex}
     :drawer {(up "sm") {:width drawer-width, :flex-shrink 0}}
     :app-bar {(up "sm") {:width (str "calc(100% - " drawer-width "px)") :margin-left drawer-width}}
     :toolbar {:margin-top "-4px"}
     :title {:flex-grow 1}
     :avatar-small {:width (spacing 3)
                    :height (spacing 3)}
     :avatar-medium {:width (spacing 7)
                     :height (spacing 7)}
     :avatar-large {:width (spacing 10)
                    :height (spacing 10)}
     :pos {:color "blue"}
     :neg {:color "red"}
     :owe {:color "orange"}
     :table-header {:font-weight 600}
     :table-header-alternate {:font-weight 600
                              :background-color (get-in palette [:action :hover])}
     :table-header-pos {:color "blue"
                        :font-weight 600}
     :table-header-neg {:color "red"
                        :font-weight 600}
     :table-header-owe {:color "orange"
                        :font-weight 600}
     :table-header-alternate-pos {:color "blue"
                                  :font-weight 600
                                  :background-color (get-in palette [:action :hover])}
     :table-header-alternate-neg {:color "red"
                                  :font-weight 600
                                  :background-color (get-in palette [:action :hover])}
     :table-header-alternate-owe {:color "orange"
                                  :font-weight 600
                                  :background-color (get-in palette [:action :hover])}
     :table-alternate {:background-color (get-in palette [:action :hover])}
     :brand-logo {:padding "0.5em"}
     :brand-name {:padding "0.5em"}
     :menu-button {(up "sm") {:display :none}
                   :margin-right (spacing 2)}
     :drawer-paper {:width drawer-width}
     :reconcile-card {:height :7em}
     :content {:flex-grow 1
               :padding (spacing 2)
               :padding-top (spacing 8)
               :padding-bottom (spacing 10)
               :overflow-x :hidden}
     :buttons {:padding-top (spacing 1)}
     :fab {:position :fixed
           :bottom (spacing 2)
           :right (spacing 2)
           :z-index (+ (:drawer z-index) 1)}
     :splash {:z-index (+ (:drawer z-index) 1)}
     :wizard-actions {:margin-top (spacing 2)}
     :who-pays-whom {:padding-left (spacing 4)}
     :paper {:padding (spacing 2)}}))

(def with-custom-styles (styles/with-styles custom-styles))

(defn handle-drawer-toggle []
  (let [mobile-open @(rf/subscribe [::subs/nav-menu-show])]
    (rf/dispatch [::se/show-nav-menu (not mobile-open)])))

(defn progress-bar []
  (if @(rf/subscribe [::subs/show-progress])
    [linear-progress {:variant :indeterminate}]
    [linear-progress {:variant :determinate
                      :value 100}]))

(defn profile [classes]
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

(defn header [{:keys [classes]}]
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
    [profile classes]]])

(defn navigate [hash]
  (js/window.location.assign hash)
  (rf/dispatch [::se/show-nav-menu false]))

(defn brand [{:keys [classes]}]
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
            }
      [avatar {:src (or (:avatar-url account) "images/icon/icon-128.png")
               :variant :rounded
               :class (:avatar-medium classes)} ":)"]]
     [grid {:item true
            :xs 8
            :container true
            :direction :column
            :class (:brand-name classes)}
      [grid {:item true}
       [typography {:variant :h4 :color :primary} (mm/app-name)]]
      [grid {:item true}
       [typography {:variant :caption} (:name account)]]]]))

(defn nav [{:keys [classes] :as props}]
  (let [properties @(rf/subscribe [::cs/properties])
        drawer_ [:div
                 [brand props]
                 [divider]
                 [list {:style {:display (when (empty? properties) :none)}}
                  [list-item {:button true
                              :on-click #(navigate "#/")}
                   [list-item-icon [dashboard]]
                   [list-item-text {:primary "Dashboard"}]]
                  [list-item {:button true
                              :on-click #(navigate (build-reconcile-url))}
                   [list-item-icon [receipt]]
                   [list-item-text {:primary "Reconcile"}]]
                  [list-item {:button true
                              :on-click #(navigate (build-report-url))}
                   [list-item-icon [assessment]]
                   [list-item-text {:primary "Report"}]]
                  [list-item {:button true
                              :on-click #(navigate "#/properties")}
                   [list-item-icon [apartment]]
                   [list-item-text {:primary "Properties"}]]
                  [list-item {:button true
                              :on-click #(navigate "#/charges")}
                   [list-item-icon [category]]
                   [list-item-text {:primary "Charges"}]]
                  ;; [list-item {:button true
                  ;;             :on-click #(navigate "#/settings")}
                  ;;  [list-item-icon [settings]]
                  ;;  [list-item-text {:primary "Settings"}]]
                  ]]]
    [:nav {:class (:drawer classes)}
     [hidden {:sm-up true}
      [drawer {:container (.. js/window -document -body)
               :variant :temporary
               :anchor :left
               :open (or @(rf/subscribe [::subs/nav-menu-show]) false)
               :on-close handle-drawer-toggle
               :classes {:paper (:drawer-paper classes)}
               :Modal-props {:keep-mounted true}}
       drawer_]]
     [hidden {:xs-down true}
      [drawer {:classes {:paper (:drawer-paper classes)}
               :variant :permanent
               :open true}
       drawer_]]]))

(defn main [{:keys [classes] :as props}]
  [:main {:class (:content classes)}
   (when-let [active-page @(rf/subscribe [::subs/active-page])]
     (condp = active-page
       :wizard [wizard/wizard]
       :dashboard [dashboard/dashboard]
       :reconcile [reconcile/reconcile props]
       :report [report/report props]
       :properties [crud-impl/properties props]
       :charges [crud-impl/charges props]
       :delegates [crud-impl/delegates props]
       :settings [settings/settings props]
       :profile [profile/profile props]
       :account [account/account props]))])

(defn sign-in-panel []
  [:div
   [css-baseline]
   [styles/theme-provider (styles/create-mui-theme custom-theme)
    [(with-custom-styles
       (fn [{:keys [classes]}]
         [:div {:class (:root classes)}
          [dialog {:open true
                   :disable-backdrop-click true
                   :disable-escape-key-down true}
           [dialog-title "Sign in"]
           [dialog-content 
            [grid {:container true
                   :direction :column
                   :align-items :center
                   :spacing 2}
             [grid {:item true}
              [text-field {:label "Email" :type :email :value "demo@email.com" :on-change #()}]]
             [grid {:item true}
              [text-field {:label "Password" :type :password :value "***********" :on-change #()}]]
             [grid {:item true}
              [button {:variant :contained :color :primary :on-click #(rf/dispatch [::be/sign-in :google])} "Sign in"]]]]]]))]]])

(defn error-snack []
  (let [account-id @(rf/subscribe [::as/account])
        accounts @(rf/subscribe [::as/accounts])
        account (when account-id (account-id accounts))
        error (when (:deleteToken account)
                "Account deletion initiated. You may cancel this in Account settings")]
    [snackbar {:open (if error true false)
               :anchor-origin {:vertical :bottom
                               :horizontal :center}}
     [alert {:severity :error} error]]))

(defn main-panel []
  [:div
   [css-baseline]
   [mui-pickers-utils-provider {:utils  cljs-time-utils
                                :locale DateTimeSymbols_en_US}
    [styles/theme-provider (-> (styles/create-mui-theme custom-theme)
                               (styles/responsive-font-sizes))
     [grid {:container true
            :direction :row
            :justify   :flex-start}
      [grid {:item true
             :xs   12}
       [(with-custom-styles
          (fn [{:keys [classes] :as props}]
            [:div {:class (:root classes)}
             [error-snack]
             [splash props]
             [fab-button props]
             [header props]
             [nav props]
             [main props]
             [dialog/create-dialog]]))]]]]]])




