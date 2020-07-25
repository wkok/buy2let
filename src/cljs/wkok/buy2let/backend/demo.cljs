(ns wkok.buy2let.backend.demo
  (:require [re-frame.core :as rf]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.backend.protocol :as protocol]
            [clojure.string :as str]))


(defn do-sign-in []
  (set! (.. js/window -location -href) "?auth=demo"))

(def property-charges {:agent-commission-id {:who-pays-whom "ac"}
                       :levy-id {:who-pays-whom "aps"}
                       :mortgage-interest-id {:who-pays-whom "mi"}
                       :mortgage-repayment-id {:who-pays-whom "opb"}
                       :payment-received-id {:who-pays-whom "apo"}
                       :rates-taxes-id {:who-pays-whom "ops"}
                       :rent-charged-id {:who-pays-whom "tpa"}})

(def demo-db {:charges {:rent-charged-id {:id :rent-charged-id :name "Rent charged"}
                        :agent-commission-id {:id :agent-commission-id :name "Agent commission"}
                        :levy-id {:id :levy-id :name "Levy"}
                        :mortgage-interest-id {:id :mortgage-interest-id :name "Mortgage interest"}
                        :mortgage-repayment-id {:id :mortgage-repayment-id :name "Mortgage repayment"}
                        :payment-received-id {:id :payment-received-id :name "Payment received"}
                        :rates-taxes-id {:id :rates-taxes-id :name "Rates & taxes"}}
              :properties {:property-one-id {:id :property-one-id :name "Property One"
                                             :charges property-charges}
                           :property-two-id {:id :property-two-id :name "Property Two"
                                             :charges property-charges}
                           :property-three-id {:id :property-three-id :name "Property Three" :hidden true}}
              :users {:jack-id {:id :jack-id :name "Jack" :email "jack@email.com"}
                      :jill-id {:id :jill-id :name "Jill" :email "jill@email.com"}}
              :ledger {:property-one-id {:2020 {:7 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance nil, :levy-id -200, :payment-received-id -1700, :rent-charged-id 2000}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1700, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -2000}}, :totals {:agent-commission 100, :agent-current 0, :owner 550, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -2000}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount nil, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false :note "This is a note"}, :payment-received-id {:amount 1700, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:amount 2000, :invoiced false}}}
                                                :6 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance nil, :levy-id -200, :payment-received-id -1600, :rent-charged-id 1900}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1600, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1900}}, :totals {:agent-commission 100, :agent-current 0, :owner 450, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1900}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount nil, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false :note "This is a note"}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1600, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:invoiced false, :amount 1900}}}
                                                :5 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance nil, :levy-id -200, :payment-received-id -1500, :rent-charged-id 1800}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1500, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1800}}, :totals {:agent-commission 100, :agent-current 0, :owner 350, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1800}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount nil, :invoiced false}, :levy-id {:amount 200, :invoiced false :note "This is a note"}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1500, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:invoiced false, :amount 1800}}}}}
                       :property-two-id {:2020 {:7 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance 0, :levy-id -200, :payment-received-id -1000, :rent-charged-id 1300}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1000, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1300}}, :totals {:agent-commission 100, :agent-current 0, :owner -150, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1300}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount 0, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1000, :invoiced false :note "This is a note"}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:amount 1300, :invoiced false}}}
                                                :6 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance 0, :levy-id -200, :payment-received-id -1000, :rent-charged-id 1300}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1000, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1300}}, :totals {:agent-commission 100, :agent-current 0, :owner -150, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1300}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount 0, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1000, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false :note "This is a note"}, :rent-charged-id {:invoiced false, :amount 1300}}}
                                                :5 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance nil, :levy-id -200, :payment-received-id -1100, :rent-charged-id 1400}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1100, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1400}}, :totals {:agent-commission 100, :agent-current 0, :owner -50, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1400}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount nil, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1100, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:invoiced false, :amount 1400 :note "This is a note"}}}}}}})

(deftype DemoBackend []
  protocol/Backend

  (init [_])

  (init-auth [_ render-fn]
    (if (str/includes? (-> js/window .-location .-href) "auth=demo")
      (let [auth {:uid "1234" :display-name "Demo User" :email "demo@email.com"}]
        (rf/dispatch [:set-backend-user auth])
        (rf/dispatch [:initialize-db demo-db])
        (render-fn auth))
      (render-fn nil)))

  (sign-out-fx [_]
    (set! (.. js/window -location -href) "?")
    {})

  (get-crud-fx [_ account]
    (rf/dispatch [::se/show-progress false])
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


