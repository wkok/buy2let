(ns wkok.buy2let.spec
  (:require [clojure.spec.alpha :as s]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Fields
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::id keyword?)
(s/def ::name string?)
(s/def ::email (s/and string? #(re-matches #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$" %)))
(s/def ::accounts (s/coll-of keyword?))
(s/def ::uid string?)
(s/def ::display-name string?)
(s/def ::who-pays-whom #{"opa" "ac" "apo" "aps" "mi" "opb" "ops" "tpa" "tpo"})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::account (s/keys :req-un [::id ::name]))
(s/def ::user (s/keys :req-un [::id ::name ::email ::accounts]))
(s/def ::delegate (s/keys :req-un [::id ::name ::email]))
(s/def ::auth (s/keys :req-un [::uid ::display-name ::email]))
(s/def ::charge (s/keys :req-un [::id ::name]))
(s/def ::property (s/keys :req-un [::id ::name ::charges]))
(s/def ::property-charge (s/keys :req-un [::who-pays-whom]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Collections
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(s/def ::crud-users (s/map-of keyword? ::delegate))
(s/def ::crud-charges (s/map-of keyword? ::charge))
(s/def ::crud-properties (s/map-of keyword? ::property))
(s/def ::charges (s/map-of keyword? ::property-charge))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Wrapping clojure.spec.alpha for convenience 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn conform [spec data]
  (let [result (s/conform spec data)]
    (if (= result ::s/invalid)
      (throw (ex-info "Invalid input" (s/explain-data spec data)))
      result)))

