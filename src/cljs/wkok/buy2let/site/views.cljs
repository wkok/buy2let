(ns wkok.buy2let.site.views
  (:require
   [re-frame.core :as rf]
   [clojure.string :as s]
   [wkok.buy2let.site.subs :as subs]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.site.dialog :as dialog]
   [wkok.buy2let.reconcile.views :as reconcile]
   [wkok.buy2let.reconcile.events :as re]
   [wkok.buy2let.report.views :as report]
   [wkok.buy2let.report.events :as repe]
   [wkok.buy2let.dashboard.views :as dashboard]
   [wkok.buy2let.settings.views :as settings]
   [wkok.buy2let.crud.impl :as crud-impl]
   [wkok.buy2let.backend.events :as be]
   [wkok.buy2let.backend.subs :as bs]
   [reagent-material-ui.icons.dashboard :refer [dashboard]]
   [reagent-material-ui.icons.receipt :refer [receipt]]
   [reagent-material-ui.icons.account-circle :refer [account-circle]]
   [reagent-material-ui.icons.menu :refer [menu]]
   [reagent-material-ui.icons.category :refer [category]]
   [reagent-material-ui.icons.settings :refer [settings]]
   [reagent-material-ui.icons.assessment :refer [assessment]]
   [reagent-material-ui.icons.apartment :refer [apartment]]
   [reagent-material-ui.cljs-time-utils :refer [cljs-time-utils]]
   [reagent-material-ui.colors :as colors]
   [reagent-material-ui.core.css-baseline :refer [css-baseline]]
   [reagent-material-ui.core.grid :refer [grid]]
   [reagent-material-ui.core.app-bar :refer [app-bar]]
   [reagent-material-ui.core.divider :refer [divider]]
   [reagent-material-ui.core.hidden :refer [hidden]]
   [reagent-material-ui.core.drawer :refer [drawer]]
   [reagent-material-ui.core.list :refer [list]]
   [reagent-material-ui.core.list-item :refer [list-item]]
   [reagent-material-ui.core.list-item-icon :refer [list-item-icon]]
   [reagent-material-ui.core.list-item-text :refer [list-item-text]]
   [reagent-material-ui.core.typography :refer [typography]]
   [reagent-material-ui.core.icon-button :refer [icon-button]]
   [reagent-material-ui.core.toolbar :refer [toolbar]]
   [reagent-material-ui.pickers.mui-pickers-utils-provider :refer [mui-pickers-utils-provider]]
   [reagent-material-ui.styles :as styles])
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

(defn fab []
  (when-let [actions @(rf/subscribe [::subs/fab-actions])]
    [:div.fab {:on-click (-> actions :left-1 :fn)}
     [:i {:class (str "fa " (-> actions :left-1 :icon))}]]))

(defn progress-bar []
  [:div.progress
   [:div {:class (if @(rf/subscribe [::subs/show-progress]) "indeterminate" "")}]])

(defn splash []
  (let [splash @(rf/subscribe [::subs/splash])]
    [:div {:class (str "splash" (if splash " splash-show" " splash-hide"))}
     [:div.splash-content
      [:div.splash-header
       [:h3 "Buy2Let"]
       [:img {:src "images/icon/icon-64.png"}]]
      [:div.splash-loader]
      [:div.splash-footer
       [:h6 "PORTFOLIO MANAGEMENT"]]]]))

