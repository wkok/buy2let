(ns wkok.buy2let.reconcile.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.reconcile.events :as re]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.reconcile.subs :as rs]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.shared :as shared]
            [tick.alpha.api :as t]
            [fork.re-frame :as fork]
            [clojure.string :as s]
            [reagent-material-ui.icons.note-outlined :refer [note-outlined]]
            [reagent-material-ui.icons.attach-file :refer [attach-file]]
            [reagent-material-ui.icons.edit :refer [edit]]
            [reagent-material-ui.icons.cloud-upload-outlined :refer [cloud-upload-outlined]]
            [reagent-material-ui.icons.cloud-done :refer [cloud-done]]
            [reagent-material-ui.icons.cloud-done-outlined :refer [cloud-done-outlined]]
            [reagent-material-ui.icons.delete-outlined :refer [delete-outlined]]
            [reagent-material-ui.core.tooltip :refer [tooltip]]
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.table :refer [table]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.button :refer [button]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.table-container :refer [table-container]]
            [reagent-material-ui.core.table-head :refer [table-head]]
            [reagent-material-ui.core.table-body :refer [table-body]]
            [reagent-material-ui.core.table-row :refer [table-row]]
            [reagent-material-ui.core.table-cell :refer [table-cell]]
            [reagent-material-ui.core.form-control-label :refer [form-control-label]]
            [reagent-material-ui.core.switch-component :refer [switch]]
            [reagent-material-ui.pickers.date-picker :refer [date-picker]]))


(defn format-amount [ledger path]
  (->> (get-in ledger path) shared/format-money))


(defn view-accounting-row [charge ledger props]
  [table-row
   [table-cell (:name charge)]
   [table-cell {:align :right}
    (format-amount ledger [:this-month :accounting :tenant (:id charge)])]
   [table-cell {:align :right
                :class (get-in props [:classes :table-alternate])}
    (format-amount ledger [:this-month :accounting :agent-current (:id charge)])]
   [table-cell {:align :right
                :class (get-in props [:classes :table-alternate])}
    (format-amount ledger [:this-month :accounting :agent-commission (:id charge)])]
   [table-cell {:align :right}
    (format-amount ledger [:this-month :accounting :owner (:id charge)])]
   [table-cell {:align :right
                :class (get-in props [:classes :table-alternate])}
    (format-amount ledger [:this-month :accounting :bank-current (:id charge)])]
   [table-cell {:align :right
                :class (get-in props [:classes :table-alternate])}
    (format-amount ledger [:this-month :accounting :bank-interest (:id charge)])]
   [table-cell {:align :right}
    (format-amount ledger [:this-month :accounting :supplier (:id charge)])]])

(defn view-accounting-detail [ledger property-charges props]
  (for [charge property-charges]
    ^{:key (:id charge)}
    [view-accounting-row charge ledger props]))

(defn view-accounting-total
  [{:keys [ledger props]}]
  (let [class-table-header (get-in props [:classes :table-header])
        class-table-header-alternate (get-in props [:classes :table-header-alternate])]
    [table-row
     [table-cell {:class class-table-header} "Total:"]
     [table-cell {:align :right
                  :class class-table-header}
      (format-amount ledger [:this-month :totals :tenant])]
     (let [agent-balance (get-in ledger [:this-month :totals :agent-current])]
       [table-cell {:align :right
                    :class class-table-header-alternate}
        (->> agent-balance shared/format-money)])
     [table-cell {:align :right
                  :class class-table-header-alternate}
      (format-amount ledger [:this-month :totals :agent-commission])]
     (let [owner-balance (get-in ledger [:this-month :totals :owner])]
       [table-cell {:align :right
                    :class class-table-header}
        (->> owner-balance shared/format-money)])
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
  [{:keys [ledger property-charges charges props] :as options}]
  (let [class-table-header (get-in props [:classes :table-header])]
    [paper
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
                        :class (get-in props [:classes :table-header-alternate])} "Agent"]
           [table-cell]
           [table-cell {:align :center
                        :col-span 2
                        :class (get-in props [:classes :table-header-alternate])} "Bank"]
           [table-cell]]
          [table-row
           [table-cell {:class class-table-header} "Charge"]
           [table-cell {:align :right
                        :class class-table-header} "Tenant"]
           [table-cell {:align :right
                        :class (get-in props [:classes :table-header-alternate])} "Current"]
           [table-cell {:align :right
                        :class (get-in props [:classes :table-header-alternate])} "Comm."]
           [table-cell {:align :right
                        :class class-table-header} "Owner"]
           [table-cell {:align :right
                        :class (get-in props [:classes :table-header-alternate])} "Current"]
           [table-cell {:align :right
                        :class (get-in props [:classes :table-header-alternate])} "Interest"]
           [table-cell {:align :right
                        :class class-table-header} "Supplier"]]]
         [table-body
          [view-accounting-row (shared/by-id :agent-opening-balance charges) ledger props]
          (view-accounting-detail ledger property-charges props)
          [view-accounting-total options]]]]]
      [grid {:container true
             :item true
             :class (get-in props [:classes :paper])
             :justify :flex-end}
       [grid {:item true}
        [form-control-label
         {:control (ra/as-element
                    [switch {:color :primary
                             :on-change #(rf/dispatch [::re/reconcile-view-toggle])
                             :checked (= :accounting @(rf/subscribe [::rs/reconcile-view-toggle]))}])
          :label "Detailed"
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
  [{:keys [property-charges props] :as options}]
  (let [class-table-header (get-in props [:classes :table-header])]
    [paper
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
             :class (get-in props [:classes :paper])
             :justify :flex-end}
       [grid {:item true}
        [form-control-label
         {:control (ra/as-element
                    [switch {:color :primary
                             :on-change #(rf/dispatch [::re/reconcile-view-toggle])
                             :checked (= :accounting @(rf/subscribe [::rs/reconcile-view-toggle]))}])
          :label "Detailed"
          :label-placement :start}]]]]]))

