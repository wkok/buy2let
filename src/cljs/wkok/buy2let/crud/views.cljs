(ns wkok.buy2let.crud.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [clojure.string :as s]
            [wkok.buy2let.crud.events :as ce]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.shared :as shared]
            [fork.re-frame :as fork]
            [clojure.walk :as w]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.list-subheader :refer [list-subheader]]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.form-control-label :refer [form-control-label]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.select :refer [select]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]
            [reagent-material-ui.core.form-control :refer [form-control]]
            [reagent-material-ui.core.input-label :refer [input-label]]
            [reagent-material-ui.core.checkbox :refer [checkbox]]
            [reagent-material-ui.core.button :refer [button]]))


(defn row [item type]
  [list-item {:button true
              :on-click #(js/window.location.assign (str "#/" (-> type :type name) "/edit/" (-> item :id name)))}
   (for [field (filter #(:default %) (:fields type))]
     ^{:key field}
     [list-item-text {:primary ((:key field) item)}])])

(defn show? [item show-hidden]
  (if (get item :hidden false)
    show-hidden
    true))

(defn list-panel [type props]
  (rf/dispatch [:set-fab-actions (get-in type [:actions :list])])
  (let [show-hidden @(rf/subscribe [::cs/show-hidden])
        heading (or (:label type) (-> (:subs type) name s/capitalize))]
    [paper {:class (get-in props [:classes :paper])}
     [grid {:container true
           :direction :column}
     [grid {:item true}
      [list {:subheader (ra/as-element [list-subheader heading])}
       (for [item (filter #(and (not (:reserved %)) (show? % show-hidden))
                          @(rf/subscribe [(:subs type)]))]
         ^{:key item}
         [row item type])]]
     [grid {:container true
            :justify :flex-end}
      [grid {:item true}
       (if show-hidden
         (shared/anchor #(rf/dispatch [::ce/crud-set-show-hidden false])
                        (str "Hide " (get type :hidden-label "hidden")))
         (shared/anchor #(rf/dispatch [::ce/crud-set-show-hidden true])
                        (str "Show " (get type :hidden-label "hidden"))))]]]]))


(defn build-checkbox
  [field {:keys [values handle-change handle-blur]}]
  (let [field-name (name (:key field))]
    ^{:key field-name}
    [grid {:item true}
     
     [form-control-label
      {:control (ra/as-element
                 [checkbox {:name      field-name
                            :checked   (values "send-invite" false)
                            :color :primary
                            :on-change handle-change
                            :on-blur   handle-blur
                            :disabled  (not (nil? (some #(values % false)
                                                        (get-in field [:disabled :if-fields]))))}])
       :label (get field :label (s/capitalize field-name))}]]))

(defn build-input
  [type field {:keys [values errors touched handle-change handle-blur]}]
  (let [field-name (name (:key field))
        error? (and (touched field-name)
                    (not (s/blank? (get errors field-name))))]
    ^{:key field-name}
    [grid {:item true}
     [text-field {:name       field-name
                  :label      (-> field-name s/capitalize)
                  :type       (:type field)
                  :margin      :normal
                  :auto-focus (and (:default field)
                                   (nil? ((:type type) @(rf/subscribe [:form-old]))))
                  :value      (values field-name "")
                  :on-change  handle-change
                  :on-blur    handle-blur
                  :disabled   (not (nil? (some #(values % false)
                                               (get-in field [:disabled :if-fields]))))
                  :error      error?
                  :helper-text (when error? (get errors field-name))}]]))

(defn build-select
  [field {:keys [values handle-change handle-blur]}]
  (let [field-name (name (:key field))]
    ^{:key field-name}
    [grid {:item true}
     [:label (get field :label (-> field-name s/capitalize))]
     [:select {:name      field-name
               :value     (values field-name)
               :on-change handle-change
               :on-blur   handle-blur}
      (for [option (:options field)]
        ^{:key (key option)}
        [:option {:value (key option)} (-> option val)])]]))

(defn build-multi-select
  [field {:keys [values set-handle-change]}]
  (let [field-name (name (:key field))]
    ^{:key field-name}
    [grid {:item true}
     [form-control {:margin :normal}
      [input-label (get field :label (-> field-name s/capitalize))]
      [select
       {:name field-name
        :multiple true
        :value (values field-name ["viewer"])
        :render-value #(->> (map (fn [s] (get (:options field) s)) %) (s/join ", "))
        :on-change #(set-handle-change
                     {:value (keep identity (-> % .-target .-value))
                      :path [field-name]})
        :disabled (not (nil? (some #(values % false)
                                   (get-in field [:disabled :if-fields]))))}
       (for [option (:options field)]
         ^{:key (key option)}
         [menu-item {:value (key option)}
          [checkbox {:color :primary
                     :checked (not (nil? (some #{(-> option key name)}
                                               (values field-name ["viewer"]))))}]
          [list-item-text {:primary (-> option val)}]])]]]))

(defn build-hidden
  [type {:keys [values handle-change handle-blur]}]
  [form-control-label
   {:control (ra/as-element
              [checkbox {:name      "hidden"
                         :checked   (values "hidden" false)
                         :color :primary
                         :on-change handle-change
                         :on-blur   handle-blur}])
    :label (->> (get type :hidden-label "Hidden")
                s/capitalize
                (str " "))}])

(defn edit-panel [type props]
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
   (fn [{:keys [form-id submitting? handle-submit] :as options}]
     [:form {:id form-id :on-submit handle-submit}
      [paper {:class (get-in props [:classes :paper])}
       [grid {:container true
              :direction :column}
        (doall
         (for [field (:fields type)]
           (case (:type field)
             :checkbox (build-checkbox field options)
             :select (build-select field options)
             :select-multi (build-multi-select field options)
             (build-input type field options))))
        (if-let [extra-fn (:extra type)]
          (extra-fn props options))
        (build-hidden type options)
        [grid {:container true
               :direction :row
               :justify :flex-start
               :spacing 1
               :class (get-in props [:classes :buttons])}
         [grid {:item true}
          [button {:variant :contained
                   :color :primary
                   :type :submit
                   :disabled submitting?}
           "Save"]]
         [grid {:item true}
          [button {:variant :outlined
                   :type :button
                   :on-click #(js/window.history.back)}
           "Cancel"]]]]]])])
