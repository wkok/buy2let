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
              :ledger {:property-one-id {:2020 {:8 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance nil, :levy-id -200, :payment-received-id -1700, :rent-charged-id 2000}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1700, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -2000}}, :totals {:agent-commission 100, :agent-current 0, :owner 550, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -2000}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount nil, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false :note "This is a note"}, :payment-received-id {:amount 1700, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:amount 2000, :invoiced false}}}
                                                :7 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance nil, :levy-id -200, :payment-received-id -1600, :rent-charged-id 1900}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1600, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1900}}, :totals {:agent-commission 100, :agent-current 0, :owner 450, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1900}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount nil, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false :note "This is a note"}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1600, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:invoiced false, :amount 1900}}}
                                                :6 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance nil, :levy-id -200, :payment-received-id -1500, :rent-charged-id 1800}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1500, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1800}}, :totals {:agent-commission 100, :agent-current 0, :owner 350, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1800}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount nil, :invoiced false}, :levy-id {:amount 200, :invoiced false :note "This is a note"}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1500, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:invoiced false, :amount 1800}}}}}
                       :property-two-id {:2020 {:8 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance 0, :levy-id -200, :payment-received-id -1000, :rent-charged-id 1300}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1000, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1300}}, :totals {:agent-commission 100, :agent-current 0, :owner -150, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1300}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount 0, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1000, :invoiced false :note "This is a note"}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:amount 1300, :invoiced false}}}
                                                :7 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance 0, :levy-id -200, :payment-received-id -1000, :rent-charged-id 1300}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1000, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1300}}, :totals {:agent-commission 100, :agent-current 0, :owner -150, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1300}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount 0, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1000, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false :note "This is a note"}, :rent-charged-id {:invoiced false, :amount 1300}}}
                                                :6 {:accounting {:agent-commission {:agent-commission-id 100}, :agent-current {:agent-commission-id -100, :agent-opening-balance nil, :levy-id -200, :payment-received-id -1100, :rent-charged-id 1400}, :owner {:agent-opening-balance 0, :mortgage-repayment-id -1000, :payment-received-id 1100, :rates-taxes-id -150}, :supplier {:levy-id 200, :rates-taxes-id 150}, :bank-interest {:mortgage-interest-id 900}, :bank-current {:mortgage-interest-id -900, :mortgage-repayment-id 1000}, :tenant {:rent-charged-id -1400}}, :totals {:agent-commission 100, :agent-current 0, :owner -50, :supplier 350, :bank-interest 900, :bank-current 100, :tenant -1400}, :breakdown {:agent-commission-id {:amount 100, :invoiced false}, :agent-opening-balance {:amount nil, :invoiced false}, :levy-id {:amount 200, :invoiced false}, :mortgage-interest-id {:amount 900, :invoiced false}, :mortgage-repayment-id {:amount 1000, :invoiced false}, :payment-received-id {:amount 1100, :invoiced false}, :rates-taxes-id {:amount 150, :invoiced false}, :rent-charged-id {:invoiced false, :amount 1400 :note "This is a note"}}}}}}})

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

  (get-crud-fx [_ _]
    (rf/dispatch [::se/show-progress false])
    {})

  (create-user-fx [_ _]
    {})

  (get-user-fx
    [_ {:keys [auth _]}]
    (rf/dispatch [:load-user auth {}])
    {})

  (get-account-fx [_ _]
    (rf/dispatch [:load-account {"id" "1234", "name" "Demo Account"}])
    {})

  (unlink-provider
    [_ {:keys [provider _ _]}]
    (let [p (case provider
              :google "google.com"
              :facebook "facebook.com"
              :twitter "twitter.com"
              :github "github.com")]
      (rf/dispatch [:unlink-succeeded p])))

  (link-provider [_ provider]
    (let [p (case provider
              :google "google.com"
              :facebook "facebook.com"
              :twitter "twitter.com"
              :github "github.com")]
      (rf/dispatch [::se/dialog {:heading "Not implemented"
                                 :message (str "When implemented, this would redirect to " p " for authorization")}])))

  (google-sign-in-fx [_]
    (do-sign-in))

  (facebook-sign-in-fx [_]
    (do-sign-in))

  (twitter-sign-in-fx [_]
    (do-sign-in))

  (github-sign-in-fx [_]
    (do-sign-in))

  (delete-account-fx
    [_ {:keys [on-success]}]
    (on-success))

  (get-ledger-year-fx [_ _]
    {})

  (get-ledger-month-fx [_ _]
    {})

  (get-ledger-fx [_ _]
    {})

  (save-crud-fx [_ _]
    {})

  (save-reconcile-fx [_ _]
    {})

  (blob-url-fx [_ _]
    {})

  (zip-invoices-fx [_ _]
    (rf/dispatch [::se/show-progress false])
    {}))


