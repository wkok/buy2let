(ns wkok.buy2let.core
  (:require
   [goog.events :as events]
   [re-frame.core :as rf]
   [reagent.dom :as rd]
   [secretary.core :as sec]
   [wkok.buy2let.backend.multimethods :as mm]
   [wkok.buy2let.charts :as charts]
   [wkok.buy2let.site.routes :as routes]
   [wkok.buy2let.site.views :as views])
  (:import
   [goog History]
   [goog.history EventType]))

(defn ^:dev/after-load start []
  (js/console.log "start")
  (mm/init-auth {:render-main-panel #(rd/render [views/main-panel] (.getElementById js/document "app"))
                 :sign-in-panel views/sign-in-panel}))

(defn- init-history []
  (doto (History.)
    (events/listen EventType.NAVIGATE #(sec/dispatch! (.-token ^js/goog.history.Event %)))
    (.setEnabled true)))

(defn init []
  (js/console.log "init")
  (rf/dispatch-sync [:initialize-db])
  (mm/init {})
  (routes/app-routes)
  (init-history)
  (charts/init)
  (start))
