(ns wkok.buy2let.site.dialog
  (:require [re-frame.core :as rf]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.site.subs :as subs]))


(defn dialog-button [btn]
  [:button {:class (:class btn)
            :on-click #(do (when-let [f (:on-click btn)] (f))
                           (rf/dispatch [::se/dialog]))} (:text btn)])

(defn dialog []
  (let [dialog @(rf/subscribe [::subs/dialog])]
    [:div {:class (str "dialog" (if dialog " dialog-show" " dialog-hide"))}
     [:div.dialog-content
      [:div.dialog-container
       (when (get dialog :closeable true)
         [:span.dialog-close.fa.fa-times {:aria-hidden true :on-click #(rf/dispatch [::se/dialog])}])
       (when-let [heading (:heading dialog)]
         [:h2 heading])
       (when-let [message (:message dialog)]
         [:p message])
       (when-let [panel (:panel dialog)]
         panel)
       (when-let [buttons (:buttons dialog)]
         [:div.dialog-toolbar
          (when-let [btn (:left buttons)]
            [:div.dialog-left-btn [dialog-button btn]])
          (when-let [btn (:middle buttons)]
            [:div.dialog-middle-btn [dialog-button btn]])
          (when-let [btn (:right buttons)]
            [:div.dialog-right-btn [dialog-button btn]])])]]]))