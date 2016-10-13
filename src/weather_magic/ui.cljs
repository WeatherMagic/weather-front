(ns weather-magic.ui
  (:require
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!) ; For being able to how see how data-layer set is changed
                        ; when pressing daya-layer buttons

(def data-layer-atom (atom #{})) ; Should be in state.cljs later on

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

(defn data-layer-button
  "Creates a button which adds a data-layer to be displayed"
  [data-layer data-layer-button-text]
  [:input {:type "button" :value (str "Visualize " data-layer-button-text)
           :class "data-layer-button"
           :on-click #((swap! data-layer-atom 
                              (if (contains? @data-layer-atom data-layer) disj conj)
                              data-layer)
                       (println (str "data-layers to be visualized: "
                                     @data-layer-atom)))}])
  

(defn data-layer-buttons
  "Buttons for choosing which data layer to display"
  []
  [:div
    [data-layer-button "temp" "temperature changes"]
    [data-layer-button "water" "sea water level"]
    [data-layer-button "pests" "pests"]
    [data-layer-button "drought" "drought"]])

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:span
   [data-layer-buttons]
   [time-slider]])

(defn mount-ui!
  "Place the user interface into the DOM."
  []
  ;; We mount the map-ui by default into our UI <span>. If we, for
  ;; example, add a welcome UI later on it might instead be the default
  ;; UI to be mounted.
  (reagent/render [map-ui] (.getElementById js/document "ui"))
  true) ; Return true.
