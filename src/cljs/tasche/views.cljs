(ns tasche.views
  (:require
   [re-frame.core :as re-frame]
   [tasche.subs :as subs]
   [tasche.events :as events]))

(defn login-panel
  []
  [:div
    [:h1 "Login!"]
    [:div
      [:input]
      [:button.button {:on-click #(re-frame/dispatch [::events/login "ABCD"])} "Login!"]]])

(defn main-panel
  [private-key]
  [:div
    [:h1 "Logged in " private-key]])

(defn start []
  (let [private-key (re-frame/subscribe [::subs/private-key])]
    (if @private-key
      [main-panel @private-key]
      [login-panel])))
