(ns wkok.buy2let.backend.demo
  (:require [re-frame.core :as rf]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.period :as period]
            [wkok.buy2let.backend.multimethods :as mm]
            [reagent.dom :as rd]
            [clojure.string :as str]
            [tick.alpha.api :as t]
            [cljc.java-time.month :as tm]))

(set! js/impl :demo)

(defmethod mm/init :demo [_])

(defmethod mm/init-auth :demo
  [{:keys [render-main-panel sign-in-panel]}]
  (if (str/includes? (-> js/window .-location .-href) "auth=demo")
    (let [auth {:uid "1234" :display-name "Demo User" :email "demo@email.com"}]
      (rf/dispatch [:get-user auth])
      (render-main-panel))
    (rd/render [sign-in-panel] (.getElementById js/document "app"))))

(defmethod mm/sign-out-fx :demo [_]
  (set! (.. js/window -location -href) "?"))

(defmethod mm/get-crud-fx :demo
  [{:keys [on-success-delegates on-success-charges on-success-properties]}]
  (let [property-charges {:agent-commission-id {:who-pays-whom :ac}
                          :levy-id {:who-pays-whom :aps}
                          :mortgage-interest-id {:who-pays-whom :mi}
                          :mortgage-repayment-id {:who-pays-whom :opb}
                          :payment-received-id {:who-pays-whom :apo}
                          :rates-taxes-id {:who-pays-whom :ops}
                          :rent-charged-id {:who-pays-whom :tpa}}]

    (on-success-charges {:rent-charged-id {:id :rent-charged-id :name "Rent charged"}
                         :agent-commission-id {:id :agent-commission-id :name "Agent commission"}
                         :levy-id {:id :levy-id :name "Levy"}
                         :mortgage-interest-id {:id :mortgage-interest-id :name "Mortgage interest"}
                         :mortgage-repayment-id {:id :mortgage-repayment-id :name "Mortgage repayment"}
                         :payment-received-id {:id :payment-received-id :name "Payment received"}
                         :rates-taxes-id {:id :rates-taxes-id :name "Rates & taxes"}})

    (on-success-delegates {:jack-id {:id :jack-id :name "Jack Hill" :email "jack@email.com" :status "ACTIVE" :roles ["viewer" "editor"]}
                           :jill-id {:id :jill-id :name "Jill Johnson" :email "jill@email.com" :status "INVITED" :send-invite true :roles ["viewer"]}
                           :john-id {:id :john-id :name "John Doe" :email "john@email.com" :status "REVOKED" :hidden true :roles ["viewer"]}})

    (on-success-properties {:property-one-id {:id :property-one-id :name "Property One"
                                              :charges property-charges}
                            :property-two-id {:id :property-two-id :name "Property Two"
                                              :charges property-charges}
                            :property-three-id {:id :property-three-id :name "Property Three" :hidden true
                                                :charges property-charges}}))
  {})

(defmethod mm/create-user-fx :demo [_] {})

(defmethod mm/get-user-fx :demo [_]
  (let [user {:id :1234 :name "Demo User" :email "demo@email.com" :accounts [:1234]}]
    (rf/dispatch [:load-claims {:claims {:roles {:viewer [:1234]
                                                 :editor [:1234]
                                                 :owner [:1234]}
                                         :email_verified true}
                                :user user}])
    (rf/dispatch [:select-account :1234])
    (rf/dispatch [:load-user user]))
  {})

(defmethod mm/get-accounts-fx :demo [_]
  (rf/dispatch [:load-account {:id :1234 :name "Demo Account"}])
  (rf/dispatch [:set-active-account :1234])
  {})

(defmethod mm/unlink-provider :demo
  [{:keys [provider]}]
  (let [p (case provider
            :google "google.com"
            :facebook "facebook.com"
            :twitter "twitter.com"
            :github "github.com")]
    (rf/dispatch [:unlink-succeeded p])))

(defmethod mm/link-provider :demo
  [{:keys [provider]}]
  (let [p (case provider
            :google "google.com"
            :facebook "facebook.com"
            :twitter "twitter.com"
            :github "github.com")]
    (rf/dispatch [::se/dialog {:heading "Not implemented"
                               :message (str "When implemented, this would redirect to " p " for authorization")
                               :buttons {:left  {:text     "Close"
                                                 :on-click #(rf/dispatch [::se/dialog])
                                                 :color :primary}}}])))

