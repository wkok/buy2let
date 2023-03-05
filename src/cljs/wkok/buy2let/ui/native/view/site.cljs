(ns wkok.buy2let.ui.native.view.site
  (:require [wkok.buy2let.ui.native.components :as components]
            [wkok.buy2let.backend.events :as be]
            [re-frame.core :as rf]))

(defn main-panel []
  [:> components/MainPanel
   {:onPress #(rf/dispatch [:sign-out])}])

(defn sign-in-panel []
  [:> components/SignInPanel
   {:onPress #(rf/dispatch [::be/sign-in :google])}])
