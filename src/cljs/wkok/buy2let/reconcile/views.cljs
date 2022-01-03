(ns wkok.buy2let.reconcile.views
  (:require-macros [reagent-mui.util :refer [react-component]])
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.reconcile.events :as re]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.reconcile.subs :as rs]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.site.styles :refer [classes]]
            [tick.core :as t]
            [fork.re-frame :as fork]
            [clojure.string :as s]
            [reagent-mui.icons.note-outlined :refer [note-outlined]]
            [reagent-mui.icons.attach-file :refer [attach-file]]
            [reagent-mui.icons.edit :refer [edit]]
            [reagent-mui.icons.cloud-upload-outlined :refer [cloud-upload-outlined]]
            [reagent-mui.icons.cloud-done :refer [cloud-done]]
            [reagent-mui.icons.cloud-done-outlined :refer [cloud-done-outlined]]
            [reagent-mui.icons.delete-outlined :refer [delete-outlined]]
            [reagent-mui.material.tooltip :refer [tooltip]]
            [reagent-mui.material.card :refer [card]]
            [reagent-mui.material.card-content :refer [card-content]]
            [reagent-mui.material.paper :refer [paper]]
            [reagent-mui.material.typography :refer [typography]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.table :refer [table]]
            [reagent-mui.material.menu-item :refer [menu-item]]
            [reagent-mui.material.button :refer [button]]
            [reagent-mui.material.icon-button :refer [icon-button]]
            [reagent-mui.material.text-field :refer [text-field]]
            [reagent-mui.material.table-container :refer [table-container]]
            [reagent-mui.material.table-head :refer [table-head]]
            [reagent-mui.material.table-body :refer [table-body]]
            [reagent-mui.material.table-row :refer [table-row]]
            [reagent-mui.material.table-cell :refer [table-cell]]
            [reagent-mui.material.form-control-label :refer [form-control-label]]
            [reagent-mui.material.switch-component :refer [switch]]
            [reagent-mui.lab.date-picker :refer [date-picker]]
            [wkok.buy2let.period :as period]))


(defn format-amount [ledger path]
  (->> (get-in ledger path) shared/format-money))


(defn view-accounting-row [charge ledger]
  [table-row
   [table-cell (:name charge)]
   [table-cell {:align :right}
    (format-amount ledger [:this-month :accounting :tenant (:id charge)])]
   [table-cell {:align :right
                :class (:table-alternate classes)}
    (format-amount ledger [:this-month :accounting :agent-current (:id charge)])]
   [table-cell {:align :right
                :class (:table-alternate classes)}
    (format-amount ledger [:this-month :accounting :agent-commission (:id charge)])]
   [table-cell {:align :right}
    (format-amount ledger [:this-month :accounting :owner (:id charge)])]
   [table-cell {:align :right}
    (format-amount ledger [:this-month :accounting :owner-control (:id charge)])]
   [table-cell {:align :right
                :class (:table-alternate classes)}
    (format-amount ledger [:this-month :accounting :bank-current (:id charge)])]
   [table-cell {:align :right
                :class (:table-alternate classes)}
    (format-amount ledger [:this-month :accounting :bank-interest (:id charge)])]
   [table-cell {:align :right}
    (format-amount ledger [:this-month :accounting :supplier (:id charge)])]])

(defn view-accounting-detail [ledger property-charges]
  (for [charge property-charges]
    ^{:key (:id charge)}
    [view-accounting-row charge ledger]))

(defn view-accounting-total
  [{:keys [ledger]}]
  (let [class-table-header (:table-header classes)
        class-table-header-alternate (:table-header-alternate classes)
        agent-balance (get-in ledger [:this-month :totals :agent-current] 0)
        tenant-balance (get-in ledger [:this-month :totals :tenant] 0)
        owner-balance (get-in ledger [:this-month :totals :owner] 0)]
    [table-row
     [table-cell {:class class-table-header} "Total:"]
     [table-cell {:align :right
                  :class (if (and (zero? agent-balance)
                                  (not (zero? tenant-balance)))
                           (:table-header-owe classes)
                           (:table-header classes))}
      (->> tenant-balance shared/format-money)]
     [table-cell {:align :right
                  :class (if (not (zero? agent-balance))
                           (:table-header-alternate-owe classes)
                           (:table-header-alternate classes))}
      (->> agent-balance shared/format-money)]
     [table-cell {:align :right
                  :class class-table-header-alternate}
      (format-amount ledger [:this-month :totals :agent-commission])]
     [table-cell {:align :right
                  :class (if (neg? owner-balance)
                           (:table-header-neg classes)
                           (when (pos? owner-balance)
                             (:table-header-pos classes)))}
      (->> owner-balance shared/format-money)]
     [table-cell {:align :right
                  :class class-table-header}
      (format-amount ledger [:this-month :totals :owner-control])]
     [table-cell {:align :right
                  :class class-table-header-alternate}
      (format-amount ledger [:this-month :totals :bank-current])]
     [table-cell {:align :right
                  :class class-table-header-alternate}
      (format-amount ledger [:this-month :totals :bank-interest])]
     [table-cell {:align :right
                  :class class-table-header}
      (format-amount ledger [:this-month :totals :supplier])]]))

