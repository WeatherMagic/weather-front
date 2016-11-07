(ns weather-magic.ui
  (:require
   [weather-magic.state :as state]
   [weather-magic.world :as world]
   [weather-magic.util  :as util]
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defn button
  "Creates a button with a given HTML id which when clicked does func on atom with args."
  [id func atom & args]
  [:input {:type "button" :value id :id id
           :on-click #(apply func atom args)}])

(defn slider [key value min max]
  [:input {:type "range" :value value :min min :max max
           :on-change (fn [event] (swap! state/date-atom assoc-in [key :value]
                                         (.-target.value event)))}])

(defn slider-component [key]
  (let [data (key @state/date-atom)]
    [:div {:class "time-slider"}
     [:span (clojure.string/capitalize (name key)) ": " (:value data)]
     [slider key (:value data) (:min data) (:max data)]]))

(defn time-slider []
  [:div {:id "time-slider-container"}
   [slider-component :year]
   [slider-component :month]])

(defn data-layer-buttons
  "Buttons for choosing which data layer to display"
  []
  [:div
   [button "Temperature" swap! state/data-layer-atom util/toggle :temp]
   [button "Sea level"   swap! state/data-layer-atom util/toggle :water]
   [button "Pests"       swap! state/data-layer-atom util/toggle :pests]
   [button "Drought"     swap! state/data-layer-atom util/toggle :drought]])

;; Blur canvas
(defn hide-unhide
  "Returns the inverse of hidden and visible. If :hidden is given, :visible is returned and vice versa."
  [hidden-or-not]
  (hidden-or-not {:hidden :visible :visible :hidden}))

(defn map-ui-blur []
  [:div {:class @state/intro-visible :id "blur"}])

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:span
   [data-layer-buttons]
   [button "Europe"   reset! state/earth-animation-fn world/show-europe]
   [button "Spinning" reset! state/earth-animation-fn world/spin]
   [button "Go to map" swap! state/intro-visible #(swap! state/intro-visible hide-unhide)]
   [time-slider]
   [map-ui-blur]])

(defn mount-ui!
  "Place the user interface into the DOM."
  []
  ;; We mount the map-ui by default into our UI <span>. If we, for
  ;; example, add a welcome UI later on it might instead be the default
  ;; UI to be mounted.
  (reagent/render [map-ui] (.getElementById js/document "ui"))
  true) ; Return true.
