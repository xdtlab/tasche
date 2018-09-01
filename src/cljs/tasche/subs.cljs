(ns tasche.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::has-logged-in
  (fn [db]
    (= (:state db) :logged-in)))

(re-frame/reg-sub
  ::is-logging-in
  (fn [db]
    (= (:state db) :logging-in)))

(re-frame/reg-sub
  ::public-key
  (fn [db]
    (js/daten.utils.hexToBase58 (.getAddress (:wallet db)) "address")))

(re-frame/reg-sub
  ::balance
  (fn [db]
    (:balance db)))

(re-frame/reg-sub
  ::transactions
  (fn [db]
    (:transactions db)))
