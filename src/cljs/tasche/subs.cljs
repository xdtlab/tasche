(ns tasche.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::has-logged-in
  (fn [db]
    (and
      (not= (:state db) :not-logged-in)
      (not= (:state db) :logging-in))))

(re-frame/reg-sub
  ::is-logging-in
  (fn [db]
    (= (:state db) :logging-in)))

(re-frame/reg-sub
  ::public-key
  (fn [db]
    (.getAddress (:wallet db))))

(re-frame/reg-sub
  ::balance
  (fn [db]
    (:balance db)))

(re-frame/reg-sub
  ::transactions
  (fn [db]
    (let [{:keys [transactions pendings]} db
          all (sort-by #(aget (:transaction %) "target") (vals (merge pendings transactions)))]
      all)))
