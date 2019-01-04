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
    (re-frame/dispatch [::refresh])
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
    (let [txs (into {} (map #(identity [(.hash %) {:state :confirmed :transaction % :confirmations 0}]) (vec txs)))]
      (doseq [[_ {:keys [transaction]}] txs]
        (re-frame/dispatch [::confirm-transaction transaction]))
      {:db (update-in db [:transactions] #(merge txs %))})))

(re-frame/reg-event-fx
  ::confirm-transaction
  (fn [{:keys [db]} [_ tx]]
    {::effects/confirm-transaction {:wallet (:wallet db)
                                    :transaction tx
                                    :on-fail #(do )
                                    :on-success #(re-frame/dispatch [::confirm-transaction-success tx %])}}))

(re-frame/reg-event-fx
  ::confirm-transaction-success
  (fn [{:keys [db]} [_ tx hashes]]
    {:db (assoc-in db [:transactions (.hash tx) :confirmations] hashes)}))

;-------------------------------------------------------

(re-frame/reg-event-fx
  ::create-transaction
  (fn [{:keys [db]} [_ {:keys [to name amount]}]]
    {::effects/create-transaction {:wallet (:wallet db)
                                   :to (js/daten.address.Address.fromString to)
                                   :name (if (empty? name) nil name)
                                   :data (js/daten.data.NoData.)
                                   :amount amount
                                   :on-success #(re-frame/dispatch [::create-transaction-success %])
                                   :on-fail #(do )}}))

(re-frame/reg-event-fx
  ::create-transaction-success
  (fn [{:keys [db]} [_ tx]]
    {:db (update-in db [:pendings] assoc (.hash tx) {:state :pending :transaction tx :confirmations 0})
     ::effects/send-transaction {:wallet (:wallet db)
                                 :transaction tx
                                 :on-success #(re-frame/dispatch [::transaction-success tx %])
                                 :on-fail #(re-frame/dispatch [::transaction-fail tx %])}}))

(re-frame/reg-event-fx
  ::transaction-success
  (fn [{:keys [db]} [_ tx response]]
    (if (aget response "ok") {} {:db (assoc-in db [:pendings (.hash tx) :state] :error)})))

(re-frame/reg-event-fx
  ::transaction-fail
  (fn [{:keys [db]} [_ tx response]]
    {:db (assoc-in db [:pendings (.hash tx) :state] :error)}))
;-------------------------------------------------------

(re-frame/reg-event-db
  ::refresh
  (fn [db _]
    (when (= (:state db) :logged-in)
      (re-frame/dispatch [::update-balance])
      (re-frame/dispatch [::update-transactions]))
    db))

(defonce do-timer (js/setInterval #(re-frame/dispatch [::refresh]) 30000))
