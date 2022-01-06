(ns wkok.buy2let.spec
  (:require [clojure.spec.alpha :as s]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Fields
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::id keyword?)
(s/def ::name string?)
(s/def ::email (s/and string? #(re-matches #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$" %)))
(s/def ::uid string?)
(s/def ::display-name string?)
(s/def ::avatar-url any?)
(s/def ::photo-url any?)
(s/def ::who-pays-whom #{:opa :ac :apo :aps :mi :opb :ops :tpa :tpo :oct :oca})
(s/def ::invoiced boolean?)
(s/def ::amount float?)
(s/def ::bank-interest float?)
(s/def ::role #{:viewer :editor :owner})
(s/def ::default-account-id keyword?)
(s/def ::provider keyword?)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::claims (s/keys :req-un [::roles]))
(s/def ::account (s/keys :req-un [::id ::name]
                         :opt-un [::avatar-url]))
(s/def ::user (s/keys :req-un [::id ::name]
                      :opt-un [::avatar-url ::default-account-id]))
(s/def ::delegate (s/keys :req-un [::id ::name ::email]))
(s/def ::auth (s/keys :req-un [::uid ::display-name]
                      :opt-un [::photo-url]))
(s/def ::charge (s/keys :req-un [::id ::name]))
(s/def ::property (s/keys :req-un [::id ::name ::charges]))
(s/def ::property-charge (s/keys :req-un [::who-pays-whom]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Collections
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(s/def ::delegates (s/map-of keyword? ::delegate))
(s/def ::crud-charges (s/map-of keyword? ::charge))
(s/def ::properties (s/map-of keyword? ::property))
(s/def ::charges (s/map-of keyword? ::property-charge))
(s/def ::roles (s/map-of ::role ::role-accounts))
(s/def ::role-accounts (s/coll-of ::id))
(s/def ::providers (s/coll-of ::provider))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Ledger
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::ledger-year (s/keys :req-un [::property-id ::year ::ledger-months]))
(s/def ::ledger-months (s/map-of keyword? ::ledger))
(s/def ::ledger-month (s/keys :req-un [::property-id ::year ::month ::ledger]))
(s/def ::ledger (s/keys :req-un [::breakdown ::accounting ::totals]))
(s/def ::accounting (s/keys :opt-un [::bank-interest ::bank-current ::supplier ::tenant ::agent-commission ::owner ::agent-current]))
(s/def ::totals (s/map-of keyword? float?))
(s/def ::breakdown (s/map-of keyword? ::breakdown-detail))
(s/def ::breakdown-detail (s/keys :req-un [::invoiced] :opt-un [::amount]))
(s/def ::bank-interest (s/map-of keyword? float?))
(s/def ::bank-current (s/map-of keyword? float?))
(s/def ::supplier (s/map-of keyword? float?))
(s/def ::tenant (s/map-of keyword? float?))
(s/def ::agent-commission (s/map-of keyword? float?))
(s/def ::owner (s/map-of keyword? float?))
(s/def ::agent-current (s/map-of keyword? float?))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Wrapping clojure.spec.alpha for convenience
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn conform [spec data]
  (let [result (s/conform spec data)]
    (if (= result ::s/invalid)
      (throw (ex-info "Invalid input" (s/explain-data spec data)))
      result)))
