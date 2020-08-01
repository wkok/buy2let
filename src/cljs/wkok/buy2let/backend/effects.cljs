(ns wkok.buy2let.backend.effects
  (:require [re-frame.core :as rf]
            [wkok.buy2let.backend.protocol :as protocol]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.backend.impl :as impl]))

(rf/reg-fx
 :unlink-provider
 (fn [provider _]
   (protocol/unlink-provider impl/backend 
                             {:provider provider
                              :on-success #(rf/dispatch [:unlink-succeeded %])
                              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message (str %)}])})))


(rf/reg-fx
 :link-provider
 (fn [provider _]
   (protocol/link-provider impl/backend provider)))
