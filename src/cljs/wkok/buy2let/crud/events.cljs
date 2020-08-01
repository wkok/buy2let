(ns wkok.buy2let.crud.events
  (:require
   [re-frame.core :as rf]
   [clojure.string :as s]
   [wkok.buy2let.shared :as shared]
   [wkok.buy2let.backend.protocol :as bp]
   [wkok.buy2let.backend.impl :as impl]
   [wkok.buy2let.site.events :as se]
   [wkok.buy2let.backend.subs :as fs]))



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

(rf/reg-event-db
  ::add-crud
  (fn [db [_ type]]
    (-> (assoc-in db [:site :active-panel] (panel ::add-crud type))
        (assoc-in [:site :heading] (heading type)))))


(rf/reg-event-db
  ::edit-crud
  (fn [db [_ id type]]
    (-> (assoc-in db [:form :old (:type type)] (get ((:type type) db) id))
        (assoc-in [:site :active-page] (:type type))
        (assoc-in [:site :active-panel] (panel ::edit-crud type))
        (assoc-in [:site :heading] (heading type)))))

(rf/reg-event-db
  ::list-crud
  (fn [db [_ type]]
    (-> (dissoc db :form)
        (assoc-in [:site :active-page] (:type type))
        (assoc-in [:site :active-panel] (panel ::list-crud type))
        (assoc-in [:site :heading] (heading type)))))

(rf/reg-event-fx
 ::save-crud
 [(rf/inject-cofx ::shared/gen-id)]
 (fn [cofx [_ type item]]
   (let [id (or (:id item) (:id cofx))
         item (assoc item :id id)
         account-id @(rf/subscribe [::fs/account])]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (merge {:db            (assoc-in (:db cofx) [(:type type) id] item)}
            (bp/save-crud-fx impl/backend 
                             {:account-id account-id 
                              :crud-type type 
                              :id id 
                              :item item
                              :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))

(rf/reg-event-db
  ::crud-set-show-hidden
  (fn [db [_ hidden]]
    (assoc-in db [:crud :show-hidden] hidden)))


