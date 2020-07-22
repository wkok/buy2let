(ns wkok.buy2let.backend.impl
  (:require [wkok.buy2let.backend.demo :as backend]))

(defonce backend (backend/DemoBackend.))