(defn view-accounting
  [{:keys [ledger property-charges charges] :as options}]
  (let [class-table-header (:table-header classes)
        agent-opening-balance (get-in ledger [:this-month :breakdown :agent-opening-balance :amount])
        tenant-opening-balance (get-in ledger [:this-month :breakdown :tenant-opening-balance :amount])]
    [paper {:sx {:width 1}}
     [grid {:container true
            :direction :column}
      [grid {:item true  :xs 12}
       [table-container
        [table {:size :small}
         [table-head
          [table-row
           [table-cell]
           [table-cell]
           [table-cell {:align :center
                        :col-span 2
                        :class (:table-header-alternate classes)} "Agent"]
           [table-cell {:align :center
                        :col-span 2
                        :class (:table-header classes)} "Owner"]
           [table-cell {:align :center
                        :col-span 2
                        :class (:table-header-alternate classes)} "Bank"]
           [table-cell]]
          [table-row
           [table-cell {:class class-table-header} "Charge"]
           [table-cell {:align :right
                        :class class-table-header} "Tenant"]
           [table-cell {:align :right
                        :class (:table-header-alternate classes)} "Current"]
           [table-cell {:align :right
                        :class (:table-header-alternate classes)} "Comm."]
           [table-cell {:align :right
                        :class class-table-header} "Current"]
           [table-cell {:align :right
                        :class class-table-header} "Control"]
           [table-cell {:align :right
                        :class (:table-header-alternate classes)} "Current"]
           [table-cell {:align :right
                        :class (:table-header-alternate classes)} "Interest"]
           [table-cell {:align :right
                        :class class-table-header} "Supplier"]]]
         [table-body
          (when (pos? agent-opening-balance) [view-accounting-row (shared/by-id :agent-opening-balance charges) ledger])
          (when (pos? tenant-opening-balance) [view-accounting-row (shared/by-id :tenant-opening-balance charges) ledger])
          (view-accounting-detail ledger property-charges)
          [view-accounting-total options]]]]]
      [grid {:container true
             :item true
             :class (:paper classes)
             :justify-content :flex-end}
       [grid {:item true}
        [form-control-label
         {:control (ra/as-element
                    [switch {:color :primary
                             :on-change #(rf/dispatch [::re/reconcile-view-toggle])
                             :checked (= :accounting @(rf/subscribe [::rs/reconcile-view-toggle]))}])
          :label (ra/as-element
                  [typography {:variant :body2}
                   "Detailed"])
          :label-placement :start}]]]]]))

(defn view-overview-row
  [charge {:keys [ledger property year month]}]
  [table-row
   [table-cell (:name charge)]
   [table-cell {:align :right}
    (format-amount ledger [:this-month :breakdown (:id charge) :amount])]
   [table-cell {:align :center}
    (when (get-in ledger [:this-month :breakdown (:id charge) :invoiced])
                 [icon-button {:size :small
                               :on-click #(rf/dispatch [::shared/view-invoice
                                                        (:id property)
                                                        year
                                                        month
                                                        charge])}
                  [attach-file {:font-size :small}]])]
   [table-cell {:align :center}
    (let [note (get-in ledger [:this-month :breakdown (:id charge) :note])]
                 (when (not (s/blank? note))
                   [icon-button {:size :small
                                 :on-click #(rf/dispatch [::se/dialog {:heading "Note" :message note}])}
                    [note-outlined {:font-size :small}]]))]])

