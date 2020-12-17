(ns wkok.buy2let.report.views
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.report.events :as re]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.report.subs :as rs]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.shared :as shared]
            [tick.alpha.api :as t]
            [clojure.string :as s]
            [reagent-material-ui.icons.cloud-download :refer [cloud-download]]
            [reagent-material-ui.icons.edit :refer [edit]]
            [reagent-material-ui.icons.attach-file :refer [attach-file]]
            [reagent-material-ui.icons.note-outlined :refer [note-outlined]]
            [reagent-material-ui.core.paper :refer [paper]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.icon-button :refer [icon-button]]
            [reagent-material-ui.core.tooltip :refer [tooltip]]
            [reagent-material-ui.core.table :refer [table]]
            [reagent-material-ui.core.table-container :refer [table-container]]
            [reagent-material-ui.core.table-head :refer [table-head]]
            [reagent-material-ui.core.table-body :refer [table-body]]
            [reagent-material-ui.core.table-row :refer [table-row]]
            [reagent-material-ui.core.table-cell :refer [table-cell]]
            [reagent-material-ui.core.form-control-label :refer [form-control-label]]
            [reagent-material-ui.core.switch-component :refer [switch]]
            [reagent-material-ui.pickers.date-picker :refer [date-picker]]))

(defn cell-class
  ([m amount classes]
   (cell-class m amount :none classes))
  ([m amount type classes]
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

(defn report-header [months props]
  (let [class (get-in props [:classes :table-header])]
    [table-row
     [table-cell {:class class} "Charge"]
     (for [m months]
       ^{:key m}
       [table-cell {:class (cell-class m 0 (:classes props))
                    :align :right} (str (shared/format-month (:date m)) " " (t/year (:date m)))])
     [table-cell {:class class
                  :align :right} "Total"]]))

(defn report-charge-col
  [m charge {:keys [ledger report property props]}]
  [table-cell {:align :right
               :class (when (odd? (-> m :month name js/parseInt))
                        (get-in props [:classes :table-alternate]))}
   [grid {:container true
          :direction :row
          :justify :flex-end}
    (when (= true (:show-invoices report))
      (let [note (get-in ledger [(:year m) (:month m) :breakdown (:id charge) :note])]
        (when (not (s/blank? note))
          [grid {:item true}
           [icon-button {:size :small
                         :on-click #(rf/dispatch [::se/dialog {:heading "Note" :message note}])}
            [note-outlined {:font-size :small}]]])))
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
  [m {:keys [ledger props]}]
  (let [profit (shared/calc-profit-property ledger m)
        class (cell-class m profit :profit (:classes props))]
    [table-cell {:align :right
                 :class class}
     (shared/format-money profit)]))

(defn report-owed-col 
  [m {:keys [ledger props]}]
  (let [agent-bal (-> (get-in ledger [(:year m) (:month m) :totals :agent-current]) shared/to-money)
        tenant-bal (-> (get-in ledger [(:year m) (:month m) :totals :tenant]) shared/to-money)
        owed (if (not (zero? agent-bal))
               agent-bal tenant-bal)
        class (cell-class m owed :owed (:classes props))]
    [table-cell {:align :right
                 :class class}
     (shared/format-money owed)]))


(defn report-cash-col 
  [m {:keys [ledger props]}]
  (let [cash (-> (get-in ledger [(:year m) (:month m) :totals :owner]) shared/to-money)
        class (cell-class m cash (:classes props))]
    [table-cell {:align :right
                 :class class}
     (shared/format-money cash)]))

(defn report-edit-col 
  [m {:keys [property props]}]
  [table-cell {:align :right
               :class (cell-class m 0 (:classes props))}
   [tooltip {:title "Edit"}
    [icon-button {:on-click #(js/window.location.assign (str "#/reconcile/" (-> property :id name)
                                                                      "/" (-> (:month m) name)
                                                                      "/" (-> (:year m) name)
                                                                      "/edit"))}
    [edit {:color :primary}]]]])

(defn report-charge-row 
  [charge months {:keys [report props] :as options}]
  [table-row
   [table-cell (:name charge)]
   (for [m months]
     ^{:key m}
     [report-charge-col m charge options])
   [table-cell {:align :right
                :class (get-in props [:classes :table-header])}
    (-> (get-in report [:result :totals :breakdown (:id charge) :amount])
        shared/format-money)]])

(defn report-profit-row-total 
  [{:keys [report props]}]
  (let [profit (+ (get-in report [:result :totals :accounting :owner])
                  (get-in report [:result :totals :accounting :agent-current])
                  (get-in report [:result :totals :accounting :tenant]))
        class (if (neg? profit)
                (get-in props [:classes :table-header-neg])
                (if (pos? profit)
                  (get-in props [:classes :table-header-pos])
                  (get-in props [:classes :table-header])))]
    [table-cell {:align :right
                 :class class}
     (shared/format-money profit)]))

(defn report-profit-row 
  [months {:keys [props] :as options}]
  [table-row
   [table-cell {:class (get-in props [:classes :table-header])} 
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

(defn report-cash-row 
  [months options]
  [table-row
   [table-cell [:strong "Cash Flow"]]
   (for [m months]
     ^{:key m}
     [report-cash-col m options])
   [table-cell {:align :right} "-"]])

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
                             :message "This will download all invoices for the selected period & might take a while"
                             :buttons {:left  {:text     "Yes"
                                               :on-click #(rf/dispatch [::re/zip-invoices property-charges])}
                                       :right {:text     "Cancel"}}}]))

(defn view-report 
  [{:keys [property-charges report props] :as options}]
  (rf/dispatch [:set-fab-actions {:left-1 {:fn   #(zip-invoices-confirm property-charges)
                                           :icon [cloud-download]
                                           :title "Download"}}])
  (let [months (-> (get-in report [:result :months]) reverse)]
    [paper
     [grid {:container true
            :direction :column}
      [grid {:item true :xs 12}
       [table-container
        [table {:size :small}
         [table-head
          [report-header months props]]
         [table-body
          (for [charge property-charges]
            ^{:key (:id charge)}
            [report-charge-row charge months options])
          [report-profit-row months options]
          [report-owed-row months options]
          [report-cash-row months options]
          (when (shared/has-role :editor)
            [report-edit-row months options])]]]]
      [grid {:container true
             :item true
             :class (get-in props [:classes :paper])
             :justify :flex-end}
       [form-control-label
        {:control (ra/as-element
                   [switch {:color :primary
                            :on-change #(rf/dispatch [::re/report-show-invoices-toggle])
                            :checked @(rf/subscribe [::rs/report-show-invoices])}])
         :label "Invoices & notes"
         :label-placement :start}]]]]))

(defn criteria
  [{:keys [properties props]}]
  (let [active-property @(rf/subscribe [::ss/active-property])]
    (shared/select-default-property active-property properties ::re/report-set-property)
    [paper {:class (get-in props [:classes :paper])}
     [grid {:container true
            :direction :row
            :spacing 2}
      [grid {:item true
             :xs 12 :sm 6}
       [text-field {:select true
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
         [date-picker {:variant :inline
                       :open-to :month
                       :views [:year :month]
                       :format "MMM YYYY"
                       :label "From"
                       :value (.parse shared/date-utils (str (name @(rf/subscribe [::rs/report-year :from])) "/"
                                                             (name @(rf/subscribe [::rs/report-month :from]))) "yyyy/MM")
                       :on-change #(do (rf/dispatch [::re/report-set-month :from (->> % (.getMonth shared/date-utils) inc str keyword)])
                                       (rf/dispatch [::re/report-set-year :from (->> % (.getYear shared/date-utils) str keyword)]))
                       :auto-ok true}]]
        [grid {:item true
               :xs 6}
         [date-picker {:variant :inline
                       :open-to :month
                       :views [:year :month]
                       :format "MMM YYYY"
                       :label "To"
                       :value (.parse shared/date-utils (str (name @(rf/subscribe [::rs/report-year :to])) "/"
                                                             (name @(rf/subscribe [::rs/report-month :to]))) "yyyy/MM")
                       :on-change #(do (rf/dispatch [::re/report-set-month :to (->> % (.getMonth shared/date-utils) inc str keyword)])
                                       (rf/dispatch [::re/report-set-year :to (->> % (.getYear shared/date-utils) str keyword)]))
                       :auto-ok true}]]]]]]))

(defn view-panel 
  [{:keys [property] :as options}]
  [grid {:container true
         :direction :column
         :spacing 2}
   [grid {:item true}
    [criteria options]]
   [grid {:item true :xs 12}
    (if (not (nil? property))
      [view-report options]
      (rf/dispatch [:set-fab-actions nil]))]])


(defn report [props]
  (let [properties @(rf/subscribe [::cs/properties])
        charges @(rf/subscribe [::cs/charges])
        report @(rf/subscribe [::rs/report])
        property-id @(rf/subscribe [::ss/active-property])
        property (shared/by-id property-id properties)
        property-charges (->> (map #(shared/by-id % charges) (keys (:charges property)))
                              (filter #(not (:hidden %)))
                              (sort-by :name))
        ledger @(rf/subscribe [:ledger-property (:id property)])]

    (rf/dispatch [::re/report-set-property property-id])    ;Refresh as property might have been changed from another view
    [view-panel {:property property 
                 :report report 
                 :ledger ledger 
                 :properties properties 
                 :property-charges property-charges
                 :props props}]))
