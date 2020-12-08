(ns wkok.buy2let.backend.protocol)

(defprotocol Backend
  "Implement this protocol to support your chosen backend (application server, cloud service, etc.)
   Look at the SampleBackend in the wkok.buy2let.backend.sample namespace for guidance"


  (init [_]
    "Implement this function to do any initialisation here that your chosen backend might need")


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Authentication & account related functions
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (init-auth [_ render-main-panel sign-in-panel])

  (sign-out-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for 
     signing out the user")

  (get-crud-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible 
     for pre-downloading any data from the server after the user successfully signed in.
    Example options:
     {:account :12345
      :on-success-delegates #(js/console.log %)
      :on-success-charges #(js/console.log %)
      :on-success-properties #(js/console.log %)}")

  (create-user-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) to create / register 
     a new user on the server
     Example options:
     {:user :12345
      :account :12345}")

  (get-user-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) to load an authenticated user from the server")

  (refresh-token-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) to refresh the auth token from the server")

  (send-email-verification-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) to send a user verification email")

  (accept-invitation-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) to accept a delegate invitation")

  (get-accounts-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) to load an authenticated user's accounts accessible from the server")

  (unlink-provider [_ options]
    "Implement this function to unlink an authenticated user from the authentication provider")

  (link-provider [_ provider]
    "Implement this function to link an authenticated user to the authentication provider")

  (google-sign-in-fx [_]
    "Implement this function by returning a Map of re-frame effect(s) responsible for initiating a Google sign in process")

  (facebook-sign-in-fx [_]
    "Implement this function by returning a Map of re-frame effect(s) responsible for initiating a Facebook sign in process")

  (twitter-sign-in-fx [_]
    "Implement this function by returning a Map of re-frame effect(s) responsible for initiating a Twitter sign in process")

  (github-sign-in-fx [_]
    "Implement this function by returning a Map of re-frame effect(s) responsible for initiating a Twitter sign in process")

  (save-account-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for saving an account")

  (delete-account-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for initiating a user's request to delete account")

  (delete-account-confirm-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for deleting a user's account")

  (apply-action-code-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for applting action codes on a user's account")

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Ledger
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (get-ledger-year-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for downloading the 
     last two year's ledger for all properties, from the server")

  (get-ledger-month-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for downloading this month 
     & last month's ledger for the property, from the server")

  (get-ledger-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for downloading the ledger 
                  for the collection of months from the server")


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Profile
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (save-profile-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for persisting the 
                 currently signed in user profile to the database")

  (upload-avatar-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for persisting the 
                 user's avatar to the database")


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; CRUD (Create Read Update Delete)
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (save-crud-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for persisting the 
                 crud item to the database")


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Reconcile
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (save-reconcile-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) responsible for persisting the 
                 monthly reconciliation values to the database")

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Invoices & Blobs
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  (blob-url-fx [_ options]
    "Implement this function by returning a re-frame effect responsible for generating a URL from
     where the blob represented with path can be downloaded. 
     Call on-success passing the URL of the blob
     Call on-error passing the error detail")

  (zip-invoices-fx [_ options]
    "Implement this function by returning a Map of re-frame effect(s) that returns a URL 
     from where the zipped invoices blob can be downloaded"))






