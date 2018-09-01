(ns tasche.events
  (:require
    [re-frame.core :as re-frame]
    [tasche.db :as db]
    [tasche.effects :as effects]
    [cljs.reader :as reader]))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

;-------------------------------------------------------

(re-frame/reg-event-fx
  ::login
  (fn [{:keys [db]} [_ {:keys [seed salt]}]]
    (cond
      (empty? seed)
        {::effects/swal {:title "Please enter a seed!"
                         :text "It can be a random string, your email address, a dummy username or..."}}

      (empty? salt)
        {::effects/swal {:title "Please enter a salt!"
                         :text "Additional security for generating a private-key!"}}

      :else
        (let [private-key (js/daten.hash.regular (js/daten.utils.encodeUtf8 (str seed salt)))]
          {:db (assoc db :state :logging-in)
           ::effects/connect {:private-key  private-key
                              :on-success   #(re-frame/dispatch [::login-success %])
                              :on-fail      #(re-frame/dispatch [::login-fail])}}))))

(re-frame/reg-event-fx
  ::login-success
  (fn [{:keys [db]} [_ wallet]]
    {:db (-> db
            (assoc :wallet wallet)
            (assoc :state :logged-in))
     ::effects/toast {:type :success :title "Connected successfully!"}}))

(re-frame/reg-event-fx
  ::login-fail
  (fn [{:keys [db]} _]
    {:db (assoc db :state :not-logged-in)
     ::effects/toast {:type :error :title "Couldn't connect to the network!"}}))

(re-frame/reg-event-fx
  ::logout
  (fn [{:keys [db]} _]
    {:db (-> db
          (assoc :state :not-logged-in)
          (assoc :wallet nil))
     ::effects/toast {:type :error :title "Couldn't connect to the network!"}}))

;-------------------------------------------------------

(re-frame/reg-event-fx
  ::update-balance
  (fn [{:keys [db]} _]
    {::effects/get-balance {:wallet (:wallet db)
                            :on-success #(re-frame/dispatch [::update-balance-success %])
                            :on-fail #(do )}}))

(re-frame/reg-event-db
  ::update-balance-success
  (fn [db [_ balance]]
    (assoc db :balance balance)))

;-------------------------------------------------------

(re-frame/reg-event-fx
  ::update-transactions
  (fn [{:keys [db]} _]
    {:db db
     ::effects/get-latest {:wallet (:wallet db)
                           :on-success #(re-frame/dispatch [::update-transactions-success %])
                           :on-fail #(do )}}))

(re-frame/reg-event-fx
  ::update-transactions-success
  (fn [{:keys [db]} [_ txs]]
    {:db (assoc db :transactions txs)}))

;-------------------------------------------------------

(re-frame/reg-event-fx
  ::send-transaction
  (fn [{:keys [db]} [_ {:keys [to name amount]}]]
    {::effects/send-transaction {:wallet (:wallet db)
                                 :to (js/daten.address.Address.fromString to)
                                 :name (if (empty? name) nil name)
                                 :data (js/daten.data.NoData.)
                                 :amount amount
                                 :on-success #(re-frame/dispatch [::send-transaction-success %])
                                 :on-fail #(do )}}))

(re-frame/reg-event-fx
  ::send-transaction-success
  (fn [{:keys [db]} [_ response]]
    (if (aget response "ok")
      {::effects/toast {:type :success :title "Transaction sent!"}}
      {::effects/swal {:type :error :title "Oops..." :text (aget response "error")}})))

;-------------------------------------------------------

(re-frame/reg-event-db
  ::refresh
  (fn [db _]
    (when (= (:state db) :logged-in)
      (re-frame/dispatch [::update-balance])
      (re-frame/dispatch [::update-transactions]))
    db))

(defonce do-timer (js/setInterval #(re-frame/dispatch [::refresh]) 10000))
