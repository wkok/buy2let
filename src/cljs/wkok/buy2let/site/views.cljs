(ns wkok.buy2let.site.views
  (:require
    [re-frame.core :as rf]
    [wkok.buy2let.site.subs :as subs]
    [wkok.buy2let.site.events :as se]
    [wkok.buy2let.reconcile.views :as reconcile]
    [wkok.buy2let.report.views :as report]
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

(defn nav-bar []
  (let [active-page @(rf/subscribe [::subs/active-page])]
    [:div.nav-bar
     [:a.menu-toggle {:href     "javascript:void(0);" :aria-label "Open main menu"
                      :on-click #(rf/dispatch [::se/show-nav-menu true])}
      [:span.sr-only "Open main menu"]
      [:span.fa.fa-bars {:aria-hidden true}]]
     [:nav.main-menu {:aria-label    "Main menu"
                      :aria-expanded @(rf/subscribe [::subs/nav-menu-show])}
      [:a.menu-close {:href     "javascript:void(0);" :aria-label "Close main menu"
                      :on-click #(rf/dispatch [::se/show-nav-menu false])}
       [:span.sr-only "Close main menu"]
       [:span.fa.fa-times {:aria-hidden true}]]
      [:ul
       [nav-menu-item "#/" :dashboard active-page]
       [nav-menu-item "#/reconcile" :reconcile active-page]
       [nav-menu-item "#/report" :report active-page]
       [nav-menu-item "#/properties" :properties active-page]
       [nav-menu-item "#/charges" :charges active-page]
       [nav-menu-item "#/settings" :settings active-page]]]
     [:a.backdrop {:href     "javascript:void(0);" :tab-index "-1" :aria-hidden true :hidden true
                   :on-click #(rf/dispatch [::se/show-nav-menu false])}]
     [:label.active @(rf/subscribe [::subs/heading])]]))


(defn fab []
  (when-let [actions @(rf/subscribe [::subs/fab-actions])]
    [:div.fab {:on-click (-> actions :left-1 :fn)}
     [:i {:class (str "fa " (-> actions :left-1 :icon))}]]))

(defn progress-bar []
  [:div.progress
   [:div {:class (if @(rf/subscribe [::subs/show-progress]) "indeterminate" "")}]])

(defn dialog-button [btn]
  [:button {:class (:class btn)
            :on-click #(do (when-let [f (:on-click btn)] (f))
                           (rf/dispatch [::se/dialog]))} (:text btn)])

(defn dialog []
  (let [dialog @(rf/subscribe [::subs/dialog])]
    [:div {:class (str "dialog" (if dialog " dialog-show" " dialog-hide"))}
     [:div.dialog-content
      [:div.dialog-container
       (when (get dialog :closeable true)
         [:span.dialog-close.fa.fa-times {:aria-hidden true :on-click #(rf/dispatch [::se/dialog])}])
       (when-let [heading (:heading dialog)]
         [:h2 heading])
       (when-let [message (:message dialog)]
         [:p message])
       (when-let [panel (:panel dialog)]
         panel)
       (when-let [buttons (:buttons dialog)]
         [:div.dialog-toolbar
          (when-let [btn (:left buttons)]
            [:div.dialog-left-btn [dialog-button btn]])
          (when-let [btn (:middle buttons)]
            [:div.dialog-middle-btn [dialog-button btn]])
          (when-let [btn (:right buttons)]
            [:div.dialog-right-btn [dialog-button btn]])])]]]))

(defn sign-in-panel []
  [:div
   [dialog]
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
   [dialog]
   [nav-bar]
   [progress-bar]
   [:main.scrollable-y
    [:div
     (when-let [active-page @(rf/subscribe [::subs/active-page])]
       (condp = active-page
         :dashboard [dashboard/dashboard]
         :reconcile [reconcile/reconcile]
         :report [report/report]
         :properties [crud-impl/properties]
         :charges [crud-impl/charges]
         :users [crud-impl/users]
         :settings [settings/settings]))]]
   [fab]])



