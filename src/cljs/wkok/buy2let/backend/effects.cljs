(ns wkok.buy2let.backend.effects
  (:require [re-frame.core :as rf]
            [wkok.buy2let.backend.multimethods :as mm]
            [wkok.buy2let.site.events :as se]))

(rf/reg-fx
 :unlink-provider
 (fn [provider _]
   (mm/unlink-provider {:provider provider
                        :on-success #(rf/dispatch [:unlink-succeeded %])
                        :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message (str %)}])})))

(rf/reg-fx
 :link-provider
 (fn [provider _]
   (mm/link-provider {:provider provider})))