(defmethod mm/google-sign-in-fx :demo [_]
  (set! (.. js/window -location -href) "?auth=demo"))

(defmethod mm/facebook-sign-in-fx :demo [_]
  (set! (.. js/window -location -href) "?auth=demo"))

(defmethod mm/save-account-fx :demo [_]
  {})

(defmethod mm/delete-account-fx :demo
  [{:keys [on-success]}]
  (on-success))

(defmethod mm/delete-account-confirm-fx :demo [_]
  {})

(defmethod mm/get-ledger-year-fx :demo
  [{:keys [on-success]}]
  (let [today (t/today)
        this-month-year {:month (-> today t/month tm/ordinal inc str keyword)
                         :year (-> today t/year str keyword)}
        last-three-fn (fn [month-year prev]
                        (let [result (period/prev-month (:month month-year)
                                                        (:year month-year))]
                          (if (= 2 (count prev))
                            (conj prev result)
                            (recur {:month (:month result)
                                    :year (:year result)}
                                   (conj prev result)))))
        last-three (last-three-fn this-month-year [this-month-year])]
    (on-success {:property-id :property-one-id
                 :year (-> last-three first :year)
                 :ledger-months {(-> last-three first :month) {:accounting {:agent-commission {:agent-commission-id 100}
                                                                            :agent-current {:agent-commission-id -100
                                                                                            :agent-opening-balance 0
                                                                                            :levy-id -200
                                                                                            :payment-received-id -1700
                                                                                            :rent-charged-id 2000}
                                                                            :owner {:agent-opening-balance 0
                                                                                    :mortgage-repayment-id -1000
                                                                                    :payment-received-id 1700
                                                                                    :rates-taxes-id -150}
                                                                            :supplier {:levy-id 200 :rates-taxes-id 150}
                                                                            :bank-interest {:mortgage-interest-id 900}
                                                                            :bank-current {:mortgage-interest-id -900
                                                                                           :mortgage-repayment-id 1000}
                                                                            :tenant {:rent-charged-id -2000}}
                                                               :totals {:agent-commission 100
                                                                        :agent-current 0
                                                                        :owner 550
                                                                        :supplier 350
                                                                        :bank-interest 900
                                                                        :bank-current 100
                                                                        :tenant -2000}
                                                               :breakdown {:agent-commission-id {:amount 100 :invoiced false}
                                                                           :agent-opening-balance {:amount 0 :invoiced false}
                                                                           :levy-id {:amount 200 :invoiced false}
                                                                           :mortgage-interest-id {:amount 900 :invoiced false}
                                                                           :mortgage-repayment-id {:amount 1000 :invoiced false :note "This is a note"}
                                                                           :payment-received-id {:amount 1700 :invoiced false}
                                                                           :rates-taxes-id {:amount 150 :invoiced false}
                                                                           :rent-charged-id {:amount 2000 :invoiced false}}}
                                 (-> last-three second :month) {:accounting {:agent-commission {:agent-commission-id 100}
                                                                             :agent-current {:agent-commission-id -100
                                                                                             :agent-opening-balance 0
                                                                                             :levy-id -200
                                                                                             :payment-received-id -1600
                                                                                             :rent-charged-id 1900}
                                                                             :owner {:agent-opening-balance 0
                                                                                     :mortgage-repayment-id -1000
                                                                                     :payment-received-id 1600
                                                                                     :rates-taxes-id -150}
                                                                             :supplier {:levy-id 200
                                                                                        :rates-taxes-id 150}
                                                                             :bank-interest {:mortgage-interest-id 900}
                                                                             :bank-current {:mortgage-interest-id -900
                                                                                            :mortgage-repayment-id 1000}
                                                                             :tenant {:rent-charged-id -1900}}
                                                                :totals {:agent-commission 100
                                                                         :agent-current 0
                                                                         :owner 450
                                                                         :supplier 350
                                                                         :bank-interest 900
                                                                         :bank-current 100
                                                                         :tenant -1900}
                                                                :breakdown {:agent-commission-id {:amount 100
                                                                                                  :invoiced false}
                                                                            :agent-opening-balance {:amount 0
                                                                                                    :invoiced false}
                                                                            :levy-id {:amount 200
                                                                                      :invoiced false}
                                                                            :mortgage-interest-id {:amount 900
                                                                                                   :invoiced false
                                                                                                   :note "This is a note"}
                                                                            :mortgage-repayment-id {:amount 1000
                                                                                                    :invoiced false}
                                                                            :payment-received-id {:amount 1600
                                                                                                  :invoiced false}
                                                                            :rates-taxes-id {:amount 150
                                                                                             :invoiced false}
                                                                            :rent-charged-id {:invoiced false
                                                                                              :amount 1900}}}
                                 (-> last-three last :month) {:accounting {:agent-commission {:agent-commission-id 100}
                                                                           :agent-current {:agent-commission-id -100
                                                                                           :agent-opening-balance 0
                                                                                           :levy-id -200
                                                                                           :payment-received-id -1500
                                                                                           :rent-charged-id 1800}
                                                                           :owner {:agent-opening-balance 0
                                                                                   :mortgage-repayment-id -1000
                                                                                   :payment-received-id 1500
                                                                                   :rates-taxes-id -150}
                                                                           :supplier {:levy-id 200
                                                                                      :rates-taxes-id 150}
                                                                           :bank-interest {:mortgage-interest-id 900}
                                                                           :bank-current {:mortgage-interest-id -900
                                                                                          :mortgage-repayment-id 1000}
                                                                           :tenant {:rent-charged-id -1800}}
                                                              :totals {:agent-commission 100
                                                                       :agent-current 0
                                                                       :owner 350
                                                                       :supplier 350
                                                                       :bank-interest 900
                                                                       :bank-current 100
                                                                       :tenant -1800}
                                                              :breakdown {:agent-commission-id {:amount 100
                                                                                                :invoiced false}
                                                                          :agent-opening-balance {:amount 0
                                                                                                  :invoiced false}
                                                                          :levy-id {:amount 200
                                                                                    :invoiced false
                                                                                    :note "This is a note"}
                                                                          :mortgage-interest-id {:amount 900
                                                                                                 :invoiced false}
                                                                          :mortgage-repayment-id {:amount 1000
                                                                                                  :invoiced false}
                                                                          :payment-received-id {:amount 1500
                                                                                                :invoiced false}
                                                                          :rates-taxes-id {:amount 150
                                                                                           :invoiced false}
                                                                          :rent-charged-id {:invoiced false
                                                                                            :amount 1800}}}}})

    (on-success {:property-id :property-two-id
                 :year (-> last-three first :year)
                 :ledger-months {(-> last-three first :month) {:accounting {:agent-commission {:agent-commission-id 100}
                                                                            :agent-current {:agent-commission-id -100
                                                                                            :agent-opening-balance 0
                                                                                            :levy-id -200
                                                                                            :payment-received-id -1000
                                                                                            :rent-charged-id 1300}
                                                                            :owner {:agent-opening-balance 0
                                                                                    :mortgage-repayment-id -1000
                                                                                    :payment-received-id 1000
                                                                                    :rates-taxes-id -150}
                                                                            :supplier {:levy-id 200
                                                                                       :rates-taxes-id 150}
                                                                            :bank-interest {:mortgage-interest-id 900}
                                                                            :bank-current {:mortgage-interest-id -900
                                                                                           :mortgage-repayment-id 1000}
                                                                            :tenant {:rent-charged-id -1300}}
                                                               :totals {:agent-commission 100
                                                                        :agent-current 0
                                                                        :owner -150
                                                                        :supplier 350
                                                                        :bank-interest 900
                                                                        :bank-current 100
                                                                        :tenant -1300}
                                                               :breakdown {:agent-commission-id {:amount 100
                                                                                                 :invoiced false}
                                                                           :agent-opening-balance {:amount 0
                                                                                                   :invoiced false}
                                                                           :levy-id {:amount 200
                                                                                     :invoiced false}
                                                                           :mortgage-interest-id {:amount 900
                                                                                                  :invoiced false}
                                                                           :mortgage-repayment-id {:amount 1000
                                                                                                   :invoiced false}
                                                                           :payment-received-id {:amount 1000
                                                                                                 :invoiced false
                                                                                                 :note "This is a note"}
                                                                           :rates-taxes-id {:amount 150
                                                                                            :invoiced false}
                                                                           :rent-charged-id {:amount 1300
                                                                                             :invoiced false}}}
                                 (-> last-three second :month) {:accounting {:agent-commission {:agent-commission-id 100}
                                                                             :agent-current {:agent-commission-id -100
                                                                                             :agent-opening-balance 0
                                                                                             :levy-id -200
                                                                                             :payment-received-id -1000
                                                                                             :rent-charged-id 1300}
                                                                             :owner {:agent-opening-balance 0
                                                                                     :mortgage-repayment-id -1000
                                                                                     :payment-received-id 1000
                                                                                     :rates-taxes-id -150}
                                                                             :supplier {:levy-id 200
                                                                                        :rates-taxes-id 150}
                                                                             :bank-interest {:mortgage-interest-id 900}
                                                                             :bank-current {:mortgage-interest-id -900
                                                                                            :mortgage-repayment-id 1000}
                                                                             :tenant {:rent-charged-id -1300}}
                                                                :totals {:agent-commission 100
                                                                         :agent-current 0
                                                                         :owner -150
                                                                         :supplier 350
                                                                         :bank-interest 900
                                                                         :bank-current 100
                                                                         :tenant -1300}
                                                                :breakdown {:agent-commission-id {:amount 100
                                                                                                  :invoiced false}
                                                                            :agent-opening-balance {:amount 0
                                                                                                    :invoiced false}
                                                                            :levy-id {:amount 200
                                                                                      :invoiced false}
                                                                            :mortgage-interest-id {:amount 900
                                                                                                   :invoiced false}
                                                                            :mortgage-repayment-id {:amount 1000
                                                                                                    :invoiced false}
                                                                            :payment-received-id {:amount 1000
                                                                                                  :invoiced false}
                                                                            :rates-taxes-id {:amount 150
                                                                                             :invoiced false
                                                                                             :note "This is a note"}
                                                                            :rent-charged-id {:invoiced false
                                                                                              :amount 1300}}}
                                 (-> last-three last :month) {:accounting {:agent-commission {:agent-commission-id 100}
                                                                           :agent-current {:agent-commission-id -100
                                                                                           :agent-opening-balance 0
                                                                                           :levy-id -200
                                                                                           :payment-received-id -1100
                                                                                           :rent-charged-id 1400}
                                                                           :owner {:agent-opening-balance 0
                                                                                   :mortgage-repayment-id -1000
                                                                                   :payment-received-id 1100
                                                                                   :rates-taxes-id -150}
                                                                           :supplier {:levy-id 200
                                                                                      :rates-taxes-id 150}
                                                                           :bank-interest {:mortgage-interest-id 900}
                                                                           :bank-current {:mortgage-interest-id -900
                                                                                          :mortgage-repayment-id 1000}
                                                                           :tenant {:rent-charged-id -1400}}
                                                              :totals {:agent-commission 100
                                                                       :agent-current 0
                                                                       :owner -50
                                                                       :supplier 350
                                                                       :bank-interest 900
                                                                       :bank-current 100
                                                                       :tenant -1400}
                                                              :breakdown {:agent-commission-id {:amount 100
                                                                                                :invoiced false}
                                                                          :agent-opening-balance {:amount 0
                                                                                                  :invoiced false}
                                                                          :levy-id {:amount 200
                                                                                    :invoiced false}
                                                                          :mortgage-interest-id {:amount 900
                                                                                                 :invoiced false}
                                                                          :mortgage-repayment-id {:amount 1000
                                                                                                  :invoiced false}
                                                                          :payment-received-id {:amount 1100
                                                                                                :invoiced false}
                                                                          :rates-taxes-id {:amount 150
                                                                                           :invoiced false}
                                                                          :rent-charged-id {:invoiced false
                                                                                            :amount 1400
                                                                                            :note "This is a note"}}}}}))
  {})

(defmethod mm/get-ledger-month-fx :demo [_]
  {})

(defmethod mm/get-ledger-fx :demo [_]
  {})

(defmethod mm/save-crud-fx :demo [_]
  {})

(defmethod mm/save-profile-fx :demo [_]
  {})

(defmethod mm/upload-avatar-fx :demo [_]
  (rf/dispatch [::se/splash false])
  {})

(defmethod mm/save-reconcile-fx :demo [_]
  {})

(defmethod mm/blob-url-fx :demo [_]
  {})

(defmethod mm/zip-invoices-fx :demo [_]
  (rf/dispatch [::se/show-progress false])
  {})




