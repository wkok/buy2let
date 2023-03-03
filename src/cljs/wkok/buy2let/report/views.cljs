(ns wkok.buy2let.report.views
  (:require-macros [reagent-mui.util :refer [react-component]])
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.site.styles :refer [classes]]
            [wkok.buy2let.report.events :as re]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.report.subs :as rs]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.period :as period]
            [tick.core :as t]
            [clojure.string :as s]
            [reagent-mui.icons.cloud-download :refer [cloud-download]]
            [reagent-mui.icons.edit :refer [edit]]
            [reagent-mui.icons.attach-file :refer [attach-file]]
            [reagent-mui.icons.note-outlined :refer [note-outlined]]
            [reagent-mui.material.paper :refer [paper]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.typography :refer [typography]]
            [reagent-mui.material.text-field :refer [text-field]]
            [reagent-mui.material.menu-item :refer [menu-item]]
            [reagent-mui.material.icon-button :refer [icon-button]]
            [reagent-mui.material.tooltip :refer [tooltip]]
            [reagent-mui.material.table :refer [table]]
            [reagent-mui.material.table-container :refer [table-container]]
            [reagent-mui.material.table-head :refer [table-head]]
            [reagent-mui.material.table-body :refer [table-body]]
            [reagent-mui.material.table-row :refer [table-row]]
            [reagent-mui.material.table-cell :refer [table-cell]]
            [reagent-mui.material.form-control-label :refer [form-control-label]]
            [reagent-mui.material.switch :refer [switch]]
            [reagent-mui.x.date-picker :refer [date-picker]]))

(defn cell-class
  ([m amount]
   (cell-class m amount :none))
  ([m amount type]
   (let [odd (odd? (-> m :month name js/parseInt))
         class-key (if (neg? amount)
                     (if odd
                       (case type
                         :owed :table-header-alternate-owe
                         :profit :table-header-alternate-neg
                         :table-header-alternate)
                       (case type
                         :owed :table-header-owe
                         :profit :table-header-neg
                         :table-header))
                     (if (pos? amount)
                       (if odd
                         (case type
                           :owed :table-header-alternate-owe
                           :profit :table-header-alternate-pos
                           :table-header-alternate)
                         (case type
                           :owed :table-header-owe
                           :profit :table-header-pos
                           :table-header))
                       (if odd
                         :table-header-alternate
                         :table-header)))]
     (class-key classes))))

(defn format-amount [ledger path]
  (->> (get-in ledger path) shared/format-money))

(defn report-header [months]
  (let [class (:table-header classes)]
    [table-row
     [table-cell {:class class} "Charge"]
     (for [m months]
       ^{:key m}
       [table-cell {:class (cell-class m 0)
                    :align :right} (str (shared/format-month (:date m)) " " (t/year (:date m)))])
     [table-cell {:class class
                  :align :right} "Total"]]))

(defn report-charge-col
  [m charge {:keys [ledger report property] :as options}]
  [table-cell {:align :right
               :class (when (odd? (-> m :month name js/parseInt))
                        (:table-alternate classes))}
   [grid {:container true
          :direction :row
          :justify-content :flex-end}
    (when (= true (:show-invoices report))
      (let [note (get-in ledger [(:year m) (:month m) :breakdown (:id charge) :note])]
        (when (not (s/blank? note))
          [grid {:item true}
           [icon-button {:size :small
                         :on-click #(rf/dispatch [::se/dialog {:heading "Note" :message note}])}
            [note-outlined {:font-size :small}]]])))
    (when (= true (:show-invoices report))
      (let [invoice-options (assoc options
                                   :year (:year m)
                                   :month (:month m)
                                   :charge-id (:id charge))]
        (when (not-empty @(rf/subscribe [::cs/invoices-for invoice-options]))
          [grid {:item true}
           [shared/invoices-button charge invoice-options :small]])))

    ;; Temporary to access legacy attachments, remove in 2023
    (when (= true (:show-invoices report))
      (when (get-in ledger [(:year m) (:month m) :breakdown (:id charge) :invoiced])
        [grid {:item true}
         [icon-button {:size :small
                       :on-click #(rf/dispatch [::shared/view-invoice
                                                (:id property)
                                                (:year m)
                                                (:month m)
                                                charge])}
          [attach-file {:font-size :small}]]]))
    [grid {:item true}
     (format-amount ledger [(:year m) (:month m) :breakdown (:id charge) :amount])]]])

(defn report-profit-col
  [m {:keys [ledger]}]
  (let [profit (shared/calc-profit-property ledger m)
        class (cell-class m profit :profit)]
    [table-cell {:align :right
                 :class class}
     (shared/format-money profit)]))

