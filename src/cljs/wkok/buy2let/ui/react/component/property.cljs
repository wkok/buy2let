(ns wkok.buy2let.ui.react.component.property
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.site.styles :refer [classes]]
            [clojure.string :as s]
            [reagent-mui.material.form-control-label :refer [form-control-label]]
            [reagent-mui.material.text-field :refer [text-field]]
            [reagent-mui.material.checkbox :refer [checkbox]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.menu-item :refer [menu-item]]
            [reagent-mui.material.list-item :refer [list-item]]
            [reagent-mui.material.list-subheader :refer [list-subheader]]
            [reagent-mui.material.list :refer [list]]))

(defn property-charges
  [{:keys [values state errors touched _ handle-blur]}]
  [grid {:item true}
   [list {:subheader (ra/as-element [list-subheader "Charges to account for"])}
    (-> (for [charge (filter #(not (:reserved %))
                             @(rf/subscribe [::cs/charges]))]
          (let [charge-id (name (:id charge))
                charge-selected (contains? (get values "charges") charge-id)]
            ^{:key (:id charge)}
            [list-item
             [grid {:container true
                    :direction :row}
              [grid {:item true :xs 12 :sm 6}
               [form-control-label
                {:control (ra/as-element
                           [checkbox {:type      :checkbox
                                      :name      charge-id
                                      :color :primary
                                      :checked   charge-selected
                                      :on-change #(if (-> % .-target .-checked)
                                                    (swap! state assoc-in [:values "charges" charge-id] {})
                                                    (swap! state update-in [:values "charges"] dissoc charge-id))
                                      :on-blur   handle-blur}])
                 :label (:name charge)}]]
              (when charge-selected
                [grid {:item true :xs 12 :sm 6
                       :class (:who-pays-whom classes)}
                 (let [field-name (str charge-id "-wpw")
                       error? (and (touched field-name)
                                   (not (s/blank? (get errors field-name))))]
                   [text-field {:variant :standard
                                :select true
                                :name      field-name
                                :label "Arrangement"
                                :value     (or (get-in values ["charges" charge-id "who-pays-whom"]) "")
                                :on-change #(swap! state assoc-in [:values "charges" charge-id "who-pays-whom"]
                                                   (-> % .-target .-value keyword))
                                :error error?
                                :helper-text (when error? (get errors field-name))
                                :full-width true}
                    [menu-item {:value :ac} "Agent Commission"]
                    [menu-item {:value :apo} "Agent pays Owner"]
                    [menu-item {:value :aps} "Agent pays Supplier"]
                    [menu-item {:value :mi} "Mortgage Interest"]
                    [menu-item {:value :oca} "Owner charges Agent"]
                    [menu-item {:value :oct} "Owner charges Tenant"]
                    [menu-item {:value :opa} "Owner pays Agent"]
                    [menu-item {:value :opb} "Owner pays Bank"]
                    [menu-item {:value :ops} "Owner pays Supplier"]
                    [menu-item {:value :tpa} "Tenant pays Agent"]
                    [menu-item {:value :tpo} "Tenant pays Owner"]])])]]))
        doall)]])
