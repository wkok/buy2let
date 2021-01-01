(ns wkok.buy2let.backend.multimethods)

(def dispatcher (fn [] (keyword js/impl)))

(defmulti app-name dispatcher)

(defmulti init dispatcher)

(defmulti init-auth dispatcher)

(defmulti packages dispatcher)

(defmulti terms-of-service dispatcher)

(defmulti privacy-policy dispatcher)

(defmulti sign-out-fx dispatcher)

(defmulti get-crud-fx dispatcher)

(defmulti create-user-fx dispatcher)

(defmulti get-user-fx dispatcher)

(defmulti get-accounts-fx dispatcher)

(defmulti unlink-provider dispatcher)

(defmulti link-provider dispatcher)

(defmulti google-sign-in-fx dispatcher)

(defmulti facebook-sign-in-fx dispatcher)

(defmulti save-account-fx dispatcher)

(defmulti accept-invitation-fx dispatcher)

(defmulti refresh-token-fx dispatcher)

(defmulti send-email-verification-fx dispatcher)

(defmulti apply-action-code-fx dispatcher)

(defmulti delete-account-fx dispatcher)

(defmulti delete-account-confirm-fx dispatcher)

(defmulti get-ledger-year-fx dispatcher)

(defmulti get-ledger-month-fx dispatcher)

(defmulti get-ledger-fx dispatcher)

(defmulti save-crud-fx dispatcher)

(defmulti save-profile-fx dispatcher)

(defmulti upload-avatar-fx dispatcher)

(defmulti save-reconcile-fx dispatcher)

(defmulti blob-url-fx dispatcher)

(defmulti zip-invoices-fx dispatcher)