(defn build-edit-url []
  (let [options (re/calc-options {})]
    (str "#/reconcile/" (-> (:property-id options) name)
         "/" (-> (:month options) name)
         "/" (-> (:year options) name)
         "/edit")))



(defn criteria
  [{:keys [properties props]}]
  (let [active-property @(rf/subscribe [::ss/active-property])]
    (shared/select-default-property active-property properties ::re/reconcile-set-property)
    [paper {:class (get-in props [:classes :paper])}
     [grid {:container true
            :direction :row
            :justify :space-between
            :spacing 1}
      [grid {:item true}
       [text-field {:select true
                    :label "Property"
                    :field     :list
                    :on-change #(rf/dispatch [::re/reconcile-set-property (.. % -target -value)])
                    :value     (shared/select-property-val active-property properties)}
        (->> (filter #(not (:hidden %)) properties)
             (map (fn [property]
                    ^{:key property}
                    [menu-item {:value (:id property)}
                     (:name property)])))]]
      [grid {:item true}
       [date-picker {:variant :inline
                     :open-to :month
                     :views [:year :month]
                     :format "MMM YYYY"
                     :label "Period"
                     :value (.parse shared/date-utils (str (name @(rf/subscribe [::rs/reconcile-year])) "/"
                                                           (name @(rf/subscribe [::rs/reconcile-month]))) "yyyy/MM")
                     :on-change #(do (rf/dispatch [::re/reconcile-set-month (->> % (.getMonth shared/date-utils) inc str keyword)])
                                     (rf/dispatch [::re/reconcile-set-year (->> % (.getYear shared/date-utils) str keyword)]))
                     :auto-ok true}]]]]))

