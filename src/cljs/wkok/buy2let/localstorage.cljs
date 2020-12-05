(ns wkok.buy2let.localstorage
  (:require [re-frame.core :as rf]))

(defn set-item!
  "Set `key' in browser's localStorage to `val`."
  [key val]
  (.setItem (.-localStorage js/window) key val))

(defn get-item
  "Returns value of `key' from browser's localStorage."
  [key]
  (.getItem (.-localStorage js/window) key))

(defn remove-item!
  "Remove the browser's localStorage value for the given `key`"
  [key]
  (.removeItem (.-localStorage js/window) key))

(rf/reg-fx
 ::remove-item
 (fn [{:keys [key on-success on-error]}]
   (try
     (remove-item! key)
     (on-success key)
     (catch :default e
       (on-error e)))))

(rf/reg-fx
 ::set-item
 (fn [{:keys [key val on-success on-error]}]
   (try
     (set-item! key val)
     (on-success key)
     (catch :default e
       (on-error e)))))

(rf/reg-fx
 ::get-item
 (fn [{:keys [key on-success on-error]}]
   (try
     (-> (get-item key)
         on-success)
     (catch :default e
       (on-error e)))))

