(ns wkok.buy2let.backend.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.account.events :as ae]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.site.effects :as sfx]
            [wkok.buy2let.account.views :as av]
            [wkok.buy2let.backend.effects]
            [wkok.buy2let.backend.multimethods :as mm]
            [wkok.buy2let.spec :as spec]
            [goog.crypt.base64 :as b64]
            [cemerick.url :as url]
            [clojure.string :as str]
            [clojure.walk :as w]
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

(defn decode-param [url param]
  (when-let [p (get-in url [:query param])]
    (-> p
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
 ::verify-email-changed
 (fn [_ [_ {:keys [user on-success]}]]
   (merge {::sfx/remove-query-params {}}
          (mm/verify-email-changed-fx {:user user
                               :on-success on-success
                               :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                     :message (str %)
                                                                     :closeable false
                                                                     :buttons {:left  {:text     "Close"
                                                                                       :on-click on-success
                                                                                       :color :primary}}}])}))))

(defn accept-invitation [user invitation]
  (rf/dispatch [::accept-invitation {:invitation invitation
                                     :on-success #(rf/dispatch [:refresh-token user])}]))

(defn confirm-deletion [user confirmation]
  (rf/dispatch [::ae/delete-account-confirm confirmation user]))

(defn verify-email [user verification]
  (rf/dispatch [::verify-email {:action-code verification
                                :on-success #(rf/dispatch [:refresh-token user])}]))

(defn verify-email-changed [user verification]
  (if (= (:email-changed-token user) (:email-changed-token verification))
    (rf/dispatch [::verify-email-changed {:user user
                                          :on-success #(rf/dispatch [:refresh-token user])}])
    (rf/dispatch [::se/dialog {:heading "Invalid token"
                               :message "Invalid or expired token. New email could not be verified"
                               :closeable false
                               :buttons {:left  {:text     "Close"
                                                 :on-click #(rf/dispatch [:refresh-token user])
                                                 :color :primary}}}])))

(rf/reg-event-db
 ::set-subscription-action
 (fn [db [_ action]]
   (assoc-in db [:backend :subscription :action] action)))

(rf/reg-event-fx
 :get-user
 (fn [_ [_ input]]
   (let [auth (spec/conform ::spec/auth input)
         url (shared/url-full)
         invitation (decode-param url "invitation")
         delete-confirmation (decode-param url "delete-confirmation")
         email-verification (get-in url [:query "oobCode"])
         email-changed-verification (decode-param url "email-changed-verification")]
     (rf/dispatch [::set-subscription-action (case (shared/url-param "subscription")
                                               "activated" :activated
                                               "cancelled" :cancelled
                                               :unchanged)])
     (mm/get-user-fx {:auth auth
                      :on-success #(if (registered? %)
                                     (cond
                                       invitation (accept-invitation % invitation)
                                       delete-confirmation (confirm-deletion % delete-confirmation)
                                       email-verification (verify-email % email-verification)
                                       email-changed-verification (verify-email-changed % email-changed-verification)
                                       :else (rf/dispatch [:refresh-token %]))
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

(defn email-verified? [db claims]
  (let [provider-id (-> db
                        :security
                        :auth
                        :provider-data
                        js->clj
                        first
                        w/keywordize-keys
                        :providerId)]
    (or (:email_verified claims)
        (= "facebook.com" provider-id))))

(rf/reg-event-db
 :load-claims
 (fn [db [_ input]]
   (let [user (spec/conform ::spec/user (:user input))
         claims (spec/conform ::spec/claims (:claims input))]
     (if (email-verified? db claims)
       (do
         (rf/dispatch [:load-user user])
         (assoc-in db [:security :claims] claims))
       (assoc-in db [:site :dialog] (verify-email-dialog user))))))

(rf/reg-event-fx
 :send-email-verification
 (fn [_ [_ user]]
   (let [check-your-mail #(rf/dispatch [::se/dialog {:heading "Check your email"
                                                     :message (str "Email verification link sent to: " (:email user))}])]
     (rf/dispatch [::se/dialog])
     (mm/send-email-verification-fx {:on-success check-your-mail
                                     :on-error #(if (str/includes? % "Try again later")
                                                  (check-your-mail)
                                                  (rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %
                                                                             :closeable false}]))}))))

(rf/reg-event-fx
 :refresh-token
 (fn [_ [_ user]]
   (merge {::sfx/remove-query-params {}}
          (mm/refresh-token-fx {:user user}))))

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
                  :name "My Account"}
         invitation (-> (shared/url-full)
                        (decode-param "invitation"))]
     (merge {:db                (assoc-in (:db cofx) [:site :show-progress] true)}
            (mm/create-user-fx {:user user
                                :account account
                                :invitation invitation
                                :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                      :message (str %)}])})))))

(defn cleanup [cleanup-fns]
  (-> (for [cleanup-fn cleanup-fns]
        (cleanup-fn))
      doall))

(rf/reg-event-fx
 :sign-out
 (fn [cofx _]
   (let [db (:db cofx)]
     (-> (get-in db [:backend :cleanup-fns]) cleanup)
     (merge {:db            (-> (assoc-in db [:site :signing-out] true)
                                (assoc-in [:site :dialog] {:heading "Signing out.." :closeable false}))}
            (mm/sign-out-fx {:on-success #(js/window.location.reload)
                             :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                   :message (str %)}])})))))

(rf/reg-event-db
 ::register-cleanup-fn
 (fn [db [_ cleanup-fn]]
   (update-in db [:backend :cleanup-fns] #(conj % cleanup-fn))))

(rf/reg-event-db
 ::register-payment-instance
 (fn [db [_ instance]]
   (assoc-in db [:backend :subscription :instance] instance)))

(rf/reg-event-db
 ::register-backend-mode
 (fn [db [_ mode]]
   (assoc-in db [:backend :mode] mode)))