(defn view-overview
  [{:keys [property-charges] :as options}]
  (let [class-table-header (:table-header classes)]
    [paper {:sx {:width 1}}
     [grid {:container true
            :direction :column}
      [grid {:item true  :xs 12}
       [table-container
        [table {:size :small}
         [table-head
          [table-row
           [table-cell {:class class-table-header} "Charge"]
           [table-cell {:class class-table-header
                        :align :right} "Amount"]
           [table-cell]
           [table-cell]]]
         [table-body
          (for [charge property-charges]
            ^{:key (:id charge)}
            [view-overview-row charge options])]]]]
      [grid {:container true
             :item true
             :class (:paper classes)
             :justify-content :flex-end}
       [grid {:item true}
        [form-control-label
         {:control (ra/as-element
                    [switch {:color :primary
                             :on-change #(rf/dispatch [::re/reconcile-view-toggle])
                             :checked (= :accounting @(rf/subscribe [::rs/reconcile-view-toggle]))}])
          :label (ra/as-element
                  [typography {:variant :body2}
                   "Detailed"])
          :label-placement :start}]]]]]))

(defn build-edit-url []
  (let [options (re/calc-options {})]
    (str "#/reconcile/" (-> (:property-id options) name)
         "/" (-> (:month options) name)
         "/" (-> (:year options) name)
         "/edit")))



(defn criteria
  [{:keys [properties]}]
  (let [active-property @(rf/subscribe [::ss/active-property])]
    (shared/select-default-property active-property properties ::re/reconcile-set-property)
    [paper {:class (:paper classes)}
     [grid {:container true
            :direction :row
            :justify-content :space-between
            :spacing 1}
      [grid {:item true
             :xs 8}
       [text-field {:variant :standard
                    :select true
                    :label "Property"
                    :field     :list
                    :on-change #(rf/dispatch [::re/reconcile-set-property (.. % -target -value)])
                    :value     (shared/select-property-val active-property properties)}
        (map (fn [property]
               ^{:key property}
               [menu-item {:value (:id property)}
                (:name property)]) properties)]]
      [grid {:item true
             :xs 4}
       [date-picker {:open-to :month
                     :views [:year :month]
                     :render-input (react-component [props]
                                     [text-field (merge props
                                                   {:variant :standard})])
                     :input-format "MMM YYYY"
                     :label "Period"
                     :value (period/year-month->inst
                              @(rf/subscribe [::rs/reconcile-year])
                              @(rf/subscribe [::rs/reconcile-month]))
                     :on-change #(when %
                                   (rf/dispatch [::re/reconcile-set-month (period/date->month %)])
                                   (rf/dispatch [::re/reconcile-set-year (period/date->year %)]))
                     :auto-ok true}]]]]))

(defn cards
  [{:keys [ledger]}]
  (let [agent-balance (get-in ledger [:this-month :totals :agent-current])
        tenant-balance (get-in ledger [:this-month :totals :tenant])
        owner-balance (get-in ledger [:this-month :totals :owner])
        ;; profit (if (zero? agent-balance)
        ;;          (-> (+ owner-balance tenant-balance)
        ;;              shared/to-money)
        ;;          (-> (+ owner-balance agent-balance)
        ;;              shared/to-money))
        profit (-> (+ owner-balance tenant-balance agent-balance)
                   shared/to-money)
        owed (let [agent-bal (-> (get-in ledger [:this-month :totals :agent-current]) shared/to-money)
                   tenant-bal (-> (get-in ledger [:this-month :totals :tenant]) shared/to-money)]
               (if (not (zero? agent-bal))
                 agent-bal tenant-bal))
        cash (-> (get-in ledger [:this-month :totals :owner]) shared/to-money)
        card-class (:reconcile-card classes)
        pos-class (:pos classes)
        neg-class (:neg classes)
        owe-class (:owe classes)]
    (if (shared/has-role :editor)
      (rf/dispatch [:set-fab-actions {:left-1 {:fn #(js/window.location.assign (build-edit-url)) :icon [edit]
                                               :title "Edit"}}])
      (rf/dispatch [:set-fab-actions nil]))
    [grid {:container true
           :direction :row
           :justify-content :space-between
           :spacing 2}
     [grid {:item true :xs 4}
      (if (neg? profit)
        [card {:class card-class}
         [card-content
          [typography {:variant :h6
                       :class neg-class}
           (shared/format-money profit)]
          [typography {:variant :caption
                       :class neg-class}
           "(net loss)"]]]
        (if (pos? profit)
          [card {:class card-class}
           [card-content
            [typography {:variant :h6
                         :class pos-class}
             (shared/format-money profit)]
            [typography {:variant :caption
                         :class pos-class}
             "(net profit)"]]]
          [card {:class card-class}
           [card-content
            [typography {:variant :h6}
             (shared/format-money profit)]
            [typography {:variant :caption}
             "(net profit)"]]]))]
     (when (not (zero? owed))
       [grid {:item true :xs 4}
        (if (pos? owed)
          [card {:class card-class}
           [card-content
            [typography {:variant :h6
                         :class owe-class}
             (shared/format-money owed)]
            [typography {:variant :caption
                         :class owe-class}
             "(owed to owner)"]]]
          (when (neg? owed)
            [card {:class card-class}
             [card-content
              [typography {:variant :h6
                           :class owe-class}
               (shared/format-money owed)]
              [typography {:variant :caption
                           :class owe-class}
               "(owed by owner)"]]]))])
     (when (not (= cash profit))
       [grid {:item true :xs 4}
        (if (neg? cash)
          [card {:class card-class}
           [card-content
            [typography {:variant :h6
                        ;;  :class neg-class
                         }
             (shared/format-money cash)]
            [typography {:variant :caption
                        ;;  :class neg-class
                         }
             "(cash flow)"]]]
          [card {:class card-class}
           [card-content
            [typography {:variant :h6
                        ;;  :class pos-class
                         }
             (shared/format-money cash)]
            [typography {:variant :caption
                        ;;  :class pos-class
                         }
             "(cash flow)"]]])])]))

