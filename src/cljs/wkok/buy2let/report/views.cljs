(ns wkok.buy2let.report.views
  (:require [re-frame.core :as rf]
            [wkok.buy2let.report.events :as re]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.report.subs :as rs]
            [wkok.buy2let.site.subs :as ss]
            [wkok.buy2let.site.events :as se]
            ;; [wkok.buy2let.backend.subs :as fs]
            ;; [wkok.buy2let.backend.core :as fc]
            [wkok.buy2let.shared :as shared]
            [tick.alpha.api :as t]
            ;; [cljc.java-time.month :as tm]
            ;; [fork.core :as fork]
            [clojure.string :as s]))


(defn format-amount [ledger path]
  (->> (get-in ledger path) shared/format-money))

(defn report-header [months]
  [:tr
   [:th "Charge"]
   (for [m months]
     ^{:key m}
     [:th.report-view-amount-col (str (shared/format-month (:date m)) " " (t/year (:date m)))])
   [:th.report-view-amount-col.report-view-alternate-col "Total"]])

(defn report-charge-col [m ledger charge report property]
  [:td.report-view-amount-col
   [:div.report-charge-col
    [:div
     (format-amount ledger [(:year m) (:month m) :breakdown (:id charge) :amount])]
    (when (= true (:show-invoices report))
      [:div.report-charge-col-icons
       [:div.report-charge-col-icon
        (let [note (get-in ledger [(:year m) (:month m) :breakdown (:id charge) :note])]
          (if (not (s/blank? note))
            [:i.far.fa-sticky-note {:on-click #(rf/dispatch [::se/dialog {:heading "Note" :message note}])}]
            [:label]))]
       [:div.report-charge-col-icon
        (if (get-in ledger [(:year m) (:month m) :breakdown (:id charge) :invoiced])
          [:i.fas.fa-paperclip {:on-click #(rf/dispatch [::shared/view-invoice
                                                         (:id property)
                                                         (:year m)
                                                         (:month m)
                                                         charge])}]
          [:label])]])]])

(defn report-profit-col [m ledger]
  (let [profit (shared/calc-profit-property ledger m)]
    (if (neg? profit)
      [:td.report-view-alternate-col.report-view-amount-col.report-view-amount-neg
       [:strong (shared/format-money profit)]]
      (if (pos? profit)
        [:td.report-view-alternate-col.report-view-amount-col.report-view-amount-pos
         [:strong (shared/format-money profit)]]
        [:td.report-view-alternate-col.report-view-amount-col
         [:strong (shared/format-money profit)]]))))

(defn report-owed-col [m ledger]
  (let [owed (-> (get-in ledger [(:year m) (:month m) :totals :agent-current]) shared/to-money)]
    (if (neg? owed)
      [:td.report-view-alternate-col.report-view-amount-col.report-view-amount-neg
       [:strong (shared/format-money owed)]]
      (if (pos? owed)
        [:td.report-view-alternate-col.report-view-amount-col.report-view-amount-owe
         [:strong (shared/format-money owed)]]
        [:td.report-view-alternate-col.report-view-amount-col
         [:strong (shared/format-money owed)]]))))

(defn report-cash-col [m ledger]
  (let [cash (-> (get-in ledger [(:year m) (:month m) :totals :owner]) shared/to-money)]
    (if (neg? cash)
      [:td.report-view-alternate-col.report-view-amount-col.report-view-amount-neg
       [:strong (shared/format-money cash)]]
      (if (pos? cash)
        [:td.report-view-alternate-col.report-view-amount-col.report-view-amount-pos
         [:strong (shared/format-money cash)]]
        [:td.report-view-alternate-col.report-view-amount-col
         [:strong (shared/format-money cash)]]))))

(defn report-charge-row [charge ledger months report property]
  [:tr
   [:td (:name charge)]
   (for [m months]
     ^{:key m}
     [report-charge-col m ledger charge report property])
   [:td.report-view-amount-col.report-view-alternate-col
    [:strong (-> (get-in report [:result :totals :breakdown (:id charge) :amount])
                 shared/format-money)]]])