(defn sign-in-panel []
  [:div
   [dialog/dialog]
   (let [error @(rf/subscribe [::bs/error])
         heading (if (not (nil? error))
                   (if (s/includes? error "An account already exists with the same email address")
                     "Account exists"
                     "Oops!")
                   "Sign in")
         message (if (not (nil? error))
                   (if (s/includes? error "An account already exists with the same email address")
                     "Please choose the same network/email you used previously (Tip: you may link your account to more providers in Settings)"
                     error)
                   "with your email")]
     (rf/dispatch [::se/dialog {:heading   heading :message message
                                :panel     [:div.providers
                                            [:input {:type :email :value "demo@email.com" :on-change #()}]
                                            [:input {:type :password :value "***********" :on-change #()}]
                                            [:button {:on-click #(rf/dispatch [::be/sign-in :google])} "Sign in"]
                                            [:label "- or -"]
                                            [:button {:on-click #(rf/dispatch [::be/sign-in :google])} "Google"]
                                            [:button {:on-click #(rf/dispatch [::be/sign-in :facebook])} "Facebook"]]
                                :closeable false}]))])

(def custom-theme
  {:palette {:primary   colors/blue}})

(defn custom-styles [{:keys [spacing breakpoints mixins] :as theme}]
  (let [drawer-width 200
        sm-up (:up breakpoints)]
    {:root {:display :flex}
     :drawer {(sm-up "sm") {:width drawer-width, :flexShrink 0}}
     :app-bar {(sm-up "sm") {:width (str "calc(100% - " drawer-width "px)") :marginLeft drawer-width}}
     :title {:flexGrow 1}
     :menu-button {(sm-up "sm") {:display :none} :marginRight (spacing 2)}
     :button {:margin (spacing 1)}
     :drawerPaper {:width drawer-width}
     :content {:flexGrow 1 :padding (spacing 3) :padding-top (spacing 7)}}))

(def with-custom-styles (styles/with-styles custom-styles))

(defn handle-drawer-toggle []
  (let [mobile-open @(rf/subscribe [::subs/nav-menu-show])]
    (rf/dispatch [::se/show-nav-menu (not mobile-open)])))

(defn header [{:keys [classes]}]
  [app-bar {:position :fixed
            :class (:app-bar classes)}
   [toolbar {:variant :dense}
    [icon-button {:edge :start
                  :color :inherit
                  :class (:menu-button classes)
                  :on-click handle-drawer-toggle}
     [menu]]
    [typography {:variant :h6
                 :no-wrap true
                 :class (:title classes)} "Buy2Let"]
    [:div
     [icon-button {:color :inherit}
      [account-circle]]]]])

(defn nav [{:keys [classes]}]
  (let [drawer_ [:div
                 [divider]
                 [list
                  [list-item {:button true
                              :on-click #(js/window.location.assign "#/")}
                   [list-item-icon [dashboard]]
                   [list-item-text {:primary "Dashboard"}]]
                  [list-item {:button true
                              :on-click #(js/window.location.assign (build-reconcile-url))}
                   [list-item-icon [receipt]]
                   [list-item-text {:primary "Reconcile"}]]
                  [list-item {:button true
                              :on-click #(js/window.location.assign (build-report-url))}
                   [list-item-icon [assessment]]
                   [list-item-text {:primary "Report"}]]
                  [list-item {:button true
                              :on-click #(js/window.location.assign "#/properties")}
                   [list-item-icon [apartment]]
                   [list-item-text {:primary "Properties"}]]
                  [list-item {:button true
                              :on-click #(js/window.location.assign "#/charges")}
                   [list-item-icon [category]]
                   [list-item-text {:primary "Charges"}]]
                  [list-item {:button true
                              :on-click #(js/window.location.assign "#/settings")}
                   [list-item-icon [settings]]
                   [list-item-text {:primary "Settings"}]]]]]
    [:nav {:class (:drawer classes)}
     [hidden {:sm-up true
                     :implementation :css}
      [drawer {:container (.. js/window -document -body)
                      :variant :temporary
                      :anchor :left
                      :open (or @(rf/subscribe [::subs/nav-menu-show]) false)
                      :on-close handle-drawer-toggle
                      :classes {:paper (:drawer-paper classes)}
                      :Modal-props {:keep-mounted true}}
       drawer_]]
     [hidden {:xs-down true
              :implementation :css}
      [drawer {:classes {:paper (:drawer-paper classes)}
               :variant :permanent
               :open true}
       drawer_]]]))

(defn main [{:keys [classes] :as props}]
  [:main {:class (:content classes)}
   [grid {:item true}
    (when-let [active-page @(rf/subscribe [::subs/active-page])]
      (condp = active-page
        :dashboard [dashboard/dashboard]
        :reconcile [reconcile/reconcile]
        :report [report/report]
        :properties [crud-impl/properties]
        :charges [crud-impl/charges]
        :delegates [crud-impl/delegates props]
        :settings [settings/settings]))]])

(defn container [{:keys [classes] :as props}]
  [:div {:class (:root classes)}
   [header props]
   [nav props]
   [main props]])

(defn main-panel []
  [:<>
   [css-baseline]
  ;;  [splash]
  ;;  [dialog/dialog]
  ;;  [progress-bar]
  ;; [fab]
   [mui-pickers-utils-provider {:utils  cljs-time-utils
                                :locale DateTimeSymbols_en_US}
    [styles/theme-provider (styles/create-mui-theme custom-theme)
     [grid
      {:container true
       :direction :row
       :justify   :flex-start}
      [grid
       {:item true
        :xs   12}
       [(with-custom-styles container)]]]]]])




