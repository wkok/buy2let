(ns wkok.buy2let.spec
  (:require [clojure.spec.alpha :as s]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Keys 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::id keyword?)
(s/def ::name string?)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::account (s/keys :req-un [::id ::name]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Wrapping clojure.spec.alpha for convenience 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn conform [spec data]
  (let [result (s/conform spec data)]
    (if (= result ::s/invalid)
      (throw (ex-info "Invalid input" (s/explain-data spec data)))
      result)))

