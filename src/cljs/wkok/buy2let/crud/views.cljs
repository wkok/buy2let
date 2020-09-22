(ns wkok.buy2let.crud.views
  (:require [re-frame.core :as rf]
            [clojure.string :as s]
            [wkok.buy2let.crud.events :as ce]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.shared :as shared]
            [fork.re-frame :as fork]
            [clojure.walk :as w]))


(defn row [item type]
  [:tr {:on-click #(js/window.location.assign (str "#/" (-> type :type name) "/edit/" (-> item :id name)))}
   (for [field (filter #(:default %) (:fields type))]
     ^{:key field}
     [:td ((:key field) item)])])

(defn show? [item show-hidden]
  (if (get item :hidden false)
    show-hidden
    true))

(defn list-panel [type]
  (rf/dispatch [:set-fab-actions (get-in type [:actions :list])])
  (let [show-hidden @(rf/subscribe [::cs/show-hidden])]
    [:div
     [:br]
     [:table
      [:tbody
       (for [item (filter #(and (not (:reserved %)) (show? % show-hidden))
                          @(rf/subscribe [(:subs type)]))]
         ^{:key item}
         [row item type])]]
     [:div.crud-show-hidden
      (if show-hidden
        (shared/anchor #(rf/dispatch [::ce/crud-set-show-hidden false])
                       (str "Hide " (get type :hidden-label "hidden")))
        (shared/anchor #(rf/dispatch [::ce/crud-set-show-hidden true])
                       (str "Show " (get type :hidden-label "hidden"))))]]))


(defn build-checkbox
  [field {:keys [values handle-change handle-blur]}]
  (let [field-name (name (:key field))]
    ^{:key field-name}
    [:div
     [:label [:input {:name      field-name
                      :type      :checkbox
                      :checked   (values "send-invite" true)
                      :on-change handle-change
                      :on-blur   handle-blur}]
      (get field :label (s/capitalize field-name))]]))

(defn build-input
  [type field {:keys [values errors touched handle-change handle-blur]}]
  (let [field-name (name (:key field))]
    ^{:key field-name}
    [:div
     [:label (get field :label (-> field-name s/capitalize (str ": ")))]
     [:input {:name       field-name
              :type       (:type field)
              :auto-focus (and (:default field)
                               (nil? ((:type type) @(rf/subscribe [:form-old]))))
              :value      (values field-name "")
              :on-change  handle-change
              :on-blur    handle-blur}]
     (when (touched field-name)
       [:div.validation-error (get errors field-name)])]))

(defn build-select
  [field {:keys [values handle-change handle-blur]}]
  (let [field-name (name (:key field))]
    ^{:key field-name}
    [:div
     [:label (get field :label (-> field-name s/capitalize (str ": ")))]
     [:select {:name      field-name
               :value     (values field-name (:default-values field))
               :on-change handle-change
               :on-blur   handle-blur}
      (for [option (:options field)]
        ^{:key (key option)}
        [:option {:value (key option)} (-> option val)])]]))

(defn build-multi-select
  [field {:keys [values set-handle-change handle-blur]}]
  (let [field-name (name (:key field))]
    ^{:key field-name}
    [:div
     [:label (get field :label (-> field-name s/capitalize (str ": ")))]
     [:select
      {:name field-name
       :multiple true
       :value (values field-name [:viewer])
       :on-change #(set-handle-change
                    {:value (let [opts (array-seq (-> % .-target .-options))]
                              (keep (fn [x]
                                      (when (.-selected x)
                                        (.-value x))) opts))
                     :path [field-name]})
       :on-blur handle-blur}
      (for [option (:options field)]
        ^{:key (key option)}
        [:option {:value (key option)} (-> option val)])]]))

(defn build-hidden
  [type {:keys [values handle-change handle-blur]}]
  [:label [:input {:name      "hidden"
                   :type      :checkbox
                   :checked   (values "hidden" false)
                   :on-change handle-change
                   :on-blur   handle-blur}]
   (->> (get type :hidden-label "Hidden")
        s/capitalize
        (str " "))])

(defn edit-panel [type]
  (rf/dispatch [:set-fab-actions nil])
  [fork/form {:form-id            "id"
              :path               :form
              :prevent-default?   true
              :clean-on-unmount?  true
              :validation         (:validate-fn type)
              :on-submit-response {400 "client error"
                                   500 "server error"}
              :on-submit          #(rf/dispatch [::ce/save-crud type (w/keywordize-keys (:values %))])
              :initial-values     (if-let [old ((:type type) @(rf/subscribe [:form-old]))]
                                    (w/stringify-keys old)
                                    (w/stringify-keys (:defaults type)))}
   (fn [{:keys [values state errors touched form-id handle-change handle-blur submitting? handle-submit] :as options}]
     [:form.crud-edit-container {:id form-id :on-submit handle-submit}
      [:div.crud-edit-fields
       (doall
        (for [field (:fields type)]
          (case (:type field)
            :checkbox (build-checkbox field options)
            :select (build-select field options)
            :select-multi (build-multi-select field options)
            (build-input type field options))))
       (if-let [extra-fn (:extra type)]
         (extra-fn values state errors touched handle-change handle-blur))
       (build-hidden type options)]
      [:div.crud-edit-buttons-save-cancel.buttons-save-cancel
       [:button {:type :submit :disabled submitting?} "Save"]
       [:button {:type :button :on-click #(js/window.history.back)} "Cancel"]]])]
)
