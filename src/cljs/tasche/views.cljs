(ns tasche.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [tasche.subs :as subs]
            [tasche.events :as events]))

(defn login-panel []
  (let [login-info (reagent/atom {:seed "" :salt ""})]
    (fn []
      [:form
        [:div
          [:h1.title.has-text-centered.has-text-weight-light {:style {:font-family "Cairo"}} "Login"]
          [:div.field
            [:div.field [:input.input {:type :text :spell-check false :placeholder "Seed" :on-change #(swap! login-info assoc :seed (-> % .-target .-value))}]]
            [:div.field [:input.input {:type :password :placeholder "Salt" :on-change #(swap! login-info assoc :salt (-> % .-target .-value))}]]
            [:div.has-text-centered.is-size-7
              [:a {:on-click #(js/swal "Towards simpler accounts..." "Login to your account with two random strings Seed & Salt!" "info")}
              "(WTH?!)"]]]
          [:div.has-text-centered
            (let [is-logging-in (re-frame/subscribe [::subs/is-logging-in])]
              [(if @is-logging-in
                :button.button.is-success.is-loading
                :button.button.is-success) {
                  :on-click
                    (fn [event]
                      (.preventDefault event)
                      (re-frame/dispatch [::events/login @login-info]))} "Login!"])]]])))

(defn send-dialog
  [on-close on-send]
  (let [tx (reagent/atom {:to "" :name "" :amount 0})]
    (fn []
      [:div.modal.is-active
        [:div.modal-background]
        [:div.modal-content.animated.bounceIn
          [:div.box.column.is-8.is-offset-2
            [:h1.title.is-size-4.has-text-centered "Send XDTs!"]
            [:div.field [:input.input {:type :text :spell-check "false" :placeholder "To" :on-change #(swap! tx assoc :to (-> % .-target .-value))}]]
            [:div.field [:input.input {:type :text :spell-check "false" :placeholder "Name" :on-change #(swap! tx assoc :name (-> % .-target .-value))}]]
            [:div.field.has-addons
              [:div.control.is-expanded [:input.input {:type :number :placeholder "Amount" :on-change #(swap! tx assoc :amount (js/parseInt (-> % .-target .-value)))}]]
              [:div.control [:button.button.is-static "XDT"]]]

            [:button.button.is-success {:on-click #(do (re-frame/dispatch [::events/create-transaction @tx]) (on-close))} "Send!"]]]
        [:button.modal-close.is-large {:aria-label :close :on-click on-close}]])))


(defn receive-dialog
  [public-key on-close]
  [:div.modal.is-active
    [:div.modal-background]
    [:div.modal-content.animated.bounceIn
      [:div.box.column.is-8.is-offset-2
        [:h1.title.is-size-4.has-text-centered "Receive XDTs!"]
        [:p.has-text-centered.is-size-7 (str public-key)]
        [:button.button.is-info {:on-click on-close} "Ok!"]]]
    [:button.modal-close.is-large {:aria-label :close :on-click on-close}]])

(defn transaction [tx]
  [:tr
    [:div {:style {:padding "5px"}}
      (str (js/daten.Wallet.formatAmount (aget (:transaction tx) "amount")))
      (case (:state tx)
        :confirmed [:div.is-pulled-right (quot (:confirmations tx) 1000) "kHash"]
        :pending [:div.is-pulled-right "Pending..."]
        :error [:div.is-pulled-right "Error!"])]])

(defn transaction-list []
  (let [show-more (reagent/atom false)
        transactions (re-frame/subscribe [::subs/transactions])]
    (fn []
      (if-let [txs (seq (reverse @transactions))]
        [:div
          [:h5.title.has-text-centered.has-text-weight-normal.has-text-dark.is-size-6 "Your recent transactions:"]
          [:table.table.is-fullwidth.is-striped.is-hoverable
            [:tbody
              (for [tx (if @show-more txs (take 5 txs))]
                [transaction tx])]
            [:div.has-text-centered.is-size-7
              (if-not @show-more
                [:a {:on-click #(reset! show-more true)} "Show more"]
                [:a {:on-click #(reset! show-more false)} "Show less"])]]]
        [:h5.title.has-text-centered.has-text-weight-normal.has-text-dark.is-size-6 "No transactions yet :)"]))))

(defn main-panel []
  (let [is-sending (reagent/atom false)
        is-receiving (reagent/atom false)
        public-key (re-frame/subscribe [::subs/public-key])]
    (fn []
      [:div
        [:p.has-text-centered.is-size-7 (str @public-key)]
        (if @is-sending
          [send-dialog #(reset! is-sending false)
            #(re-frame/dispatch [::events/new-transaction])])
        (if @is-receiving
          [receive-dialog @public-key #(reset! is-receiving false)])

        (let [balance (re-frame/subscribe [::subs/balance])]
          [:h1.title.has-text-centered.has-text-weight-light {:style {:font-family "Cairo"}}
            (if @balance
              (js/daten.Wallet.formatAmount @balance)
              "Connecting...")])

        [transaction-list]
        [:div.buttons.is-centered
          [:button.button.is-success {:on-click #(reset! is-sending true)} "Send"]
          [:button.button.is-danger {:on-click #(reset! is-receiving true)} "Receive"]]])))

(defn start []
  [:div.hero.is-fullheight
    [:div.hero-body
      [:div.container
        [:div.columns
          [:div.column]
          (let [has-logged-in (re-frame/subscribe [::subs/has-logged-in])]
            [(if @has-logged-in :div.column.is-5 :div.column.is-4)
              [:div.box
                [:figure.level.has-text-centered
                  [:img.image.is-64x64.level-item {:src "img/logo.svg"}]]
                (if @has-logged-in
                  [main-panel]
                  [login-panel])]])
          [:div.column]]]]])
