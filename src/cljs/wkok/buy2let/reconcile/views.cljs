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
            [reagent-material-ui.core.card :refer [card]]
            [reagent-material-ui.core.card-content :refer [card-content]]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.typography :refer [typography]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.table :refer [table]]
            [reagent-material-ui.core.table-container :refer [table-container]]
            [reagent-material-ui.core.table-head :refer [table-head]]
            [reagent-material-ui.core.table-body :refer [table-body]]
            [reagent-material-ui.core.table-row :refer [table-row]]
            [reagent-material-ui.core.table-cell :refer [table-cell]]
))


(defn format-amount [ledger path]
  (->> (get-in ledger path) shared/format-money))

(defn view-accounting-header []
  [:thead
   [:tr
    [:th]
    [:th]
    [:th.reconcile-view-merged-col.reconcile-view-alternate-col {:col-span 2} "Agent"]
    [:th]
    [:th.reconcile-view-merged-col.reconcile-view-alternate-col {:col-span 2} "Bank"]
    [:th]]
   [:tr
    [:th "Charge"]
    [:th.reconcile-view-amount-col "Tenant"]
    [:th.reconcile-view-amount-col.reconcile-view-alternate-col "Current"]
    [:th.reconcile-view-amount-col.reconcile-view-alternate-col "Comm."]
    [:th.reconcile-view-amount-col "Owner"]
    [:th.reconcile-view-amount-col.reconcile-view-alternate-col "Current"]
    [:th.reconcile-view-amount-col.reconcile-view-alternate-col "Interest"]
    [:th.reconcile-view-amount-col "Supplier"]]])

(defn view-accounting-row [charge ledger]
  [:tr
   [:td (:name charge)]
   [:td.reconcile-view-amount-col
    (format-amount ledger [:this-month :accounting :tenant (:id charge)])]
   [:td.reconcile-view-amount-col.reconcile-view-alternate-col
    (format-amount ledger [:this-month :accounting :agent-current (:id charge)])]
   [:td.reconcile-view-amount-col.reconcile-view-alternate-col
    (format-amount ledger [:this-month :accounting :agent-commission (:id charge)])]
   [:td.reconcile-view-amount-col
    (format-amount ledger [:this-month :accounting :owner (:id charge)])]
   [:td.reconcile-view-amount-col.reconcile-view-alternate-col
    (format-amount ledger [:this-month :accounting :bank-current (:id charge)])]
   [:td.reconcile-view-amount-col.reconcile-view-alternate-col
    (format-amount ledger [:this-month :accounting :bank-interest (:id charge)])]
   [:td.reconcile-view-amount-col
    (format-amount ledger [:this-month :accounting :supplier (:id charge)])]])

(defn view-accounting-detail [ledger property-charges]
  (for [charge property-charges]
    ^{:key (:id charge)}
    [view-accounting-row charge ledger]))

(defn view-accounting-total [ledger]
  [:tr
   [:td [:strong "Total:"]]
   [:td.reconcile-view-amount-col
    [:strong (format-amount ledger [:this-month :totals :tenant])]]
   (let [agent-balance (get-in ledger [:this-month :totals :agent-current])]
     [:td {:class (if (neg? agent-balance)
                    "reconcile-view-alternate-col reconcile-view-amount-col reconcile-view-amount-neg"
                    (if (pos? agent-balance)
                      "reconcile-view-alternate-col reconcile-view-amount-col reconcile-view-amount-owe"
                      "reconcile-view-alternate-col reconcile-view-amount-col"))}
      [:strong (->> agent-balance shared/format-money)]])
   [:td.reconcile-view-amount-col.reconcile-view-alternate-col
    [:strong (format-amount ledger [:this-month :totals :agent-commission])]]
   (let [owner-balance (get-in ledger [:this-month :totals :owner])]
     [:td {:class (if (neg? owner-balance)
                    "reconcile-view-amount-col reconcile-view-amount-neg"
                    "reconcile-view-amount-col reconcile-view-amount-pos")}
      [:strong (->> owner-balance shared/format-money)]])
   [:td.reconcile-view-amount-col.reconcile-view-alternate-col
    [:strong (format-amount ledger [:this-month :totals :bank-current])]]
   [:td.reconcile-view-amount-col.reconcile-view-alternate-col
    [:strong (format-amount ledger [:this-month :totals :bank-interest])]]
   [:td.reconcile-view-amount-col
    [:strong (format-amount ledger [:this-month :totals :supplier])]]])

