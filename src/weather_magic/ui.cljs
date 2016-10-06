(ns weather-magic.ui
  (:require
   [weather-magic.state :as state]
   [reagent.core :as reagent :refer [atom]]))

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:div
    [:input {:type "button" :value "Europe" :id "europe"
                 :on-click #(set! state/earth-view "Europe")}]
    [:input {:type "button" :value "Spinning" :id "spinning"
                   :on-click #(set! state/earth-view "Spinning")}]]
  )

(defn mount-ui!
  "Place the user interface into the DOM."
  []
  ;; We mount the map-ui by default into our UI <span>. If we, for
  ;; example, add a welcome UI later on it might instead be the default
  ;; UI to be mounted.
  (reagent/render [map-ui] (.getElementById js/document "ui"))
  true) ; Return true.
