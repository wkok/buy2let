(ns wkok.buy2let.spec
  (:require [clojure.spec.alpha :as s]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Keys 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::id keyword?)
(s/def ::name string?)
(s/def ::email (s/and string? #(re-matches #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$" %)))
(s/def ::accounts (s/coll-of keyword?))
(s/def ::uid string?)
(s/def ::display-name string?)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::account (s/keys :req-un [::id ::name]))
(s/def ::user (s/keys :req-un [::id ::name ::email ::accounts]))
(s/def ::auth (s/keys :req-un [::uid ::display-name ::email]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Wrapping clojure.spec.alpha for convenience 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn conform [spec data]
  (let [result (s/conform spec data)]
    (if (= result ::s/invalid)
      (throw (ex-info "Invalid input" (s/explain-data spec data)))
      result)))

