(ns wkok.buy2let.crud.types
  (:require [re-frame.core :as rf]
            [reagent.core :as ra]
            [wkok.buy2let.crud.subs :as cs]
            [wkok.buy2let.crud.events :as ce]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.account.subs :as as]
            [wkok.buy2let.backend.subs :as bs]
            [wkok.buy2let.site.styles :refer [classes]]
            [goog.crypt.base64 :as b64]
            [clojure.string :as s]
            [reagent-mui.icons.add :refer [add]]
            [reagent-mui.material.form-control-label :refer [form-control-label]]
            [reagent-mui.material.text-field :refer [text-field]]
            [reagent-mui.material.checkbox :refer [checkbox]]
            [reagent-mui.material.grid :refer [grid]]
            [reagent-mui.material.tooltip :refer [tooltip]]
            [reagent-mui.material.menu-item :refer [menu-item]]
            [reagent-mui.material.list-item :refer [list-item]]
            [reagent-mui.material.list-subheader :refer [list-subheader]]
            [reagent-mui.material.list-item-secondary-action :refer [list-item-secondary-action]]
            [reagent-mui.material.icon-button :refer [icon-button]]
            [reagent-mui.material.list :refer [list]]
            [reagent-mui.icons.visibility-outlined :refer [visibility-outlined]]
            [wkok.buy2let.reconcile.subs :as rs]
            [wkok.buy2let.site.subs :as ss]))

(defn validate-name [values]
  (when (s/blank? (get values "name"))
    {"name" "Name is required"}))

(defn validate-currency [values]
  (when (s/blank? (get values "currency"))
    {"currency" "Currency is required"}))

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
   :fields      [{:key :name :type :text :default true}
                 {:key :currency :type :select-currency}]
   :validate-fn #(merge (validate-name %) (validate-who-pays %) (validate-currency %))
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/properties/add") :icon [add]
                                 :title "Add"}}}
   :extra       (fn [{:keys [values state errors touched _ handle-blur]}]
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
   :singular "property"
   :allow-hidden? #(let [properties @(rf/subscribe [::cs/properties])]
                     (> (count properties) 1))
   :show-show-hidden? #(let [properties @(rf/subscribe [::cs/hidden-properties])]
                         (>= (count properties) 1))})

(def charge
  {:type        :charges
   :subs        ::cs/all-charges
   :fields      [{:key :name :type :text :default true}]
   :validate-fn #(validate-name %)
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/charges/add") :icon [add]
                                 :title "Add"}}}
   :singular "charge"
   :show-show-hidden? #(let [charges @(rf/subscribe [::cs/hidden-charges])]
                         (>= (count charges) 1))})

(defn get-invoices-uri-path
  []
  (str "#/reconcile/" (name @(rf/subscribe [::ss/active-property]))
       "/" (name @(rf/subscribe [::rs/reconcile-month]))
       "/" (name @(rf/subscribe [::rs/reconcile-year]))
       "/" (name @(rf/subscribe [::rs/reconcile-charge-id]))
       "/invoices"))

(defn get-invoice-keys
  []
  {:property-id @(rf/subscribe [::ss/active-property])
   :charge-id @(rf/subscribe [::rs/reconcile-charge-id])
   :year @(rf/subscribe [::rs/reconcile-year])
   :month @(rf/subscribe [::rs/reconcile-month])})

(defn get-invoices-charge-name
  []
  (-> @(rf/subscribe [::rs/reconcile-charge-id])
      (shared/by-id @(rf/subscribe [::cs/charges]))
      :name))

(defn invoice-attachment->blobs
  [invoice]
  (let [account-id (name @(rf/subscribe [::as/account]))]
    [{:path (str "data/" account-id "/invoices/" (-> invoice :id name))
      :file       (:attachment invoice)
      :action     (if (:attachment invoice)
                    :put
                    (when (:attachment-deleted invoice)
                      :delete))
      ;; :on-progress #(.log js/console (str "Upload is " % "%"))
      ;; :on-success #()
      :on-error   #(rf/dispatch [::se/dialog {:heading "Oops!"
                                              :message (str "Error uploading invoice:\nDetail: " %)}])
      :metadata {:customMetadata {"accountId" account-id}}}]))

(defn upload-invoice-attachment
  "Removes the attachment file blob as it is not stored in the database
  Sets the attached flag to true, only if attached is currently false & file was uploaded"
  [invoice]
  (when (or (:attachment invoice)
            (:attachment-deleted invoice))
    (rf/dispatch [::ce/upload-attachments (invoice-attachment->blobs invoice)]))

  (if (:attached invoice) ; already uploaded
    (dissoc invoice :attachment :attachment-deleted)
    (-> (dissoc invoice :attachment :attachment-deleted)
        (assoc :attached (not (nil? (:attachment invoice))))))
  )

(def invoice
  {:type        :invoices
   :subs        ::cs/all-invoices
   :fields      [{:key :name :type :text :default true}
                 {:key :invoice :type :attachment}]
   :sub-header-fn get-invoices-charge-name
   :validate-fn validate-name
   :key-fields-fn get-invoice-keys
   :uri-path-fn get-invoices-uri-path
   :actions     {:list {:left-1 {:fn #(js/window.location.assign (str (get-invoices-uri-path) "/add"))
                                 :icon [add]
                                 :title "Add"}}}
   :singular "invoice"
   :show-show-hidden? #(let [invoices @(rf/subscribe [::cs/hidden-invoices])]
                         (>= (count invoices) 1))
   :calculated-fn #(-> % upload-invoice-attachment)
   :secondary-action  (fn [invoice]
                        (when (:attached invoice)
                          (ra/as-element
                           [list-item-secondary-action
                            [tooltip {:title "View"}
                             [icon-button {:edge :end
                                           :color :primary
                                           :on-click #(rf/dispatch [::shared/view-attachment :invoices (:id invoice)])}
                              [visibility-outlined]]]])))})

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
                              :accept-url (str (shared/url-host)
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
   :singular "delegate"
   :show-show-hidden? #(let [delegates @(rf/subscribe [::cs/hidden-delegates])]
                         (>= (count delegates) 1))
   :empty-message "Invite people to access your account by clicking the add button below"})
