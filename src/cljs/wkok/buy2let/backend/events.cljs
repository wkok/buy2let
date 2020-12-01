(ns wkok.buy2let.backend.events
  (:require [re-frame.core :as rf]
            [wkok.buy2let.shared :as shared]
            [wkok.buy2let.db.events :as dbe]
            [wkok.buy2let.site.events :as se]
            [wkok.buy2let.backend.impl :as impl]
            [wkok.buy2let.backend.subs :as bs]
            [wkok.buy2let.backend.effects]
            [wkok.buy2let.backend.protocol :as bp]
            [wkok.buy2let.spec :as spec]
            [goog.crypt.base64 :as b64]
            [reagent.core :as ra]
            [cemerick.url :as url]
            [nano-id.core :as nid]
            [clojure.string :as str]
            [cljs.reader]
            [reagent-material-ui.core.text-field :refer [text-field]]
            [reagent-material-ui.core.menu-item :refer [menu-item]]))

(rf/reg-event-fx
 ::sign-in
 (fn [_ [_ provider]]
   (case provider
     :google (bp/google-sign-in-fx impl/backend)
     :facebook (bp/facebook-sign-in-fx impl/backend)
     :twitter (bp/twitter-sign-in-fx impl/backend)
     :github (bp/github-sign-in-fx impl/backend))))

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
   (bp/apply-action-code-fx impl/backend
                            {:action-code action-code
                             :on-success on-success
                             :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" 
                                                                   :message %
                                                                   :buttons {:right {:text     "Continue"
                                                                                     :on-click on-success}}}])})))

(rf/reg-event-fx
 :get-user
 (fn [_ [_ input]]
   (let [auth (spec/conform ::spec/auth input)]
     (bp/get-user-fx impl/backend
                     {:auth auth
                      :on-success #(if (registered? %)
                                     (if-let [invitation (accepting-invitation?)]
                                       (rf/dispatch [::accept-invitation {:invitation invitation
                                                                          :on-success (fn [] (rf/dispatch [:refresh-token %]))}])
                                       (if-let [confirmation (delete-confirmation?)]
                                         (rf/dispatch [::delete-account-confirm confirmation %])
                                         (if-let [action-code (verifying-email?)]
                                           (rf/dispatch [::verify-email {:action-code action-code
                                                                         :on-success (fn [] (rf/dispatch [:refresh-token %]))}])
                                           (rf/dispatch [:refresh-token %]))))
                                     (rf/dispatch [:create-user auth]))}))))

(rf/reg-event-fx
 ::get-account
 (fn [_ [_ account-id]]
   (bp/get-account-fx impl/backend
                      {:account-id account-id
                       :on-success #(rf/dispatch [:load-account %])})))


(def selected-account-id (ra/atom :none))
(defn select-account []
  (let [accounts @(rf/subscribe [::bs/accounts])]
    [text-field {:select true
                 :label "Account"
                 :field     :list
                 :on-change #(reset! selected-account-id (-> % .-target .-value))
                 :value     @selected-account-id}
     (for [account accounts]
       ^{:key (key account)}
       [menu-item {:value (key account)} 
        (-> account val :name)])]))

(defn account-dialog []
  {:heading   "Which account?"
   :message   "Please choose the account you want to access"
   :panel     [select-account]
   :buttons   {:middle {:text     "Continue"
                        :on-click #(do
                                     (rf/dispatch [:set-active-account (keyword @selected-account-id)])
                                     (rf/dispatch [::se/dialog]))}}
   :closeable false})

(defn accounts-from [roles]
  (->> (map #(val %) roles)
       (reduce concat)
       distinct))

(defn choose-account-fx [db user accounts]
  (reset! selected-account-id (-> accounts first name))
  (rf/dispatch [::se/dialog (account-dialog)])
  (merge {:db (assoc-in db [:security :user] user)}
         (bp/get-accounts-fx impl/backend
                             {:account-ids accounts
                              :on-success #(rf/dispatch [:load-account %])})))

(defn get-account-fx [db user accounts]
  (merge {:db            (assoc-in db [:security :user] user)}
         (bp/get-account-fx impl/backend
                            {:account-id (first accounts)
                             :on-success #(do (rf/dispatch [:load-account %])
                                              (rf/dispatch [:set-active-account (:id %)]))})))

(rf/reg-event-fx
 :load-user
 (fn [cofx [_ input]]
   (let [user (spec/conform ::spec/user input)
         accounts (-> (:db cofx) :security :claims :roles accounts-from)]
     (rf/dispatch [::se/splash false])
     (if (second accounts) ; User has access to more than one account
       (choose-account-fx (:db cofx) user accounts)
       (get-account-fx (:db cofx) user accounts)))))

(rf/reg-event-db
 :load-account
 (fn [db [_ input]]
   (let [account (spec/conform ::spec/account input)]
     (update-in db [:security :accounts] #(assoc % (:id account) account)))))

(rf/reg-event-db
 ::view-account
 (fn [db [_ _]]
   (-> (assoc-in db [:site :active-page] :account)
       (assoc-in [:site :active-panel] :account-view)
       (assoc-in [:site :heading] "Account"))))

(rf/reg-event-db
 ::edit-account
 (fn [db [_ _]]
   (let [accounts (get-in db [:security :accounts])
         account-id (get-in db [:security :account])
         account (account-id accounts)]
     (-> (assoc-in db [:form :old :account] account)
         (assoc-in [:site :active-page] :account)
         (assoc-in [:site :active-panel] :account-edit)
         (assoc-in [:site :heading] "Edit account")))))

(rf/reg-event-fx
 ::save-account
 (fn [cofx [_ input]]
   (let [db (:db cofx)
         account (spec/conform ::spec/account input)]
     (js/window.history.back)                              ;opportunistic.. assume success 99% of the time..
     (merge {:db            (assoc-in db [:security :accounts (:id account)] account)}
            (bp/save-account-fx impl/backend
                                {:account account
                                 :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %}])})))))

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
     (bp/send-email-verification-fx impl/backend
                                    {:on-success check-your-mail
                                     :on-error #(if (str/includes? % "Try again later")
                                                  (check-your-mail)
                                                  (rf/dispatch [::se/dialog {:heading "Oops, an error!" :message %
                                                                             :closeable false}]))}))))

(rf/reg-event-fx
 :refresh-token
 (fn [_ [_ user]]
   (bp/refresh-token-fx impl/backend {:user user})))

(rf/reg-event-fx
 ::accept-invitation
 (fn [_ [_ options]]
   (bp/accept-invitation-fx impl/backend options)))

(rf/reg-event-db
 :set-active-account
 (fn [db [_ input]]
   (let [account-id (spec/conform ::spec/id input)]
     (rf/dispatch [::dbe/get-crud account-id])
     (assoc-in db [:security :account] account-id))))

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
                  :name (:display-name auth)}]
     (merge {:db                (assoc-in (:db cofx) [:site :show-progress] true)}
            (bp/create-user-fx impl/backend
                               {:user user
                                :account account
                                :invitation (accepting-invitation?)
                                :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                      :message (str %)}])})))))

