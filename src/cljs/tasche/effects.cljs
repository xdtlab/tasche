(ns tasche.effects
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-fx
  ::swal
  (fn [config]
    (js/swal (clj->js config))))

(re-frame/reg-fx
  ::toast
  (fn [config]
    (js/swal (clj->js (merge {:toast true
                              :position "top-end"
                              :showConfirmButton false
                              :timer 3000} config)))))

(re-frame/reg-fx
  ::connect
  (fn [{:keys [private-key on-success on-fail]}]
    (js/daten.contrib.getNodeList
      (fn [nodes]
        (let [wallet (js/daten.Wallet. (rand-nth nodes) private-key)]
          (.getStatus wallet #(on-success wallet) #(on-fail))))
      on-fail)))

(re-frame/reg-fx
  ::get-balance
  (fn [{:keys [wallet on-success on-fail]}]
    (.getBalance wallet #(on-success %) #(on-fail))))

(re-frame/reg-fx
  ::get-latest
  (fn [{:keys [wallet on-success on-fail]}]
    (.latest wallet #(on-success %) #(on-fail))))

(re-frame/reg-fx
  ::confirm-transaction
  (fn [{:keys [wallet transaction on-success on-fail]}]
    (.confirm wallet transaction 6 #(on-success %2) #(on-fail))))

(re-frame/reg-fx
  ::query
  (fn [{:keys [wallet filters on-success on-fail]}]
    (.query wallet (clj->js filters) on-success on-fail)))

(re-frame/reg-fx
  ::create-transaction
  (fn [{:keys [wallet name to amount data on-success on-fail]}]
    (.createTransaction wallet name to amount data on-success on-fail)))

(re-frame/reg-fx
  ::send-transaction
  (fn [{:keys [wallet transaction on-success on-fail]}]
    (.sendTransaction wallet transaction on-success on-fail)))

(re-frame/reg-fx
  ::download
  (fn [{:keys [name content]}]
    (let [href (str "data:text/plain;charset=utf-8," (js/escape (str content)))
          a (js/document.createElement "a")]
      (set! (.-href a) href)
      (set! (.-download a) name)
      (.click a))))

(re-frame/reg-fx
  ::save
  (fn [{:keys [key value]}]
    (js/localStorage.setItem key value)
    (js/localStorage.getItem key)))

(re-frame/reg-fx
  ::load
  (fn [{:keys [key on-loaded]}]
    (on-loaded (js/localStorage.getItem key))))