(defn view-panel
  [{:keys [property year month] :as options}]
  [grid {:container true
         :direction :column
         :spacing 2}
   [grid {:item true}
    [criteria options]]
   (when (not-any? nil? [property year month])
     [grid {:item true}
      [cards options]])
   (when (not-any? nil? [property year month])
     [grid {:container true
            :item true :xs 12
            :class (:scroll-x classes)}
      (case @(rf/subscribe [::rs/reconcile-view-toggle])
        :accounting [view-accounting options]
        [view-overview options])])
   (when (some nil? [property year month])
     (rf/dispatch [:set-fab-actions nil]))])

(defn swap-amount [val charge-id state parse-fn abs-fn format-fn]
  (let [path [:values :this-month :breakdown charge-id :amount]]
    (if (not (s/blank? val))
      (swap! state assoc-in path (->> val parse-fn abs-fn format-fn))
      (swap! state update-in [:values :this-month :breakdown charge-id] dissoc :amount))))

(defn prev-month [charge values state]
  (when-let [amount (get-in values [:prev-month :breakdown (:id charge) :amount])]
    (shared/anchor #(swap-amount amount (:id charge) state js/parseFloat Math/abs shared/format-money)
                   (str "(use previous: " (shared/format-money amount) ")"))))

(defn edit-amount-field
  [charge {:keys [values state]}]
  [grid {:item true}
   [grid {:container true
          :direction :column}
    [text-field {:variant :standard
                 :name        "amount"
                 :type        :number
                 :label       (:name charge)
                 :value       (get-in values [:this-month :breakdown (:id charge) :amount])
                 :on-change   #(swap-amount (-> % .-target .-value) (:id charge) state identity identity identity)
                 :on-blur     #(swap-amount (-> % .-target .-value) (:id charge) state js/parseFloat Math/abs shared/format-money)
                 :min         0 :step "0.01"
                 :placeholder "0.00"
                 :InputLabelProps {:shrink true}}]
    [grid {:container true
           :justify-content :flex-end}
     [prev-month charge values state]]]])

(defn edit-invoice-field-upload [charge state handle-blur icon]
  [:div
   [:input {:id        (:id charge)
            :name      "invoice"
            :type      :file
            :accept    "image/*,.pdf"
            :style     {:display :none}
            :on-change #(let [file (-> % .-target .-files (aget 0))]
                          (when (shared/validate-file-size file 2000000)
                            (swap! state assoc-in [:values :this-month :breakdown (:id charge) :invoice] file)
                            (swap! state update-in [:values :this-month :breakdown (:id charge)] dissoc :invoice-deleted)))
            :on-blur   handle-blur}]
   [:label {:html-for (:id charge)}
    [tooltip {:title "Upload invoice"}
     [icon-button {:variant :contained
                   :component :span
                   :color :primary}
      icon]]]])

(defn swap-invoice-deleted [state charge]
  (swap! state update-in [:values :this-month :breakdown (:id charge)] dissoc :invoice)
  (swap! state assoc-in [:values :this-month :breakdown (:id charge) :invoiced] false)
  (swap! state assoc-in [:values :this-month :breakdown (:id charge) :invoice-deleted] true))

(defn edit-invoice-field-delete [charge state]
  [tooltip {:title "Delete invoice"}
   [icon-button {:color :secondary
                 :on-click   #(when (= true (get-in @state [:values :this-month :breakdown (:id charge) :invoiced]))
                                (rf/dispatch [::se/dialog {:heading "Delete invoice?"
                                                           :message "Invoice will be deleted after this form is saved"
                                                           :buttons {:left  {:text     "Delete"
                                                                             :color :secondary
                                                                             :on-click (fn [] (swap-invoice-deleted state charge))}
                                                                     :right {:text "Cancel"}}}]))}
    [delete-outlined]]])

