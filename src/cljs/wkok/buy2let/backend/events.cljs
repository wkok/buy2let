(ns wkok.buy2let.backend.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.account.events :as ae]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.account.views :as av]
            [wkok.buy2let.backend.effects]
            [wkok.buy2let.backend.multimethods :as mm]
            [wkok.buy2let.spec :as spec]
            [goog.crypt.base64 :as b64]
            [cemerick.url :as url]
            [clojure.string :as str]
            [cljs.reader]))

(rf/reg-event-fx
 ::sign-in
 (fn [_ [_ provider]]
   (case provider
     :google (mm/google-sign-in-fx {})
     :facebook (mm/facebook-sign-in-fx {}))))

(rf/reg-event-db
 :unlink-succeeded
 (fn [db [_ provider]]
   (-> (assoc-in db [:security :auth :provider-data] (->> (get-in db [:security :auth])
                                                          :provider-data
                                                          js->clj
                                                          (remove #(= (get % "providerId") provider))))
       (assoc-in [:site :dialog] {:heading "Unlinked"
                                  :message (str "Successfully unlinked " provider)
                                  :buttons {:middle {:text     "Close"}}}))))

(rf/reg-event-fx
 ::link
 (fn [_ [_ provider]]
   {:link-provider provider}))

(rf/reg-event-fx
 ::unlink
 (fn [_ [_ provider]]
   {:unlink-provider provider}))

(defn accepting-invitation? []
  (when-let [invitation (-> js/window .-location .-href
                            url/url
                            (get-in [:query "invitation"]))]
    (-> invitation
        b64/decodeString
        cljs.reader/read-string)))

(defn verifying-email? []
  (-> js/window .-location .-href
      url/url
      (get-in [:query "oobCode"])))

(defn delete-confirmation? []
  (when-let [confirmation (-> js/window .-location .-href
                              url/url
                              (get-in [:query "delete-confirmation"]))]
    (-> confirmation
        b64/decodeString
        cljs.reader/read-string)))

(defn registered? [user]
  (not (nil? user)))

(rf/reg-event-fx
 ::verify-email
 (fn [_ [_ {:keys [action-code on-success]}]]
   (mm/apply-action-code-fx {:action-code action-code
                             :on-success on-success
                             :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                   :message %
                                                                   :buttons {:right {:text     "Continue"
                                                                                     :on-click on-success}}}])})))

(rf/reg-event-fx
 :get-user
 (fn [_ [_ input]]
   (let [auth (spec/conform ::spec/auth input)]
     (mm/get-user-fx {:auth auth
                      :on-success #(if (registered? %)
                                     (if-let [invitation (accepting-invitation?)]
                                       (rf/dispatch [::accept-invitation {:invitation invitation
                                                                          :on-success (fn [] (rf/dispatch [:refresh-token %]))}])
                                       (if-let [confirmation (delete-confirmation?)]
                                         (rf/dispatch [::ae/delete-account-confirm confirmation %])
                                         (if-let [action-code (verifying-email?)]
                                           (rf/dispatch [::verify-email {:action-code action-code
                                                                         :on-success (fn [] (rf/dispatch [:refresh-token %]))}])
                                           (rf/dispatch [:refresh-token %]))))
                                     (rf/dispatch [:create-user auth]))}))))

(rf/reg-event-fx
 :load-user
 (fn [cofx [_ input]]
   (let [db (:db cofx)
         user (spec/conform ::spec/user input)
         accounts (-> db :security :claims :roles shared/accounts-from)
         default-account-id (-> (:default-account-id user) keyword)]
     (rf/dispatch [::se/splash false])
     (if (second accounts) ; User has access to more than one account
       (if-let [account-id (some #{default-account-id} accounts)]
         (rf/dispatch [:set-active-account account-id])
         (rf/dispatch [::av/choose-account]))
       (rf/dispatch [:set-active-account (first accounts)]))
     (merge
      {:db            (assoc-in db [:security :user] user)}
      (mm/get-accounts-fx {:account-ids accounts
                           :on-success #(rf/dispatch [:load-account %])})))))

(defn verify-email-dialog [user]
  {:heading "Verify email"
   :message "Please check your email & click the verification link. If you haven't
                                               received it, you can try resending it"
   :closeable false
   :buttons {:left  {:text     "Resend"
                     :on-click #(rf/dispatch [:send-email-verification user])
                     :color :secondary}
             :right  {:text     "Sign out"
                      :on-click #(rf/dispatch [:sign-out])
                      :color :primary}}})

(rf/reg-event-db
 :load-claims
 (fn [db [_ input]]
   (let [user (spec/conform ::spec/user (:user input))
         claims (spec/conform ::spec/claims (:claims input))]
     (if (:email_verified claims)
       (do
         (rf/dispatch [:load-user user])
         (assoc-in db [:security :claims] claims))
       (assoc-in db [:site :dialog] (verify-email-dialog user))))))

(rf/reg-event-fx
 :send-email-verification
 (fn [_ [_ user]]
   (let [check-your-mail #(rf/dispatch [::se/dialog {:heading "Check your email"
                                                     :message (str "Email verification link sent to: " (:email user))
                                                     :closeable false}])]
     (rf/dispatch [::se/dialog])
     (mm/send-email-verification-fx {:on-success check-your-mail
                                     :on-error #(if (str/includes? % "Try again later")
                                                  (check-your-mail)
                                                  (rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %
                                                                             :closeable false}]))}))))

(rf/reg-event-fx
 :refresh-token
 (fn [_ [_ user]]
   (mm/refresh-token-fx {:user user})))

(rf/reg-event-fx
 ::accept-invitation
 (fn [_ [_ options]]
   (mm/accept-invitation-fx options)))

(rf/reg-event-fx
 :create-user
 [(rf/inject-cofx ::shared/gen-id)]                        ; Generate account id
 (fn [cofx [_ input]]
   (let [auth (spec/conform ::spec/auth input)
         user (spec/conform ::spec/user
                            {:id         (keyword (:uid auth))
                             :name       (:display-name auth)
                             :email      (:email auth)
                             :avatar-url (:photo-url auth)})
         account {:id   (keyword (:id cofx))
                  :name "My Account"
                  :subscription {:properties 1}}]
     (merge {:db                (assoc-in (:db cofx) [:site :show-progress] true)}
            (mm/create-user-fx {:user user
                                :account account
                                :invitation (accepting-invitation?)
                                :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                      :message (str %)}])})))))

(rf/reg-event-fx
 :sign-out
 (fn [cofx _]
   (merge {:db            (-> (assoc-in (:db cofx) [:site :signing-out] true)
                              (assoc-in [:site :dialog] {:heading "Signing out.." :closeable false}))}
          (mm/sign-out-fx {:on-success #(js/window.location.reload)
                           :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                 :message (str %)}])}))))



