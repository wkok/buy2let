(ns wkok.buy2let.crud.types
  (:require
   [clojure.string :as s]
   [re-frame.core :as rf]
   [wkok.buy2let.account.subs :as as]
   [wkok.buy2let.ui.react.component.icon :as icon]
   [wkok.buy2let.ui.react.component.invoice :as inv]
   [wkok.buy2let.ui.react.component.property :as prop]
   [wkok.buy2let.crud.events :as ce]
   [wkok.buy2let.crud.subs :as cs]
   [wkok.buy2let.reconcile.subs :as rs]
   [wkok.buy2let.shared :as shared]
   [wkok.buy2let.site.events :as se]
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
                 {:key :currency :type :select-currency}
                 {:key :purchase-price :type :number :label "Purchase Price"}
                 {:key :cash-invested :type :number :label "Cash Invested"}]
   :validate-fn #(merge (validate-name %) (validate-who-pays %) (validate-currency %))
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/properties/add") :icon icon/add-icon
                                 :title "Add"}}}
   :extra prop/property-charges
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
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/charges/add") :icon icon/add-icon
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
        (assoc :attached (not (nil? (:attachment invoice)))))))

(def invoice
  {:type        :invoices
   :subs        ::cs/all-invoices
   :fields      [{:key :name :type :text :default true}
                 {:key :invoice :type :attachment}]
   :sub-header-fn get-invoices-charge-name
   :validate-fn validate-name
   :uri-path-fn get-invoices-uri-path
   :actions     {:list {:left-1 {:fn #(rf/dispatch [::ce/invoice-add])
                                 :icon icon/add-icon
                                 :title "Add"}}}
   :singular "invoice"
   :show-show-hidden? #(let [invoices @(rf/subscribe [::cs/hidden-invoices])]
                         (>= (count invoices) 1))
   :calculated-fn #(-> % upload-invoice-attachment)
   :secondary-action inv/attachment-button})

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
   :validate-fn #(merge (validate-name %) (validate-email %))
   :actions     {:list {:left-1 {:fn   #(js/window.location.assign "#/delegates/add") :icon icon/add-icon
                                 :title "Add"}}}
   :hidden-label "revoked"
   :singular "delegate"
   :show-show-hidden? #(let [delegates @(rf/subscribe [::cs/hidden-delegates])]
                         (>= (count delegates) 1))
   :empty-message "Invite people to access your account by clicking the add button below"})
