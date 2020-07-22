(ns wkok.buy2let.backend.demo
  (:require [re-frame.core :as rf]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.backend.protocol :as protocol]
            [clojure.string :as str]))


(defn do-sign-in []
  (set! (.. js/window -location -href) "?auth=demo"))


(deftype DemoBackend []
  protocol/Backend

  (init [_])

  (init-auth [_ render-fn]
    (if (str/includes? (-> js/window .-location .-href) "auth=demo")
      (let [auth {:uid "1234" :display-name "Demo User" :email "demo@email.com"}]
        (rf/dispatch [:set-backend-user auth])
        (render-fn auth))
      (render-fn nil)))

  (sign-out-fx [_]
    (set! (.. js/window -location -href) "?")
    {})

  (get-crud-fx [_ account]
    (rf/dispatch [:load-properties])
    {})

  (create-user-fx [_ user account]
    {})

  (get-user-fx [_ auth]
    (rf/dispatch [:load-user auth {:data {}}])
    {})

  (get-account-fx [_ user]
    (rf/dispatch [:load-account {:data {"id" "1234", "name" "Demo Account"}, :id "1234"}])
    {})

  (unlink-provider [_ provider]
    (rf/dispatch [::se/dialog {:heading "Not implemented"
                               :message (str "Please implement backend effect to unlink: " provider)}]))

  (link-provider [_ provider]
    (rf/dispatch [::se/dialog {:heading "Not implemented"
                               :message (str "Please implement backend effect to link: " provider)}]))

  (google-sign-in-fx [_]
    (do-sign-in))

  (facebook-sign-in-fx [_]
    (do-sign-in))

  (twitter-sign-in-fx [_]
    (do-sign-in))

  (github-sign-in-fx [_]
    (do-sign-in))

  (get-ledger-year-fx [_ properties account-id this-year last-year]
    {})

  (get-ledger-month-fx [_ property account-id this-year this-month prev-year prev-month]
    {})

  (get-ledger-fx [_ property account-id months]
    {})

  (save-crud-fx [_ account-id crud-type id item on-failure]
    {})

  (save-reconcile-fx [_ account-id property-id year month charges-this-month]
    {})

  (blob-url-fx [_ path on-success on-error]
    {})

  (zip-invoices-fx [_ account-id uuid file-name invoice-paths on-success on-error]
    (rf/dispatch [::se/show-progress false])
    {}))


