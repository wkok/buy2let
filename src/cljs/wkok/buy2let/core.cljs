(ns wkok.buy2let.core
  (:require
   [reagent.dom :as rd]
   [re-frame.core :as rf]
   [wkok.buy2let.site.views :as views]
   [wkok.buy2let.config :as config]
   [wkok.buy2let.backend.protocol :as bp]
   [wkok.buy2let.backend.impl :as impl]
   [wkok.buy2let.site.routes :as routes]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))


(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (routes/app-routes)
  (bp/init-auth impl/backend
                #(rd/render [views/main-panel] (.getElementById js/document "app"))
                views/sign-in-panel))



(defn init []
  (dev-setup)
  (rf/dispatch-sync [:initialize-db])
  (bp/init impl/backend)
  (mount-root))
