(ns wkok.buy2let.site.effects
  (:require [re-frame.core :as rf]))

(rf/reg-fx
  ::location-hash
  (fn [hash _]
    (if (= hash "#/")
      (rf/dispatch [:set-active-page :dashboard "Dashboard"])
      (set! (.-hash js/location) hash))))                   ;used when deep linking