(defn report-profit-row-total [report]
  (let [profit (+ (get-in report [:result :totals :accounting :owner])
                  (get-in report [:result :totals :accounting :agent-current]))]
    (if (neg? profit)
      [:td.report-view-amount-col.report-view-alternate-col.reconcile-view-amount-neg
       [:strong (shared/format-money profit)]]
      [:td.report-view-amount-col.report-view-alternate-col.reconcile-view-amount-pos
       [:strong (shared/format-money profit)]])))

(defn report-profit-row [ledger months report]
  [:tr
   [:td.report-view-alternate-col [:strong [:label.report-view-amount-pos "Profit"] " / " [:label.report-view-amount-neg "(Loss)"]]]
   (for [m months]
     ^{:key m}
     [report-profit-col m ledger])
   [report-profit-row-total report]])

(defn report-owed-row [ledger months]
  [:tr
   [:td.report-view-alternate-col [:strong [:label.report-view-amount-owe "Owed"] " / " [:label.report-view-amount-neg "(Owing)"]]]
   (for [m months]
     ^{:key m}
     [report-owed-col m ledger])
   [:td.report-view-amount-col.report-view-alternate-col "-"]])

(defn report-cash-row [ledger months]
  [:tr
   [:td.report-view-alternate-col [:strong "Cash Flow"]]
   (for [m months]
     ^{:key m}
     [report-cash-col m ledger])
   [:td.report-view-amount-col.report-view-alternate-col "-"]])

(defn zip-invoices-confirm [property-charges]
  (rf/dispatch [::se/dialog {:heading "Continue?"
                             :message "This will download all invoices for the selected period & might take a while"
                             :buttons {:left  {:text     "Yes"
                                               :on-click #(rf/dispatch [::re/zip-invoices property-charges])}
                                       :right {:text     "Cancel"}}}]))

(defn view-report [ledger property-charges report property]
  (rf/dispatch [:set-fab-actions {:left-1 {:fn   #(zip-invoices-confirm property-charges)
                                           :icon "fa-download"}}])
  (let [months (-> (get-in report [:result :months]) reverse)]
    [:div
     [:div.scrollable-x
      [:table.report-view-container
       [:thead
        [report-header months]]
       [:tbody
        (for [charge property-charges]
          ^{:key (:id charge)}
          [report-charge-row charge ledger months report property])
        [report-profit-row ledger months report]
        [report-owed-row ledger months]
        [report-cash-row ledger months]]]]
     [:div.report-view-show-invoices
      (if (= true (:show-invoices report))
        (shared/anchor #(rf/dispatch [::re/report-set-show-invoices false])
                       "Hide invoices / notes")
        (shared/anchor #(rf/dispatch [::re/report-set-show-invoices true])
                       "Show invoices / notes"))]]))

(defn view-panel [property report ledger properties property-charges]
  [:div
   [:div.report-options-container
    (shared/select-property properties
                            #(rf/dispatch [::re/report-set-property (.. % -target -value)])
                            @(rf/subscribe [::ss/active-property])
                            "--select--")
    [:div.report-options
     [:label "From:"
      [:div.year-month
       (shared/select-month #(rf/dispatch [::re/report-set-month :from (.. % -target -value)])
                            @(rf/subscribe [::rs/report-month :from]))
       (shared/select-year #(rf/dispatch [::re/report-set-year :from (.. % -target -value)])
                           @(rf/subscribe [::rs/report-year :from]))]]
     [:label "To:"
      [:div.year-month
       (shared/select-month #(rf/dispatch [::re/report-set-month :to (.. % -target -value)])
                            @(rf/subscribe [::rs/report-month :to]))
       (shared/select-year #(rf/dispatch [::re/report-set-year :to (.. % -target -value)])
                           @(rf/subscribe [::rs/report-year :to]))]]]]
   [:br]
   (if (not (nil? property))
     [view-report ledger property-charges report property]
     (rf/dispatch [:set-fab-actions nil]))])



(defn report []
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
    [view-panel property report ledger properties property-charges]))
