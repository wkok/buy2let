(ns wkok.buy2let.backend.protocol)

(defprotocol Backend
  "Implement this protocol to support your chosen backend (application server, cloud service, etc.)
   Look at the SampleBackend in the wkok.buy2let.backend.sample namespace for guidance"


  (init [_]
    "Implement this function to do any initialisation here that your chosen backend might need")


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Authentication & account related functions
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (init-auth [_ render-fn]
    "Implement this function to initiate the authentication / sign-in process")

  (sign-out-fx [_]
    "Implement this function by returning a Map of re-frame effect(s) responsible for signing out the user")

  (get-crud-fx [_ account]
    "Implement this function by returning a Map of re-frame effect(s) responsible for pre-downloading any data from the server
     after the user successfully signed in.")

  (create-user-fx [_ user account]
    "Implement this function by returning a Map of re-frame effect(s) to create / register a new user on the server")

  (get-user-fx [_ auth]
    "Implement this function by returning a Map of re-frame effect(s) to load an authenticated user from the server")

  (get-account-fx [_ user]
    "Implement this function by returning a Map of re-frame effect(s) to load an authenticated user's account from the server")

  (unlink-provider [_ provider]
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

  (delete-account-fx [_ user-id account-id on-success on-error]
    "Implement this function by returning a Map of re-frame effect(s) responsible for deleting a user's account")


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Ledger
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (get-ledger-year-fx [_ properties account-id this-year last-year]
    "Implement this function by returning a Map of re-frame effect(s) responsible for downloading the 
     last two year's ledger for all properties, from the server")

  (get-ledger-month-fx [_ property account-id this-year this-month prev-year prev-month]
    "Implement this function by returning a Map of re-frame effect(s) responsible for downloading this month 
     & last month's ledger for the property, from the server")

  (get-ledger-fx [_ property account-id months]
    "Implement this function by returning a Map of re-frame effect(s) responsible for downloading the ledger 
                  for the collection of months from the server")


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; CRUD (Create Read Update Delete)
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (save-crud-fx [_ account-id crud-type id item on-failure]
    "Implement this function by returning a Map of re-frame effect(s) responsible for persisting the 
                 crud item to the database")


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Reconcile
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (save-reconcile-fx [_ account-id property-id year month charges-this-month]
    "Implement this function by returning a Map of re-frame effect(s) responsible for persisting the 
                 monthly reconciliation values to the database")

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Invoices & Blobs
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  (blob-url-fx [_ path on-success on-error]
    "Implement this function by returning a re-frame effect responsible for generating a URL from
     where the blob represented with path can be downloaded. 
     Call on-success passing the URL of the blob
     Call on-error passing the error detail")

  (zip-invoices-fx [_ account-id uuid file-name invoice-paths on-success on-error]
    "Implement this function by returning a Map of re-frame effect(s) that returns a URL 
     from where the zipped invoices blob can be downloaded"))






