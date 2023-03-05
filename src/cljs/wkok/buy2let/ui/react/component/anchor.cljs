(ns wkok.buy2let.ui.react.component.anchor
  (:require
   [reagent-mui.material.link :refer [link]]))

; See: https://github.com/facebook/react/issues/16382
(defn anchor
  [on-click label]
  [link {:href "#" :on-click #(do (.preventDefault %)
                                  (on-click))}
   label])
