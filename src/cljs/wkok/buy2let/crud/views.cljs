(ns wkok.buy2let.crud.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [clojure.string :as s]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.crud.events :as ce]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.currencies :as currencies]
            [fork.re-frame :as fork]
            [clojure.walk :as w]
            [reagent-material-ui.core.list :refer [list]]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.form-control-label :refer [form-control-label]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.core.switch-component :refer [switch]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.select :refer [select]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.list-item-text :refer [list-item-text]]
            [reagent-material-ui.core.form-control :refer [form-control]]
            [reagent-material-ui.core.input-label :refer [input-label]]
            [reagent-material-ui.core.checkbox :refer [checkbox]]
            [reagent-material-ui.core.button :refer [button]]))


(defn row [item type]
  (let [default-field (-> (filter #(:default %) (:fields type)) first)
        props {:button true
               :on-click #(js/window.location.assign (str "#/" (-> type :type name) "/edit/" (-> item :id name)))}]
    (if-let [secondary-field (:secondary default-field)]
      [list-item props
       [list-item-text {:primary ((:key default-field) item) :secondary (secondary-field item)}]]
      [list-item props
       [list-item-text {:primary ((:key default-field) item)}]])))

(defn show? [item show-hidden]
  (if (get item :hidden false)
    show-hidden
    true))

(defn list-panel [type props]
  (if (shared/has-role :editor)
    (rf/dispatch [:set-fab-actions (get-in type [:actions :list])])
    (rf/dispatch [:set-fab-actions nil]))
  (let [show-hidden @(rf/subscribe [::cs/show-hidden])
        cruds @(rf/subscribe [(:subs type)])]
    (if (empty? cruds)
      [grid {:container true
             :direction :column
             :justify :center
             :align-items :center
             :style {:min-height "60vh"}
             :spacing 2}
       [grid {:item true}
        [typography {:variant :h5} (str "No " (-> type :type name) " yet")]]
       [grid {:item true}
        [typography (:empty-message type)]]]
      [paper {:class (get-in props [:classes :paper])}
       [grid {:container true
              :direction :column}
        [grid {:item true}
         [list
          (for [item (filter #(and (not (:reserved %)) (show? % show-hidden))
                             cruds)]
            ^{:key item}
            [row item type])]]
        (when ((:show-show-hidden? type))
          [grid {:container true
                 :justify :flex-end}
           [grid {:item true}
            [form-control-label
             {:control (ra/as-element
                        [switch {:color :primary
                                 :on-change #(rf/dispatch [::ce/crud-set-show-hidden (not show-hidden)])
                                 :checked show-hidden}])
              :label (ra/as-element
                      [typography {:variant :body2}
                       (str "Show " (get type :hidden-label "hidden"))])
              :label-placement :start}]]])]])))


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
                  :margin     :normal
                  :full-width true
                  :auto-focus (and (:default field)
                                   (nil? ((:type type) @(rf/subscribe [:form-old]))))
                  :value      (values field-name "")
                  :on-change  handle-change
                  :on-blur    handle-blur
                  :disabled   (not (nil? (some #(values % false)
                                               (get-in field [:disabled :if-fields]))))
                  :error      error?
                  :helper-text (when error? (get errors field-name))}]]))

(defn build-select-currency
  [field {:keys [values state touched errors]}]
  (let [field-name (name (:key field))
        error? (and (touched field-name)
                    (not (s/blank? (get errors field-name))))]
    ^{:key field-name}
    [grid {:item true}
     [currencies/select-currency {:value (values field-name "")
                                  :on-change #(swap! state assoc-in [:values field-name] %)
                                  :error      error?
                                  :helper-text (when error? (get errors field-name))}]]))

(defn build-select
  [field {:keys [values state]}]
  (let [field-name (name (:key field))]
    ^{:key field-name}
    [grid {:item true}
     [text-field {:select true
                  :name  field-name
                  :label (get field :label (-> field-name s/capitalize))
                  :value (values field-name "")
                  :on-change #(swap! state assoc-in [:values field-name]
                                     (-> % .-target .-value keyword))}
      (for [option (:options field)]
        ^{:key (:key option)}
        [menu-item {:value (:key option)} (:val option)])]]))

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
        :value (values field-name [])
        :render-value #(->> (map (fn [s] (get (:options field) s)) %) (s/join ", "))
        :on-change (if-let [on-change (:on-change field)]
                     #(on-change field-name (-> % .-target .-value) set-handle-change)
                     #(set-handle-change
                       {:value (keep identity (-> % .-target .-value))
                        :path [field-name]}))
        :disabled (not (nil? (some #(values % false)
                                   (get-in field [:disabled :if-fields]))))}
       (for [option (:options field)]
         ^{:key (key option)}
         [menu-item {:value (key option)}
          [checkbox {:color :primary
                     :checked (not (nil? (some #{(-> option key name)}
                                               (values field-name []))))}]
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

(defn apply-edit-defaults [old type]
  (into {} (for [[k v] old]
             {k (if (contains? (-> type :defaults :edit) k)
                  (-> type :defaults :edit k)
                  v)})))

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
                                    (-> old (apply-edit-defaults type) w/stringify-keys)
                                    (w/stringify-keys (-> type :defaults :add)))}
   (fn [{:keys [form-id submitting? handle-submit] :as options}]
     [:form {:id form-id :on-submit handle-submit}
      [paper {:class (get-in props [:classes :paper])}
       [grid {:container true
              :direction :column
              :spacing 1}
        (doall
         (for [field (:fields type)]
           (case (:type field)
             :checkbox (build-checkbox field options)
             :select (build-select field options)
             :select-multi (build-multi-select field options)
             :select-currency (build-select-currency field options)
             (build-input type field options))))
        (if-let [extra-fn (:extra type)]
          (extra-fn props options))
        (if-let [allow-hidden? (:allow-hidden? type)]
          (when (allow-hidden?)
            (build-hidden type options))
          (build-hidden type options))
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
