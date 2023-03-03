(ns wkok.buy2let.security
  (:require [re-frame.core :as rf]))

(defn has-role [role claims account-id]
  (some #{account-id} (get-in claims [:roles role])))

(defn with-authorisation
  [role db f]
  (let [claims (get-in db [:security :claims])
        account-id (get-in db [:security :account])]
    (if (has-role role claims account-id)
      (f)
      (rf/dispatch [::unauthorised]))))

(rf/reg-event-fx
 ::unauthorised
 (fn [_ _]
   {}))
