(ns wkok.buy2let.crud.events
  (:require
   [re-frame.core :as rf]
   [clojure.string :as s]
   [wkok.buy2let.shared :as shared]
   [wkok.buy2let.backend.multimethods :as mm]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.db.default :as ddb]
   [wkok.buy2let.security :as sec]
   [goog.crypt.base64 :as b64]))



(defn heading [type]
  (-> (:type type)
      name
      s/capitalize))


(defn panel [event type]
  (-> (:type type)
      name
      (str (case event
             ::edit-crud "-edit"
             ::add-crud "-edit"
             ::save-crud "-list"
             ::cancel-crud "-list"
             ::delete-crud "-list"
             ::list-crud "-list"))
      keyword))

(defn subscription-allows? [current-properties subscribed-properties]
  (>= subscribed-properties (inc current-properties)))

(defn add-property [db]
  (let [account-id (-> db :security :account)
        accounts (-> db :security :accounts)
        account (when account-id (account-id accounts))
        current-properties (-> db :properties count)
        subscribed-properties (get-in account [:subscription :properties] 1)
        property-s (if (> subscribed-properties 1)
                     " properties" " property")]
    (when (not (get-in db [:site :location :currency]))
      (rf/dispatch [::se/detect-location]))
    (if (subscription-allows? current-properties subscribed-properties)
      (-> (dissoc db :wizard)
          (assoc-in [:site :active-page] :wizard)
          (assoc-in [:site :heading] (str "Add property")))
      (do (rf/dispatch [::se/dialog {:heading "Upgrade subscription"
                                     :message (str "Your current subscription allows " subscribed-properties " " property-s
                                                   ". Please upgrade your subscription to add more.")
                                     :buttons   {:middle {:text     "Upgrade"
                                                          :on-click #(js/window.location.assign "#/subscription")
                                                          :color :secondary}
                                                 :right {:text     "Not now"
                                                         :on-click #(rf/dispatch [::se/dialog])}}
                                     :closeable false}])
          (js/window.history.back)
          db))))

(defn assoc-add-crud-keys
  [db type]
  (-> (assoc-in db [:site :active-panel] (panel ::add-crud type))
      (assoc-in [:site :heading] (str "Add " (:singular type)))))

(rf/reg-event-db
 ::add-crud
 (fn [db [_ role type options]]
   (sec/with-authorisation role db
     #(case (:type type)
        :properties (add-property db)
        :invoices (-> (assoc-in db [:reconcile :charge-id] (:charge-id (ddb/calc-options-db db options)))
                      (assoc-add-crud-keys type))
        (assoc-add-crud-keys db type)))))

(defn assoc-reconcile-keys
  [db options]
  (-> (assoc-in db [:site :active-property] (:property-id options))
      (assoc :reconcile (select-keys options [:year :month :charge-id]))))

(defn assoc-edit-crud-keys
  [db id type]
  (-> (assoc-in db [:form :old (:type type)] (get (get-in db [(:type type)]) id))
      (assoc-in [:site :active-page] (:type type))
      (assoc-in [:site :active-panel] (panel ::edit-crud type))
      (assoc-in [:site :heading] (str "Edit " (:singular type)))))

(rf/reg-event-db
 ::edit-crud
 (fn [db [_ role id type options]]
   (sec/with-authorisation role db
     #(case (:type type)
        :invoices (-> (assoc-reconcile-keys db (ddb/calc-options-db db options))
                      (assoc-edit-crud-keys id type))
        (assoc-edit-crud-keys db id type)))))

(defn assoc-list-crud-keys
  [db type]
  (-> (dissoc db :form)
      (assoc-in [:site :active-page] (:type type))
      (assoc-in [:site :active-panel] (panel ::list-crud type))
      (assoc-in [:site :heading] (heading type))))

(rf/reg-event-db
 ::list-crud
 (fn [db [_ role type options]]
   (sec/with-authorisation role db
     #(case (:type type)
        :invoices (-> (assoc-reconcile-keys db (ddb/calc-options-db db options))
                      (assoc-list-crud-keys type))
        (assoc-list-crud-keys db type)))))

(rf/reg-event-fx
 ::upload-attachments
 (fn [cofx [_ blobs]]
   (merge {:db (:db cofx)}
          (mm/upload-attachments-fx {:blobs blobs
                                    :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])}))))

(defn get-key-fields
  [db type]
  (case (:type type)
    :invoices {:property-id (get-in db [:site :active-property])
               :charge-id (get-in db [:reconcile :charge-id])
               :year (get-in db [:reconcile :year])
               :month (get-in db [:reconcile :month])}
    {}))

(defn calc-status [item]
  (assoc item :status
         (if (:hidden item)
           "REVOKED"
           (if (:send-invite item)
             "INVITED"
             "ACTIVE"))))

(defn create-invite
  [item db]
  (if (:send-invite item)
    (assoc item :invitation
           (let [accounts (get-in db [:security :accounts])
                 account-id (get-in db [:security :account])
                 local-user (get-in db [:security :user])]
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

(defn get-calculated-fn
  [db type]
  (case (:type type)
    :delegates #(-> % calc-status (create-invite db))
    (:calculated-fn type)))

(rf/reg-event-fx
 ::save-crud
 [(rf/inject-cofx ::shared/gen-id)]
 (fn [{:keys [id db]} [_ type item]]
   (let [id (or (:id item) id)
         calculated-fn (or (get-calculated-fn db type) identity)
         key-fields (get-key-fields db type)
         item (merge (-> (assoc item :id id) calculated-fn)
                     key-fields)
         account-id (get-in db [:security :account])]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (merge {:db            (assoc-in db [(:type type) id] item)}
            (mm/save-crud-fx {:account-id account-id
                              :crud-type type
                              :id id
                              :item item
                              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))


(rf/reg-event-db
  ::crud-set-show-hidden
  (fn [db [_ hidden]]
    (assoc-in db [:crud :show-hidden] hidden)))

(rf/reg-event-fx
 ::invoice-add
 (fn [{:keys [db]} [_ _]]
   (let [uri-path (str "#/reconcile/" (name (get-in db [:site :active-property]))
                       "/" (name (get-in db [:reconcile :month]))
                       "/" (name (get-in db [:reconcile :year]))
                       "/" (name (get-in db [:reconcile :charge-id]))
                       "/invoices")]
     (js/window.location.assign (str uri-path "/add")))))