(defn view-accounting [ledger property-charges charges]
  [:div
   [:div.scrollable-x
    [:table.reconcile-view-container
     [view-accounting-header]
     [:tbody
      [view-accounting-row (shared/by-id :agent-opening-balance charges) ledger]
      (view-accounting-detail ledger property-charges)
      [view-accounting-total ledger]]]]
   [:div.reconcile-view-accounting
    (shared/anchor #(rf/dispatch [::re/reconcile-view-toggle])
                   "Simple view")]])

(defn view-overview-row
  [charge {:keys [ledger property year month]}]
  [table-row
   [table-cell (:name charge)]
   [table-cell {:align :right}
    (format-amount ledger [:this-month :breakdown (:id charge) :amount])]
   [table-cell (when (get-in ledger [:this-month :breakdown (:id charge) :invoiced])
                 [:i.fas.fa-paperclip {:on-click #(rf/dispatch [::shared/view-invoice
                                                                (:id property)
                                                                year
                                                                month
                                                                charge])}])]
   [table-cell (let [note (get-in ledger [:this-month :breakdown (:id charge) :note])]
                 (when (not (s/blank? note))
                   [:i.far.fa-sticky-note {:on-click #(rf/dispatch [::se/dialog {:heading "Note" :message note}])}]))]])

(defn view-overview
  [{:keys [property-charges props] :as options}]
  (let [class-table-header (get-in props [:classes :table-header])]
    [paper 
     [grid {:container true
            :direction :column}
      [grid {:item true}
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
      [grid {:item true}
       [grid {:container true
              :justify :flex-end}
        [grid {:item true
               :class (get-in props [:classes :paper])}
         (shared/anchor #(rf/dispatch [::re/reconcile-view-toggle])
                        "Detailed view")]]]]]))

(defn build-edit-url []
  (let [options (re/calc-options {})]
    (str "#/reconcile/" (-> (:property-id options) name)
         "/" (-> (:month options) name)
         "/" (-> (:year options) name)
         "/edit")))

(defn criteria
  [{:keys [properties props]}]
  [paper {:class (get-in props [:classes :paper])}
   [grid {:container true
          :direction :row
          :justify :space-between}
    [grid {:item true}
     (shared/select-property properties
                             #(rf/dispatch [::re/reconcile-set-property (.. % -target -value)])
                             @(rf/subscribe [::ss/active-property])
                             "--select--" "Property")]
    [grid {:item true}
     [grid {:container true
            :direction :row
            :wrap :nowrap}
      [grid {:item true}
       (shared/select-month #(rf/dispatch [::re/reconcile-set-month (.. % -target -value)])
                            @(rf/subscribe [::rs/reconcile-month])
                            "Period")]
      [grid {:item true}
       (shared/select-year #(rf/dispatch [::re/reconcile-set-year (.. % -target -value)])
                           @(rf/subscribe [::rs/reconcile-year]))]]]]])

(defn cards
  [{:keys [ledger]}]
  (let [profit (-> (+ (get-in ledger [:this-month :totals :owner])
                      (get-in ledger [:this-month :totals :agent-current]))
                   shared/to-money)
        owed (-> (get-in ledger [:this-month :totals :agent-current]) shared/to-money)
        cash (-> (get-in ledger [:this-month :totals :owner]) shared/to-money)]
    (rf/dispatch [:set-fab-actions {:left-1 {:fn #(js/window.location.assign (build-edit-url)) :icon "fa-edit"}}])
    [grid {:container true
           :direction :row}
     (when (not (zero? profit))
       [grid {:item true}
        (if (neg? profit)
          [card
           [card-content
            [typography {:variant :h5}
             (shared/format-money profit)]
            [typography {:color :textSecondary}
             "(net loss)"]]]
          [card
           [card-content
            [typography {:variant :h5}
             (shared/format-money profit)]
            [typography {:color :textSecondary}
             "(net profit)"]]])])
     (when (not (zero? owed))
       [grid {:item true}
        (if (pos? owed)
          [card
           [card-content
            [typography {:variant :h5}
             (shared/format-money owed)]
            [typography {:color :textSecondary}
             "(owed to owner)"]]]
          (when (neg? owed)
            [card
             [card-content
              [typography {:variant :h5}
               (shared/format-money owed)]
              [typography {:color :textSecondary}
               "(owed to agent)"]]]))])
     (when (not (= cash profit))
       [grid {:item true}
        (if (neg? cash)
          [card
           [card-content
            [typography {:variant :h5}
             (shared/format-money cash)]
            [typography {:color :textSecondary}
             "(cash flow)"]]]
          [card
           [card-content
            [typography {:variant :h5}
             (shared/format-money cash)]
            [typography {:color :textSecondary}
             "(cash flow)"]]])])]))

(defn view-panel
  [{:keys [property year month ledger property-charges charges] :as options}]
  [grid {:container true
         :direction :column
         :spacing 2}
   [grid {:item true}
    [criteria options]]
   (when (not-any? nil? [property year month])
     [grid {:item true}
      [cards options]])
   (when (not-any? nil? [property year month])
     [grid {:item true}
      (case @(rf/subscribe [::rs/reconcile-view-toggle])
        :accounting [view-accounting {:ledger ledger
                                      :property-charges property-charges
                                      :charges charges}]
        [view-overview options])])
   (when (not-any? nil? [property year month])
     (rf/dispatch [:set-fab-actions nil]))])

(defn swap-amount [val charge-id state format-fn parse-fn abs-fn]
  (let [path [:values :this-month :breakdown charge-id :amount]]
    (if (not (s/blank? val))
      (swap! state assoc-in path (->> val format-fn parse-fn abs-fn))
      (swap! state update-in [:values :this-month :breakdown charge-id] dissoc :amount))))

(defn prev-month [charge values state]
  (when-let [amount (get-in values [:prev-month :breakdown (:id charge) :amount])]
    [:div.reconcile-edit-component-amount-prev
     (shared/anchor #(swap-amount amount (:id charge) state shared/format-money js/parseFloat Math/abs)
                    (str "(use previous: " (shared/format-money amount) ")"))]))

(defn edit-amount-field [charge values state]
  [:div.reconcile-edit-component-amount
   [:strong (str (:name charge) ":")]
   [:input {:name        "amount"
            :type        :number
            :value       (get-in values [:this-month :breakdown (:id charge) :amount])
            :on-change   #(swap-amount (-> % .-target .-value) (:id charge) state identity identity identity)
            :on-blur     #(swap-amount (-> % .-target .-value) (:id charge) state shared/format-money js/parseFloat Math/abs)
            :min         0 :step "0.01"
            :placeholder "0.00"}]
   [prev-month charge values state]])

(defn edit-invoice-field-upload [charge state handle-blur text icon]
  [:div.upload-btn-wrapper
   [:button.upload-btn {:type :button}
    [:i {:aria-hidden "true" :class (str "fa " icon)}] text]
   [:input {:name      "invoice"
            :type      :file
            :on-change #(do (swap! state assoc-in [:values :this-month :breakdown (:id charge) :invoice] (-> % .-target .-files (aget 0)))
                            (swap! state update-in [:values :this-month :breakdown (:id charge)] dissoc :invoice-deleted))
            :on-blur   handle-blur}]])

(defn swap-invoice-deleted [state charge]
  (swap! state update-in [:values :this-month :breakdown (:id charge)] dissoc :invoice)
  (swap! state assoc-in [:values :this-month :breakdown (:id charge) :invoiced] false)
  (swap! state assoc-in [:values :this-month :breakdown (:id charge) :invoice-deleted] true))

(defn edit-invoice-field-delete [charge state]
  [:div.upload-btn-wrapper
   [:button.upload-btn {:type     :button
                        :on-click #(when (= true (get-in @state [:values :this-month :breakdown (:id charge) :invoiced]))
                                     (rf/dispatch [::se/dialog {:heading "Are you sure?"
                                                                :message "Invoice will be deleted after this form is saved"
                                                                :buttons {:left  {:text     "Yes"
                                                                                  :on-click (fn [] (swap-invoice-deleted state charge))}
                                                                          :right {:text "Cancel"}}}]))}
    [:i {:aria-hidden "true" :class "fa fa-trash"}]]])

(defn edit-invoice-field [charge values state handle-blur property year month]
  [:div.reconcile-edit-component-invoice
   (let [attached (get-in values [:this-month :breakdown (:id charge) :invoice])
         uploaded (get-in values [:this-month :breakdown (:id charge) :invoiced])]
     (if uploaded
       [:div
        [edit-invoice-field-upload charge state handle-blur "" "fa-upload"]
        [edit-invoice-field-delete charge state property year month]]
       (if attached
         [edit-invoice-field-upload charge state handle-blur " Invoice" "fa-check"]
         [edit-invoice-field-upload charge state handle-blur " Invoice" "fa-upload"])))])

(defn edit-note-field [charge values state handle-blur]
  [:div.reconcile-edit-component-note
   [:label "Note:"]
   [:textarea {:name        "note"
               :type        :textarea
               :value       (get-in values [:this-month :breakdown (:id charge) :note])
               :on-change   #(swap! state assoc-in [:values :this-month :breakdown (:id charge) :note]
                                    (-> % .-target .-value))
               :on-blur     handle-blur
               :placeholder "Any additional information"}]])

(defn edit-panel [property year month ledger property-charges]
  (rf/dispatch [:set-fab-actions nil])
  [:div
   [:div.reconcile-options-container
    [:h4 (:name property)]
    [:label (s/capitalize (str (t/month (t/new-date 2010 (-> month name js/parseInt) 1)) " / " (name year)))]]
   [fork/form {:form-id            "id"
               :path               :form
               :prevent-default?   true
               :clean-on-unmount?  true
               ;:validation         #(validate %)
               :on-submit-response {400 "client error"
                                    500 "server error"}
               :on-submit          #(rf/dispatch [::re/save-reconcile (:values %)])
               :initial-values     ledger}
    (fn [{:keys [values state form-id handle-blur submitting? handle-submit]}]
      [:form {:id form-id :on-submit handle-submit}
       [:hr]
       (doall
         (for [charge property-charges]
           ^{:key (:id charge)}
           [:div
            [:div.reconcile-edit-component-container
             [edit-amount-field charge values state]
             [edit-invoice-field charge values state handle-blur property year month]
             [edit-note-field charge values state handle-blur]]
            [:hr]]))
       [:div.buttons-save-cancel
        [:button {:type :submit :disabled submitting?} "Save"]
        [:button {:type :button :on-click #(js/window.history.back)} "Cancel"]]])]])


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
      :reconcile-edit [edit-panel property year month ledger property-charges]
      [view-panel {:property property 
                   :year year 
                   :month month 
                   :ledger ledger 
                   :properties properties 
                   :property-charges property-charges 
                   :charges charges
                   :props props}])))
