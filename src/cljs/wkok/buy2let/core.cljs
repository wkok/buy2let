(ns wkok.buy2let.core
  (:require
   [reagent.dom :as rd]
   [re-frame.core :as rf]
   [wkok.buy2let.site.views :as views]
   [wkok.buy2let.config :as config]
   [wkok.buy2let.backend.multimethods :as mm]
   [wkok.buy2let.site.routes :as routes]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))


(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (routes/app-routes)
  (mm/init-auth {:render-main-panel #(rd/render [views/main-panel] (.getElementById js/document "app"))
                 :sign-in-panel views/sign-in-panel}))



(defn init []
  (dev-setup)
  (rf/dispatch-sync [:initialize-db])
  (mm/init {})
  (mount-root))
