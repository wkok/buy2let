(ns wkok.buy2let.core
  (:require
    [reagent.core :as ra]
    [re-frame.core :as rf]
    [wkok.buy2let.db.events :as dbe]
    [wkok.buy2let.site.views :as views]
    [wkok.buy2let.config :as config]
    [wkok.buy2let.backend.protocol :as bp]
    [wkok.buy2let.backend.impl :as impl]
    [wkok.buy2let.site.routes :as routes]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))


(defn render [panel]
  (ra/render panel (.getElementById js/document "app")))


(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (routes/app-routes)
  (bp/init-auth impl/backend
                #(if (nil? %)
                   (render [views/sign-in-panel])
                   (render [views/main-panel]))))


(defn init []
  (dev-setup)
  (rf/dispatch-sync [::dbe/initialize-db])
  (bp/init impl/backend)
  (mount-root))