(defn cards
  [{:keys [ledger props]}]
  (let [profit (-> (+ (get-in ledger [:this-month :totals :owner])
                      (get-in ledger [:this-month :totals :agent-current]))
                   shared/to-money)
        owed (-> (get-in ledger [:this-month :totals :agent-current]) shared/to-money)
        cash (-> (get-in ledger [:this-month :totals :owner]) shared/to-money)
        card-class (get-in props [:classes :reconcile-card])]
    (if (shared/has-role :editor)
      (rf/dispatch [:set-fab-actions {:left-1 {:fn #(js/window.location.assign (build-edit-url)) :icon [edit]
                                             :title "Edit"}}])
      (rf/dispatch [:set-fab-actions nil]))
    [grid {:container true
           :direction :row
           :justify :space-between
           :spacing 2}
     (when (not (zero? profit))
       [grid {:item true :xs 4}
        (if (neg? profit)
          [card {:class card-class}
           [card-content
            [typography {:variant :h6}
             (shared/format-money profit)]
            [typography {:variant :caption}
             "(net loss)"]]]
          [card {:class card-class}
           [card-content
            [typography {:variant :h6}
             (shared/format-money profit)]
            [typography {:variant :caption}
             "(net profit)"]]])])
     (when (not (zero? owed))
       [grid {:item true :xs 4}
        (if (pos? owed)
          [card {:class card-class}
           [card-content
            [typography {:variant :h6}
             (shared/format-money owed)]
            [typography {:variant :caption}
             "(owed to owner)"]]]
          (when (neg? owed)
            [card {:class card-class}
             [card-content
              [typography {:variant :h6}
               (shared/format-money owed)]
              [typography {:variant :caption}
               "(owed to agent)"]]]))])
     (when (not (= cash profit))
       [grid {:item true :xs 4}
        (if (neg? cash)
          [card {:class card-class}
           [card-content
            [typography {:variant :h6}
             (shared/format-money cash)]
            [typography {:variant :caption}
             "(cash flow)"]]]
          [card {:class card-class}
           [card-content
            [typography {:variant :h6}
             (shared/format-money cash)]
            [typography {:variant :caption}
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
     [grid {:item true :xs 12}
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
    [text-field {:name        "amount"
                 :type        :number
                 :label       (:name charge)
                 :value       (get-in values [:this-month :breakdown (:id charge) :amount])
                 :on-change   #(swap-amount (-> % .-target .-value) (:id charge) state identity identity identity)
                 :on-blur     #(swap-amount (-> % .-target .-value) (:id charge) state js/parseFloat Math/abs shared/format-money)
                 :min         0 :step "0.01"
                 :placeholder "0.00"
                 :InputLabelProps {:shrink true}}]
    [grid {:container true
           :justify :flex-end}
     [prev-month charge values state]]]])

(defn edit-invoice-field-upload [charge state handle-blur icon]
  [:div
   [:input {:id        (:id charge)
            :name      "invoice"
            :type      :file
            :accept    "image/*,.pdf"
            :style     {:display :none}
            :on-change #(do (swap! state assoc-in [:values :this-month :breakdown (:id charge) :invoice] (-> % .-target .-files (aget 0)))
                            (swap! state update-in [:values :this-month :breakdown (:id charge)] dissoc :invoice-deleted))
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
  [text-field {:name        "note"
               :type        :textarea
               :multiline   true
               :rows        1
               :rows-max    4
               :label       "Note"
               :value       (get-in values [:this-month :breakdown (:id charge) :note])
               :on-change   #(swap! state assoc-in [:values :this-month :breakdown (:id charge) :note]
                                    (-> % .-target .-value))
               :on-blur     handle-blur
               :helper-text "Any additional information"}])

(defn edit-panel
  [{:keys [property year month ledger property-charges props] :as options}]
  (rf/dispatch [:set-fab-actions nil])
  [grid {:container true
         :direction :column
         :spacing 2}
   [grid {:item true}
    [paper {:class (get-in props [:classes :paper])}
     [grid {:container true
            :item true
            :direction :row
            :justify :space-between
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
                :justify :space-between}
          (doall
           (for [charge property-charges]
             ^{:key (:id charge)}
             [grid {:item true
                    :xs 12 :md 6 :lg 4}
              [paper {:class (get-in props [:classes :paper])}
               [grid {:container true
                      :item true
                      :direction :column}
                [grid {:container true
                       :item true
                       :direction :row
                       :spacing 1
                       :justify :space-between}
                 [edit-amount-field charge form]
                 [edit-invoice-field charge options form]]
                [grid {:container true
                       :item true
                       :direction :column}
                 [edit-note-field charge form]]]]]))]
         [grid {:container true
                :item true
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
            "Cancel"]]]]])]]])


(defn reconcile [props]
  (let [properties @(rf/subscribe [::cs/properties])
        charges @(rf/subscribe [::cs/charges])
        property (shared/by-id @(rf/subscribe [::ss/active-property]) properties)
        property-charges (->> (map #(shared/by-id % charges) (keys (:charges property)))
                              (filter #(not (:hidden %)))
                              (sort-by :name))
        year @(rf/subscribe [::rs/reconcile-year])
        month @(rf/subscribe [::rs/reconcile-month])
        ledger @(rf/subscribe [:ledger-months (:id property) year month])]

    (case @(rf/subscribe [::ss/active-panel])
      :reconcile-edit [edit-panel {:property property
                                   :year year
                                   :month month
                                   :ledger ledger
                                   :property-charges property-charges
                                   :props props}]
      [view-panel {:property property
                   :year year
                   :month month
                   :ledger ledger
                   :properties properties
                   :property-charges property-charges
                   :charges charges
                   :props props}])))