(defn edit-invoice-field
  [charge {:keys [property year month]} {:keys [values state handle-blur]}]
  [grid {:item true}
   (let [attached (get-in values [:this-month :breakdown (:id charge) :invoice])
         uploaded (get-in values [:this-month :breakdown (:id charge) :invoiced])]
     (if uploaded
       [grid {:container true
              :direction :row}
        [grid {:item true}
         [edit-invoice-field-delete charge state property year month]]
        [grid {:item true}
         [edit-invoice-field-upload charge state handle-blur [cloud-done]]]]
       (if attached
         [edit-invoice-field-upload charge state handle-blur [cloud-done-outlined]]
         [edit-invoice-field-upload charge state handle-blur [cloud-upload-outlined]])))])

(defn edit-note-field
  [charge {:keys [values state handle-blur]}]
  [text-field {:variant :standard
               :name        "note"
               :type        :textarea
               :multiline   true
               :rows        1
               :max-rows    4
               :label       "Note"
               :value       (get-in values [:this-month :breakdown (:id charge) :note])
               :on-change   #(swap! state assoc-in [:values :this-month :breakdown (:id charge) :note]
                                    (-> % .-target .-value))
               :on-blur     handle-blur
               :helper-text "Any additional information"}])

(defn edit-panel
  [{:keys [property year month ledger property-charges] :as options}]
  (rf/dispatch [:set-fab-actions nil])
  [grid {:container true
         :direction :column
         :spacing 2}
   [grid {:item true}
    [paper {:class (:paper classes)}
     [grid {:container true
            :item true
            :direction :row
            :justify-content :space-between
            :spacing 1}
      [typography {:variant :h5} (:name property)]
      [typography {:variant :subtitle1} (s/capitalize (str (t/month (t/new-date 2010 (-> month name js/parseInt) 1)) " / " (name year)))]]]]
   [grid {:item true}
    [fork/form {:form-id            "id"
                :path               :form
                :prevent-default?   true
                :clean-on-unmount?  true
               ;:validation         #(validate %)
                :on-submit-response {400 "client error"
                                     500 "server error"}
                :on-submit          #(rf/dispatch [::re/save-reconcile (:values %)])
                :initial-values     (shared/apply-breakdown ledger shared/format-money)}
     (fn [{:keys [form-id submitting? handle-submit] :as form}]
       [:form {:id form-id :on-submit handle-submit}
        [grid {:container true
               :direction :column
               :spacing 2}
         [grid {:container true
                :item true
                :direction :row
                :spacing 2
                :justify-content :space-between}
          (doall
           (for [charge property-charges]
             ^{:key (:id charge)}
             [grid {:item true
                    :xs 12 :md 6 :lg 4}
              [paper {:class (:paper classes)}
               [grid {:container true
                      :item true
                      :direction :column}
                [grid {:container true
                       :item true
                       :direction :row
                       :spacing 1
                       :justify-content :space-between}
                 [edit-amount-field charge form]
                 [edit-invoice-field charge options form]]
                [grid {:container true
                       :item true
                       :direction :column}
                 [edit-note-field charge form]]]]]))]
         [grid {:container true
                :item true
                :direction :row
                :justify-content :flex-start
                :spacing 1
                :class (:buttons classes)}
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
            "Cancel"]]]]])]]])

(defn reconcile []
  (let [properties @(rf/subscribe [::cs/properties])
        charges @(rf/subscribe [::cs/charges])
        property (shared/by-id @(rf/subscribe [::ss/active-property]) properties)
        property-charges (->> (map #(shared/by-id % charges) (keys (:charges property)))
                              (filter #(not (:hidden %)))
                              (remove nil?)
                              (sort-by :name))
        year @(rf/subscribe [::rs/reconcile-year])
        month @(rf/subscribe [::rs/reconcile-month])
        ledger @(rf/subscribe [:ledger-months (:id property) year month])]
    (case @(rf/subscribe [::ss/active-panel])
      :reconcile-edit [edit-panel {:property property
                                   :year year
                                   :month month
                                   :ledger ledger
                                   :property-charges property-charges}]
      [view-panel {:property property
                   :year year
                   :month month
                   :ledger ledger
                   :properties properties
                   :property-charges property-charges
                   :charges charges}])))
