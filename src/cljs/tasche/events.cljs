(ns tasche.events
  (:require
   [re-frame.core :as re-frame]
   [tasche.db :as db]
   ))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))


(re-frame/reg-event-db
  ::login
  (fn [db [_ private-key]]
    (assoc db :private-key private-key)))