(defn report-owed-col
  [m {:keys [ledger]}]
  (let [agent-bal (-> (get-in ledger [(:year m) (:month m) :totals :agent-current]) shared/to-money)
        tenant-bal (-> (get-in ledger [(:year m) (:month m) :totals :tenant]) shared/to-money)
        owed (if (not (zero? agent-bal))
               agent-bal tenant-bal)
        class (cell-class m owed :owed)]
    [table-cell {:align :right
                 :class class}
     (shared/format-money owed)]))

(defn report-yield-col
  [m {:keys [ledger]}]
  (let [year (:year m)
        month (:month m)
        yield (-> ledger year month :yield :net)]
    [table-cell {:align :right
                 :class (cell-class m yield :profit)}
     (shared/format-money yield)]))

(defn report-roi-col
  [m {:keys [ledger]}]
  (let [year (:year m)
        month (:month m)
        roi(-> ledger year month :yield :roi)]
    [table-cell {:align :right
                 :class (cell-class m roi :profit)}
     (shared/format-money roi)]))

(defn report-edit-col
  [m {:keys [property]}]
  [table-cell {:align :right
               :class (cell-class m 0)}
   [tooltip {:title "Edit"}
    [icon-button {:on-click #(js/window.location.assign (str "#/reconcile/" (-> property :id name)
                                                                      "/" (-> (:month m) name)
                                                                      "/" (-> (:year m) name)
                                                                      "/edit"))}
    [edit {:color :primary}]]]])

(defn report-charge-row
  [charge months {:keys [report] :as options}]
  [table-row
   [table-cell (:name charge)]
   (for [m months]
     ^{:key m}
     [report-charge-col m charge options])
   [table-cell {:align :right
                :class (:table-header classes)}
    (-> (get-in report [:result :totals :breakdown (:id charge) :amount])
        shared/format-money)]])

(defn report-profit-row-total
  [{:keys [report]}]
  (let [profit (+ (get-in report [:result :totals :accounting :owner])
                  (get-in report [:result :totals :accounting :agent-current])
                  (get-in report [:result :totals :accounting :tenant]))
        class (if (neg? profit)
                (:table-header-neg classes)
                (if (pos? profit)
                  (:table-header-pos classes)
                  (:table-header classes)))]
    [table-cell {:align :right
                 :class class}
     (shared/format-money profit)]))

(defn report-profit-row
  [months options]
  [table-row
   [table-cell {:class (:table-header classes)}
    [:strong [:label.report-view-amount-pos "Profit"] " / " [:label.report-view-amount-neg "(Loss)"]]]
   (for [m months]
     ^{:key m}
     [report-profit-col m options])
   [report-profit-row-total options]])

(defn report-owed-row
  [months options]
  [table-row
   [table-cell
    [:strong [:label.report-view-amount-owe "Owed"] " / " [:label.report-view-amount-neg "(Owing)"]]]
   (for [m months]
     ^{:key m}
     [report-owed-col m options])
   [table-cell {:align :right} "-"]])

(defn report-yield-row-total
  [{:keys [report property]}]
  (let [yield (shared/calc-yield (get-in report [:result :totals :breakdown]) (:purchase-price property))
        class (if (neg? yield)
                (:table-header-neg classes)
                (if (pos? yield)
                  (:table-header-pos classes)
                  (:table-header classes)))]
    [table-cell {:align :right
                 :class class}
     (shared/format-money yield)]))

(defn report-yield-row
  [months options]
  [table-row
   [table-cell [:strong "Net Yield %"]]
   (for [m months]
     ^{:key m}
     [report-yield-col m options])
   [report-yield-row-total options]])

(defn report-roi-row-total
  [{:keys [report property]}]
  (let [yield (shared/calc-yield (get-in report [:result :totals :breakdown]) (:cash-invested property))
        class (if (neg? yield)
                (:table-header-neg classes)
                (if (pos? yield)
                  (:table-header-pos classes)
                  (:table-header classes)))]
    [table-cell {:align :right
                 :class class}
     (shared/format-money yield)]))

(defn report-roi-row
  [months options]
  [table-row
   [table-cell [:strong "ROI %"]]
   (for [m months]
     ^{:key m}
     [report-roi-col m options])
   [report-roi-row-total options]])

(defn report-edit-row
  [months options]
  [table-row
   [table-cell]
   (for [m months]
     ^{:key m}
     [report-edit-col m options])
   [table-cell]])

