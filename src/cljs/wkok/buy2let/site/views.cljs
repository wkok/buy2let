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
   [wkok.buy2let.crud.impl :as crud-impl]
   [wkok.buy2let.backend.events :as be]
   [reagent-material-ui.icons.dashboard :refer [dashboard]]
   [reagent-material-ui.icons.receipt :refer [receipt]]
   [reagent-material-ui.icons.menu :refer [menu]]
   [reagent-material-ui.icons.category :refer [category]]
   [reagent-material-ui.icons.settings :refer [settings]]
   [reagent-material-ui.icons.assessment :refer [assessment]]
   [reagent-material-ui.icons.apartment :refer [apartment]]
   [reagent-material-ui.cljs-time-utils :refer [cljs-time-utils]]
   [reagent-material-ui.colors :as colors]
   [reagent-material-ui.core.css-baseline :refer [css-baseline]]
   [reagent-material-ui.core.grid :refer [grid]]
   [reagent-material-ui.core.fab :refer [fab]]
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
    [fab {:color :primary
          :class (get-in props [:classes :fab])
          :on-click (-> actions :left-1 :fn)}
     (-> actions :left-1 :icon)]))

(defn splash [props]
  [backdrop {:open @(rf/subscribe [::subs/splash])
             :class (get-in props [:classes :splash])}
   [circular-progress]])

(def custom-theme
  {:palette {:primary   colors/blue}})

(defn custom-styles [{:keys [spacing breakpoints z-index]}]
  (let [up (:up breakpoints)
        drawer-width 200]
    {:root {:display :flex}
     :drawer {(up "sm") {:width drawer-width, :flex-shrink 0}}
     :app-bar {(up "sm") {:width (str "calc(100% - " drawer-width "px)") :margin-left drawer-width}}
     :title {:flex-grow 1}
     :menu-button {(up "sm") {:display :none} 
                   :margin-right (spacing 2)}
     :drawer-paper {:width drawer-width}
     :content {:flex-grow 1 
               :padding (spacing 2) 
               :padding-top (spacing 8)
               :padding-bottom (spacing 8)
               :overflow-x :hidden}
     :buttons {:padding-top (spacing 1)}
     :fab {:position :fixed
           :bottom (spacing 2)
           :right (spacing 2)}
     :splash {:z-index (+ (:drawer z-index) 1)}
     :who-pays-whom {:padding-left (spacing 4)}
     :paper {:padding (spacing 2)}
     :table-header {:font-weight 600}}))

(def with-custom-styles (styles/with-styles custom-styles))

(defn handle-drawer-toggle []
  (let [mobile-open @(rf/subscribe [::subs/nav-menu-show])]
    (rf/dispatch [::se/show-nav-menu (not mobile-open)])))

(defn progress-bar []
  (when @(rf/subscribe [::subs/show-progress])
    [linear-progress]))

(defn header [{:keys [classes]}]
  [app-bar {:position :fixed
            :class (:app-bar classes)}
   [progress-bar]
   [toolbar {:variant :dense}
    [icon-button {:edge :start
                  :color :inherit
                  :class (:menu-button classes)
                  :on-click handle-drawer-toggle}
     [menu]]
    [typography {:variant :h5
                 :no-wrap true
                 :class (:title classes)} "Buy2Let"]
    ;; [:div
    ;;  [icon-button {:color :inherit}
    ;;   [account-circle]]]
    ]])

(defn navigate [hash]
  (js/window.location.assign hash)
  (rf/dispatch [::se/show-nav-menu false]))

(defn nav [{:keys [classes]}]
  (let [drawer_ [:div
                 [divider]
                 [list
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
                  [list-item {:button true
                              :on-click #(navigate "#/settings")}
                   [list-item-icon [settings]]
                   [list-item-text {:primary "Settings"}]]]]]
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
       :dashboard [dashboard/dashboard props]
       :reconcile [reconcile/reconcile props]
       :report [report/report props]
       :properties [crud-impl/properties props]
       :charges [crud-impl/charges props]
       :delegates [crud-impl/delegates props]
       :settings [settings/settings props]))])

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

(defn main-panel []
  [:div
   [css-baseline]
   [mui-pickers-utils-provider {:utils  cljs-time-utils
                                :locale DateTimeSymbols_en_US}
    [styles/theme-provider (styles/create-mui-theme custom-theme)
     [grid {:container true
            :direction :row
            :justify   :flex-start}
      [grid {:item true
             :xs   12}
       [(with-custom-styles
          (fn [{:keys [classes] :as props}]
            [:div {:class (:root classes)}
             [splash props]
             [fab-button props]
             [header props]
             [nav props]
             [main props]
             [dialog/create-dialog]]))]]]]]])