(rf/reg-event-fx
 :sign-out
 (fn [cofx _]
   (merge {:db            (-> (assoc-in (:db cofx) [:site :signing-out] true)
                              (assoc-in [:site :dialog] {:heading "Signing out.." :closeable false}))}
          (bp/sign-out-fx impl/backend
                          {:on-success #(js/window.location.reload)
                           :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                                 :message (str %)}])}))))

(defn create-delete-confirmation [user account delete-token]
  {:to (:email user)
   :template {:name "delete-account"
              :data {:user-name (:name user)
                     :account-name (:name account)
                     :delete-url (str (.. js/window -location -protocol) "//"
                                      (.. js/window -location -host)
                                      "?delete-confirmation="
                                      (b64/encodeString {:user-id (:id user)
                                                         :account-id (:id account)
                                                         :delete-token delete-token}))}}})

(rf/reg-event-db
 ::delete-account-understood
 (fn [db [_ delete-token account-id]]
   (rf/dispatch [::se/splash false])
   (assoc-in db [:security :accounts account-id :deleteToken] delete-token)))

(defn active-delegates? [db]
  (not (empty? (->> (:delegates db)
                    (filter #(not= "REVOKED" (-> % val :status)))))))

(rf/reg-event-fx
 ::delete-account
 (fn [cofx _]
   (let [db (:db cofx)
         accounts (get-in db [:security :accounts])
         account-id (get-in db [:security :account])
         account (account-id accounts)
         user (get-in db [:security :user])
         delete-token (nid/nano-id)]
     
     (if (active-delegates? db)
       (rf/dispatch [::se/dialog {:heading "Delegated access"
                                  :message "You have users linked / invited to your account. Please revoke
                                               their access first, before continuing with account deletion"
                                  :buttons   {:middle {:text     "Take me there"
                                                       :on-click (fn [] (js/window.location.assign "#/delegates"))}}}])
       (merge {:db (assoc-in db [:site :splash] true)}
              (bp/delete-account-fx
               impl/backend
               {:user-id (:id user)
                :account-id account-id
                :delete-token delete-token
                :confirmation (create-delete-confirmation user account delete-token)
                :on-success #(rf/dispatch [::se/dialog {:heading   "Email confirmation"
                                                        :message   "Please check your email for an account deletion link.
                                                                                Your account will remain active until you confirm deletion 
                                                                                by clicking the link in the email."
                                                        :buttons   {:middle {:text     "Understood"
                                                                             :on-click (fn [] (rf/dispatch [::delete-account-understood delete-token account-id]))}}
                                                        :closeable false}])
                :on-error #(rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                      :message (str %)}])}))))))

(rf/reg-event-fx
 ::delete-account-confirm
 (fn [cofx [_ {:keys [account-id user-id delete-token]} user]]
   (shared/reset-query-params)
   (merge {:db                (assoc-in (:db cofx) [:site :splash] true)}
          (bp/delete-account-confirm-fx
           impl/backend
           {:user-id user-id
            :account-id account-id
            :delete-token delete-token
            :on-success #(do
                           (rf/dispatch [::se/splash false])
                           (rf/dispatch [::se/show-progress false])
                           (rf/dispatch [::se/dialog {:heading   "Account deleted"
                                                      :message   "Successfully deleted your account. Hope to have you back soon!"
                                                      :buttons   {:middle {:text     "Goodbye"
                                                                           :on-click (fn [] (rf/dispatch [:sign-out]))}}
                                                      :closeable false}]))
            :on-error #(do
                         (rf/dispatch [:refresh-token user])
                         (rf/dispatch [::se/splash false])
                         (rf/dispatch [::se/show-progress false])
                         (rf/dispatch [::se/dialog {:heading "Oops, an error!"
                                                    :message (str %)}]))}))))
