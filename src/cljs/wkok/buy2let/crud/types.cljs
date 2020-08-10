(ns wkok.buy2let.crud.types
  (:require [re-frame.core :as rf]
            [wkok.buy2let.crud.subs :as cs]
            [clojure.string :as s]))

(defn validate-name [values]
  (when (s/blank? (get values "name"))
    {"name" "Name is required"}))

(defn validate-email [values]
  (when (s/blank? (get values "email"))
    {"email" "Email is required"}))

(defn validate-who-pays [values]
  (->> (filter #(or (= "none" (get (val %) "who-pays-whom"))
                    (not (contains? (val %) "who-pays-whom"))) (get values "charges"))
       (into {} (map #(hash-map (str (key %) "-wpw") "Please select one")))))

(def property
  {:type        :properties
   :subs        ::cs/properties
   :fields      [{:key :name :type :text :default true}]
   :validate-fn #(merge (validate-name %) (validate-who-pays %))
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/properties/add") :icon "fa-plus"}}}
   :extra       (fn [values state errors touched _ handle-blur]
                  [:div
                   [:label "Charges to account for: "]
                   [:table
                    [:tbody
                     (-> (for [charge (filter #(and (not (:reserved %)) (not (:hidden %)))
                                              @(rf/subscribe [::cs/charges]))]
                           (let [charge-id (name (:id charge))]
                             ^{:key (:id charge)}
                             [:tr
                              [:td
                               [:div
                                [:label
                                 [:input {:type      :checkbox
                                          :name      charge-id
                                          :checked   (contains? (get values "charges") charge-id)
                                          :on-change #(if (-> % .-target .-checked)
                                                        (swap! state assoc-in [:values "charges" charge-id] {})
                                                        (swap! state update-in [:values "charges"] dissoc charge-id))
                                          :on-blur   handle-blur}]
                                 (str " " (:name charge))]]]
                              (let [field-name (str charge-id "-wpw")]
                                [:td
                                 [:select {:name      field-name
                                           :value     (or (get-in values ["charges" charge-id "who-pays-whom"]) :none)
                                           :on-change #(swap! state assoc-in [:values "charges" charge-id "who-pays-whom"]
                                                              (-> % .-target .-value))}
                                  [:option {:value :none} "-- choose applicable --"]
                                  [:option {:value :ac} "Agent Commission"]
                                  [:option {:value :apo} "Agent pays Owner"]
                                  [:option {:value :aps} "Agent pays Supplier"]
                                  [:option {:value :mi} "Mortgage Interest"]
                                  [:option {:value :opa} "Owner pays Agent"]
                                  [:option {:value :opb} "Owner pays Bank"]
                                  [:option {:value :ops} "Owner pays Supplier"]
                                  [:option {:value :tpa} "Tenant pays Agent"]
                                  [:option {:value :tpo} "Tenant pays Owner"]]
                                 (when (touched field-name)
                                   [:div.validation-error (get errors field-name)])])]))
                         doall)]]])})

(def charge
  {:type        :charges
   :subs        ::cs/charges
   :fields      [{:key :name :type :text :default true}]
   :validate-fn #(validate-name %)
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/charges/add") :icon "fa-plus"}}}})

(def delegate
  {:type        :delegates
   :subs        ::cs/delegates
   :fields      [{:key :name :type :text :default true}
                 {:key :email :type :email}]
   :validate-fn #(merge (validate-name %) (validate-email %))
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/delegates/add") :icon "fa-plus"}}}})