(ns wkok.buy2let.ui.native.app
  (:require
   [re-frame.core :as rf]
   [wkok.buy2let.backend.multimethods :as mm]
   [wkok.buy2let.site.routes :as routes]
   [wkok.buy2let.ui.native.view.site :as views]
   [wkok.buy2let.ui.native.expo :as expo]
   [reagent.core :as r]))

(defn ^:dev/after-load start []
  (js/console.log "start")
  (mm/init-auth {:render-main-panel #(expo/render-root (r/as-element [views/main-panel]))
                 :render-sign-in-panel #(expo/render-root (r/as-element [views/sign-in-panel]))}))

(defn init []
  (js/console.log "init")
  (rf/dispatch-sync [:initialize-db])
  (mm/init {})
  (routes/app-routes)
  ;; (charts/init)
  (start))
