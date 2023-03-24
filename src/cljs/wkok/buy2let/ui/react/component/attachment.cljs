(ns wkok.buy2let.ui.react.component.attachment
  (:require [re-frame.core :as rf]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.shared :as shared]
            [reagent-mui.icons.cloud-upload-outlined :refer [cloud-upload-outlined]]
            [reagent-mui.icons.cloud-done :refer [cloud-done]]
            [reagent-mui.icons.cloud-done-outlined :refer [cloud-done-outlined]]
            [reagent-mui.icons.delete-outlined :refer [delete-outlined]]
            [reagent-mui.icons.visibility-outlined :refer [visibility-outlined]]
            [reagent-mui.material.tooltip :refer [tooltip]]
            [reagent-mui.material.typography :refer [typography]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.icon-button :refer [icon-button]]
            [clojure.walk :as w]))

(defn swap-attachment-deleted [state]
  (swap! state update-in [:values] dissoc :attachment)
  (swap! state assoc-in [:values :attached] false)
  (swap! state assoc-in [:values :attachment-deleted] true))

(defn edit-attachment-field-upload [field state handle-blur icon]
  [:div
   [:input {:id        (:key field)
            :name      "attachment"
            :type      :file
            :accept    "image/*,.pdf"
            :style     {:display :none}
            :on-change #(let [file (-> % .-target .-files (aget 0))]
                          (when (shared/validate-file-size file 2000000)
                            (swap! state assoc-in [:values :attachment] file)
                            (swap! state update-in [:values] dissoc :attachment-deleted)))
            :on-blur   handle-blur}]
   [:label {:html-for (:key field)}
    [tooltip {:title "Upload attachment"}
     [icon-button {:variant :contained
                   :component :span
                   :color :primary}
      icon]]]])

(defn edit-attachment-field-view [invoice-id]
  [tooltip {:title "View attachment"}
   [icon-button {:color :primary
                 :on-click #(rf/dispatch [::shared/view-attachment :invoices invoice-id])}
    [visibility-outlined]]])

(defn edit-attachment-field-delete [state]
  [tooltip {:title "Delete attachment"}
   [icon-button {:color :secondary
                 :on-click   #(when (= true (get-in @state [:values "attached"]))
                                (rf/dispatch [::se/dialog {:heading "Delete attachment?"
                                                           :message "Attachment will be deleted after this form is saved"
                                                           :buttons {:left  {:text     "Delete"
                                                                             :color :secondary
                                                                             :on-click (fn [] (swap-attachment-deleted state))}
                                                                     :right {:text "Cancel"}}}]))}
    [delete-outlined]]])

(defn build-attachment
  [field {:keys [values state handle-blur]}]
  (let [values-kw (w/keywordize-keys values)
        attached (:attachment values-kw)
        uploaded (:attached values-kw)]
    ^{:key (:key field)}
    [grid {:item true}
     [typography {:variant :caption} "Attached Invoice"]
     (if uploaded
       [grid {:container true
              :direction :row}
        [grid {:item true}
         [edit-attachment-field-view (:id values-kw)]]
        [grid {:item true}
         [edit-attachment-field-upload field state handle-blur [cloud-done]]]
        [grid {:item true}
         [edit-attachment-field-delete state]]]
       (if attached
         [edit-attachment-field-upload field state handle-blur [cloud-done-outlined]]
         [edit-attachment-field-upload field state handle-blur [cloud-upload-outlined]]))]))
