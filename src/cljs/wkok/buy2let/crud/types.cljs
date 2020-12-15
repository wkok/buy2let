(ns wkok.buy2let.crud.types
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.account.subs :as as]
            [wkok.buy2let.backend.subs :as bs]
            [goog.crypt.base64 :as b64]
            [clojure.string :as s]
            [reagent-material-ui.icons.add :refer [add]]
            [reagent-material-ui.core.form-control-label :refer [form-control-label]]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.checkbox :refer [checkbox]]
            [reagent-material-ui.core.grid :refer [grid]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]
            [reagent-material-ui.core.list-item :refer [list-item]]
            [reagent-material-ui.core.list-subheader :refer [list-subheader]]
            [reagent-material-ui.core.list :refer [list]]))

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
   :subs        ::cs/all-properties
   :fields      [{:key :name :type :text :default true}]
   :validate-fn #(merge (validate-name %) (validate-who-pays %))
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/properties/add") :icon [add]
                                 :title "Add"}}}
   :extra       (fn [props {:keys [values state errors touched _ handle-blur]}]
                  [grid {:item true}
                   [list {:subheader (ra/as-element [list-subheader "Charges to account for"])}
                    (-> (for [charge (filter #(and (not (:reserved %)) (not (:hidden %)))
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
                                       :class (get-in props [:classes :who-pays-whom])}
                                 (let [field-name (str charge-id "-wpw")
                                       error? (and (touched field-name)
                                                   (not (s/blank? (get errors field-name))))]
                                   [text-field {:select true
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
:singular "property"})

(def charge
  {:type        :charges
   :subs        ::cs/all-charges
   :fields      [{:key :name :type :text :default true}]
   :validate-fn #(validate-name %)
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/charges/add") :icon [add]
                                 :title "Add"}}}
   :singular "charge"})

(defn calc-status [item]
  (assoc item :status
         (if (:hidden item)
           "REVOKED"
           (if (:send-invite item)
             "INVITED"
             "ACTIVE"))))

(defn create-invite [item]
  (if (:send-invite item) 
    (assoc item :invitation
         (let [accounts @(rf/subscribe [::as/accounts])
               account-id @(rf/subscribe [::as/account])
               local-user @(rf/subscribe [::bs/local-user])]
           {:to (:email item)
            :template {:name "invitation"
                       :data {:delegate-name (:name item)
                              :user-name (:name local-user)
                              :account-name (-> (filter #(= account-id (key %)) accounts)
                                                first
                                                val
                                                :name)
                              :accept-url (str (.. js/window -location -protocol) "//"
                                               (.. js/window -location -host)
                                               "?invitation=" (b64/encodeString {:delegate-id (:id item)
                                                                                 :account-id account-id}))}}}))
    item))

(defn select-lower-weighted [roles]
  (let [weighted {1 "viewer"
                  2 "editor"
                  3 "owner"}]
    
    (->> (map (fn [role]
                (let [role-idx (->> (filter #(= role (val %)) weighted)
                                    first
                                    key)]
                  (->> (filter #(<= (key %) role-idx) weighted)
                       vals)))
              roles)
         flatten
         distinct
         (keep identity))))

(defn on-change-delegate-role
  [field-name selected-roles set-handle-change]
  (let [roles (-> selected-roles seq select-lower-weighted)]
    (set-handle-change
     {:value roles
      :path [field-name]})))

(def delegate
  {:type        :delegates
   :subs        ::cs/all-delegates
   :fields      [{:key :name :type :text :default true :secondary :status}
                 {:key :email :type :email
                  :disabled {:if-fields ["status"]}}
                 {:key :roles :type :select-multi
                  :options {"viewer" "View only"
                            "editor" "View & edit"
                            "owner" "Account owner"}
                  :on-change on-change-delegate-role
                  :disabled {:if-fields ["hidden"]}}
                 {:key :send-invite :type :checkbox
                  :label " Send invitation"
                  :disabled {:if-fields ["hidden"]}}]
   :defaults {:add {:send-invite true
                    :roles ["viewer"]}
              :edit {:send-invite false}}
   :calculated-fn #(-> % calc-status create-invite)
   :validate-fn #(merge (validate-name %) (validate-email %))
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/delegates/add") :icon [add]
                                 :title "Add"}}}
   :hidden-label "revoked"
   :singular "delegate"})