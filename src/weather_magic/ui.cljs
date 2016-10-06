(ns weather-magic.ui
  (:require
   [weather-magic.state :as state]
   [weather-magic.world :as world]
   [reagent.core :as reagent :refer [atom]]))

(defn animation-button
  "Creates a button which sets a world-animation."
  [id function]
  [:input {:type "button" :value id :id id
               :on-click #(reset! state/earth-animation-fn function)}])
(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:div
    [animation-button "Europe" world/show-europe]
    [animation-button "Spinning" world/spin]])

(defn mount-ui!
  "Place the user interface into the DOM."
  []
  ;; We mount the map-ui by default into our UI <span>. If we, for
  ;; example, add a welcome UI later on it might instead be the default
  ;; UI to be mounted.
  (reagent/render [map-ui] (.getElementById js/document "ui"))
  true) ; Return true.
