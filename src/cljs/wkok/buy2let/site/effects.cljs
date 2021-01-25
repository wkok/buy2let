(ns wkok.buy2let.site.effects
  (:require [re-frame.core :as rf]))

(rf/reg-fx
  ::location-hash
  (fn [hash _]
    (if (= hash "#/")
      (rf/dispatch [:set-active-page :dashboard "Dashboard"])
      (set! (.-hash js/location) hash))))                   ;used when deep linking

(rf/reg-fx
 ::remove-query-params
 (fn [_ _]
   (when (.-pushState js/history)
     (let [url (str
                (.. js/window -location -origin)
                (.. js/window -location -pathname))]
       (.pushState (.-history js/window) #js {:path url} "" url)))))