(defn zip-invoices-confirm [property-charges]
  (rf/dispatch [::se/dialog {:heading "Download?"
                             :message "This will download all invoices for the selected period in a categorised zipped file"
                             :buttons {:left  {:text     "Yes"
                                               :on-click #(rf/dispatch [::re/zip-invoices property-charges])}
                                       :right {:text     "Cancel"}}}]))

(defn view-report
  [{:keys [property-charges report] :as options}]
  (rf/dispatch [:set-fab-actions {:left-1 {:fn   #(zip-invoices-confirm property-charges)
                                           :icon [cloud-download]
                                           :title "Download"}}])
  (let [months (-> (get-in report [:result :months]) reverse)]
    [paper {:sx {:width 1}}
     [grid {:container true
            :direction :column}
      [grid {:item true :xs 12}
       [table-container
        [table {:size :small}
         [table-head
          [report-header months]]
         [table-body
          (for [charge property-charges]
            ^{:key (:id charge)}
            [report-charge-row charge months options])
          [report-profit-row months options]
          [report-owed-row months options]
          [report-yield-row months options]
          [report-roi-row months options]
          #_[report-cash-row months options]
          (when (shared/has-role :editor)
            [report-edit-row months options])]]]]
      [grid {:container true
             :item true
             :class (:paper classes)
             :justify-content :flex-end}
       [form-control-label
        {:control (ra/as-element
                   [switch {:color :primary
                            :on-change #(rf/dispatch [::re/report-show-invoices-toggle])
                            :checked @(rf/subscribe [::rs/report-show-invoices])}])
         :label (ra/as-element
                 [typography {:variant :body2}
                  "Invoices & notes"])
         :label-placement :start}]]]]))

(defn criteria
  [{:keys [properties]}]
  (let [active-property @(rf/subscribe [::ss/active-property])]
    (shared/select-default-property active-property properties ::re/report-set-property)
    [paper {:class (:paper classes)}
     [grid {:container true
            :direction :row
            :spacing 2}
      [grid {:item true
             :xs 12 :sm 6}
       [text-field {:select true
                    :variant :standard
                    :label "Property"
                    :field     :list
                    :on-change #(rf/dispatch [::re/report-set-property (.. % -target -value)])
                    :value     (shared/select-property-val active-property properties)}
        (map (fn [property]
               ^{:key property}
               [menu-item {:value (:id property)}
                (:name property)]) properties)]]
      [grid {:item true
             :xs 12 :sm 6}
       [grid {:container true
              :direction :row
              :spacing 3}
        [grid {:item true
               :xs 6}
         [date-picker {:open-to :month
                       :views [:year :month]
                       :render-input (react-component [props]
                                                      [text-field (merge props
                                                                         {:variant :standard})])
                       :input-format "MMM YYYY"
                       :label "From"
                       :value (period/year-month->inst
                               @(rf/subscribe [::rs/report-year :from])
                               @(rf/subscribe [::rs/report-month :from]))
                       :on-change #(when %
                                     (rf/dispatch [::re/report-set-month :from (period/date->month %)])
                                     (rf/dispatch [::re/report-set-year :from (period/date->year %)]))
                       :auto-ok true}]]
        [grid {:item true
               :xs 6}
         [date-picker {:open-to :month
                       :views [:year :month]
                       :render-input (react-component [props]
                                                      [text-field (merge props
                                                                         {:variant :standard})])
                       :input-format "MMM YYYY"
                       :label "To"
                       :value (period/year-month->inst
                               @(rf/subscribe [::rs/report-year :to])
                               @(rf/subscribe [::rs/report-month :to]))
                       :on-change #(when %
                                     (rf/dispatch [::re/report-set-month :to (period/date->month %)])
                                     (rf/dispatch [::re/report-set-year :to (period/date->year %)]))
                       :auto-ok true}]]]]]]))

(defn view-panel
  [{:keys [property] :as options}]
  [grid {:container true
         :direction :column
         :spacing 2}
   [grid {:item true}
    [criteria options]]
   [grid {:container true
          :item true :xs 12
          :class (:scroll-x classes)}
    (if (not (nil? property))
      [view-report options]
      (rf/dispatch [:set-fab-actions nil]))]])


(defn report []
  (let [properties @(rf/subscribe [::cs/properties])
        charges @(rf/subscribe [::cs/charges])
        report @(rf/subscribe [::rs/report])
        property-id @(rf/subscribe [::ss/active-property])
        property (shared/by-id property-id properties)
        property-charges (->> (map #(shared/by-id % charges) (keys (:charges property)))
                              (filter #(not (:hidden %)))
                              (remove nil?)
                              (sort-by :name))
        ledger @(rf/subscribe [:ledger-property (:id property)])]

    (rf/dispatch [::re/report-set-property property-id])    ;Refresh as property might have been changed from another view
    [view-panel {:property property
                 :report report
                 :ledger ledger
                 :properties properties
                 :property-charges property-charges}]))
