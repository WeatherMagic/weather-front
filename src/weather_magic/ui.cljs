(ns weather-magic.ui
  (:require
   [reagent.core :as reagent :refer [atom]]))

;; Time-slider
(def date-atom (atom {:year {:value 2016 :min 1950 :max 2100}
                      :month {:value 1 :min 1 :max 12}}))

(defn slider [key value min max]

  [:input {:type "range" :value value :min min :max max
           :on-change (fn [e]
                        (swap! date-atom assoc-in [key :value] (.-target.value e)))}])

(defn slider-component [key]
  (let [data (key @date-atom)]
    [:div {:class "time-slider"}
     [:span (clojure.string/capitalize (name key)) ": " (:value data)]
     [slider key (:value data) (:min data) (:max data)]]))

(defn time-slider []
  [:div {:id "time-slider-container"}
   [slider-component :year]
   [slider-component :month]])

(defn data-layers-buttons
  "Buttons for choosing which data layer to display"
  []
  [:div
  [:input {:type "button" :value "Visualize temperature changes"
           :class "data-layers-button"}]
  [:input {:type "button" :value "Visualize sea water level"
           :class "data-layers-button"}]
  [:input {:type "button" :value "Visualize pests"
           :class "data-layers-button"}]
<<<<<<< 85fda19750eba98bb4c0a859f51bddf40aaa9e18
  [:input {:type "button" :value "Visualize "
           :class "data-layers-button"}]]))
=======
  [:input {:type "button" :value "Visualize drought"
           :class "data-layers-button"}]
  ]
  )
)
>>>>>>> continued with buttons

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:span
   [data-layers-buttons]
   [time-slider]])

(defn mount-ui!
  "Place the user interface into the DOM."
  []
  ;; We mount the map-ui by default into our UI <span>. If we, for
  ;; example, add a welcome UI later on it might instead be the default
  ;; UI to be mounted.
  (reagent/render [map-ui] (.getElementById js/document "ui"))
  true) ; Return true.
