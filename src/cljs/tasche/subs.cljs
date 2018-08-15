(ns tasche.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::private-key
 (fn [db]
   (:private-key db)))
