(ns wkok.buy2let.crud.events
  (:require
   [re-frame.core :as rf]
   [clojure.string :as s]
   [wkok.buy2let.shared :as shared]
   [wkok.buy2let.backend.multimethods :as mm]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.account.subs :as as]))



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
 (fn [db [_ type options]]
   (case (:type type)
       :properties (add-property db)
       :invoices (-> (assoc-in db [:reconcile :charge-id] (:charge-id options))
                     (assoc-add-crud-keys type))
       (assoc-add-crud-keys db type))))

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
  (fn [db [_ id type options]]
    (case (:type type)
      :invoices (-> (assoc-reconcile-keys db options)
                    (assoc-edit-crud-keys id type))
      (assoc-edit-crud-keys db id type))))

(defn assoc-list-crud-keys
  [db type]
  (-> (dissoc db :form)
      (assoc-in [:site :active-page] (:type type))
      (assoc-in [:site :active-panel] (panel ::list-crud type))
      (assoc-in [:site :heading] (heading type))))

(rf/reg-event-db
  ::list-crud
  (fn [db [_ type options]]
    (case (:type type)
      :invoices (-> (assoc-reconcile-keys db options)
                    (assoc-list-crud-keys type))
      (assoc-list-crud-keys db type))))

(rf/reg-event-fx
 ::upload-attachments
 (fn [cofx [_ blobs]]
   (merge {:db (:db cofx)}
          (mm/upload-attachments-fx {:blobs blobs
                                    :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])}))))

(rf/reg-event-fx
 ::save-crud
 [(rf/inject-cofx ::shared/gen-id)]
 (fn [cofx [_ type item]]
   (let [id (or (:id item) (:id cofx))
         calculated-fn (or (:calculated-fn type) identity)
         key-fields (when-let [key-fields-fn (:key-fields-fn type)]
                      (key-fields-fn))
         item (merge (-> (assoc item :id id) calculated-fn)
                     key-fields)
         account-id @(rf/subscribe [::as/account])]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (merge {:db            (assoc-in (:db cofx) [(:type type) id] item)}
            (mm/save-crud-fx {:account-id account-id
                              :crud-type type
                              :id id
                              :item item
                              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))


(rf/reg-event-db
  ::crud-set-show-hidden
  (fn [db [_ hidden]]
    (assoc-in db [:crud :show-hidden] hidden)))
