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
   [wkok.buy2let.backend.subs :as bs]
   [clojure.string :as s]))

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
                   "Choose your favourite network")]
     (rf/dispatch [::se/dialog {:heading   heading :message message
                                :panel     [:div.providers
                                            [:button {:on-click #(rf/dispatch [::be/sign-in :google])} "Google"]
                                            [:button {:on-click #(rf/dispatch [::be/sign-in :facebook])} "Facebook"]
                                            [:button {:on-click #(rf/dispatch [::be/sign-in :twitter])} "Twitter"]
                                            [:button {:on-click #(rf/dispatch [::be/sign-in :github])} "Github"]]
                                :closeable false}]))])

(defn main-panel []
  [:div
   [dialog/dialog]
   [nav-bar]
   [progress-bar]
   [:main
    [:div
     (when-let [active-page @(rf/subscribe [::subs/active-page])]
       (condp = active-page
         :dashboard [dashboard/dashboard]
         :reconcile [reconcile/reconcile]
         :report [report/report]
         :properties [crud-impl/properties]
         :charges [crud-impl/charges]
         :delegates [crud-impl/delegates]
         :settings [settings/settings]))]]
   [fab]])




