(ns wkok.buy2let.db.default)

(def default-db
  {:site      {:heading      "Dashboard"
               :show-progress true
               :active-property      "--select--"
               :active-page :dashboard
               :splash true}
   :report    {:show-invoices false}
   :charges   {:agent-opening-balance {:id       :agent-opening-balance
                                       :name     "Opening balance"
                                       :reserved true}}})
