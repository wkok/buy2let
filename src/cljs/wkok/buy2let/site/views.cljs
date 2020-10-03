(ns wkok.buy2let.site.views
  (:require
   [reagent.core :as r]
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
   [wkok.buy2let.backend.subs :as bs]
   [clojure.string :as s]
   ["@material-ui/core" :as mui]
   ["@material-ui/core/styles" :refer [createMuiTheme withStyles]]
   ["@material-ui/core/colors" :as mui-colors]
   ["@material-ui/icons" :as icons]
   [goog.object :as gobj]))

(defn nav-menu-item [href page active-page]
  [:li [:a {:href     href
            :class    (when (= page active-page) "active")
            :on-click #(rf/dispatch [::se/show-nav-menu false])} (-> page name s/capitalize)]])

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

(defn nav-bar []
  (let [active-page @(rf/subscribe [::subs/active-page])]
    [:div.nav-bar
     [:a.menu-toggle {:href     "#" :aria-label "Open main menu"
                      :on-click #(do (.preventDefault %) 
                                     (rf/dispatch [::se/show-nav-menu true]))}
      [:span.sr-only "Open main menu"]
      [:span.fa.fa-bars {:aria-hidden true}]]
     [:nav.main-menu {:aria-label    "Main menu"
                      :aria-expanded @(rf/subscribe [::subs/nav-menu-show])}
      [:a.menu-close {:href     "#" :aria-label "Close main menu"
                      :on-click #(do (.preventDefault %) (rf/dispatch [::se/show-nav-menu false]))}
       [:span.sr-only "Close main menu"]
       [:span.fa.fa-times {:aria-hidden true}]]
      [:ul
       [nav-menu-item "#/" :dashboard active-page]
       [nav-menu-item (build-reconcile-url) :reconcile active-page]
       [nav-menu-item (build-report-url) :report active-page]
       [nav-menu-item "#/properties" :properties active-page]
       [nav-menu-item "#/charges" :charges active-page]
       [nav-menu-item "#/settings" :settings active-page]]]
     [:a.backdrop {:href     "#" :tab-index "-1" :aria-hidden true :hidden true
                   :on-click #(do (.preventDefault %) (rf/dispatch [::se/show-nav-menu false]))}]
     [:label.active @(rf/subscribe [::subs/heading])]]))


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
  (createMuiTheme
   #js {:palette #js {:primary #js {:main (gobj/get (.-blue ^js/Mui.Colors mui-colors) 500)}}}))

(defn custom-styles [^js/Mui.Theme theme]
  (let [drawerWidth 240
        sm (.up (.-breakpoints theme) "sm")]
    (clj->js
     {:root {:display :flex}
      :drawer {sm {:width drawerWidth, :flexShrink 0}}
      :appBar {sm {:width (str "calc(100% - " drawerWidth "px)") :marginLeft drawerWidth}}
      :title {:flexGrow 1}
      :menuButton {sm {:display :none}
                   :marginRight (.spacing theme 2)}
      :toolbar (.. theme -mixins -toolbar) ; necessary for content to be below app bar
      :drawerPaper {:width drawerWidth}
      :content {:flexGrow 1, :padding (.spacing theme 3)}})))

(def with-custom-styles (withStyles custom-styles))

(defn make [component]
  (-> component
      r/reactify-component
      with-custom-styles))

(defn handle-drawer-toggle []
  (let [mobile-open @(rf/subscribe [::subs/nav-menu-show])]
    (rf/dispatch [::se/show-nav-menu (not mobile-open)])))

(def header
  (make
   (fn [{:keys [classes]}]
     [:> mui/AppBar {:position :fixed
                     :class (.-appBar classes)}
      [:> mui/Toolbar {:variant :dense}
       [:> mui/IconButton {:edge :start
                           :color :inherit
                           :class (.-menuButton classes)
                           :on-click handle-drawer-toggle}
        [:> icons/Menu]]
       [:> mui/Typography {:variant :h6
                           :no-wrap true
                           :class (.-title classes)} "Buy2Let"]
       [:div
        [:> mui/IconButton {:color :inherit}
         [:> icons/AccountCircle]]]]])))

(def nav
  (make
   (fn [{:keys [classes]}]
     (let [drawer [:div
                   [:div {:class (.-toolbar classes)}]
                   [:> mui/Divider]
                   [:> mui/List
                    [:> mui/ListItem {:button true
                                      :on-click #(js/window.location.assign "#/")}
                     [:> mui/ListItemIcon
                      [:> icons/MoveToInbox]]
                     [:> mui/ListItemText {:primary "Dashboard"}]]
                    [:> mui/ListItem {:button true
                                      :on-click #(js/window.location.assign (build-reconcile-url))}
                     [:> mui/ListItemIcon
                      [:> icons/MoveToInbox]]
                     [:> mui/ListItemText {:primary "Reconcile"}]]
                    [:> mui/ListItem {:button true
                                      :on-click #(js/window.location.assign (build-report-url))}
                     [:> mui/ListItemIcon
                      [:> icons/MoveToInbox]]
                     [:> mui/ListItemText {:primary "Report"}]]
                    [:> mui/ListItem {:button true
                                      :on-click #(js/window.location.assign "#/properties")}
                     [:> mui/ListItemIcon
                      [:> icons/MoveToInbox]]
                     [:> mui/ListItemText {:primary "Properties"}]]
                    [:> mui/ListItem {:button true
                                      :on-click #(js/window.location.assign "#/charges")}
                     [:> mui/ListItemIcon
                      [:> icons/MoveToInbox]]
                     [:> mui/ListItemText {:primary "Charges"}]]
                    [:> mui/ListItem {:button true
                                      :on-click #(js/window.location.assign "#/settings")}
                     [:> mui/ListItemIcon
                      [:> icons/MoveToInbox]]
                     [:> mui/ListItemText {:primary "Settings"}]]]]]
       [:nav {:class (.-drawer classes)}
        [:> mui/Hidden {:smUp true
                        :implementation :css}
         [:> mui/Drawer {:container (.. js/window -document -body)
                         :variant :temporary
                         :anchor :left
                         :open (or @(rf/subscribe [::subs/nav-menu-show]) false)
                         :on-close handle-drawer-toggle
                         :classes (clj->js {:paper (.-drawerPaper classes)})
                         :ModalProps #js {:keepMounted true}}
          drawer]]
        [:> mui/Hidden {:xsDown true
                        :implementation :css}
         [:> mui/Drawer {:classes (clj->js {:paper (.-drawerPaper classes)})
                         :variant :permanent
                         :open true}
          drawer]]]))))

(def main
  (make
   (fn [{:keys [classes]}]
     [:main {:class (.-content classes)}
      [:div {:class (.-toolbar classes)}]
      [:> mui/Grid {:item true}
       (when-let [active-page @(rf/subscribe [::subs/active-page])]
         (condp = active-page
           :dashboard [dashboard/dashboard]
           :reconcile [reconcile/reconcile]
           :report [report/report]
           :properties [crud-impl/properties]
           :charges [crud-impl/charges]
           :delegates [crud-impl/delegates]
           :settings [settings/settings]))]])))

(def container
  (make
   (fn [{:keys [classes]}]
     [:div {:class (.-root classes)}
      [:> header]
      [:> nav]
      [:> main]])))

(defn main-panel []
  [:div
   [:> mui/CssBaseline]
  ;;  [splash]
  ;;  [dialog/dialog]
  ;;  [nav-bar]
  ;;  [progress-bar]
  ;; [fab]
   [:> mui/MuiThemeProvider {:theme custom-theme}
    [:> container]]])




