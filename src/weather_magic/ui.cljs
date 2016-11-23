(ns weather-magic.ui
  (:require
   [weather-magic.models   :as models]
   [weather-magic.state    :as state]
   [weather-magic.world    :as world]
   [weather-magic.shaders  :as shaders]
   [weather-magic.util     :as util]
   [reagent.core           :as reagent :refer [atom]]
   [thi.ng.geom.gl.shaders :as sh]))

(enable-console-print!)

(defn hide-unhide
  "Returns the inverse of hidden and visible. If :hidden is given, :visible is returned and vice versa."
  [hidden-or-not]
  (hidden-or-not {:hidden :visible :visible :hidden}))

(defn button
  "Creates a button with a given HTML id which when clicked does func on atom with args."
  [id func atom & args]
  [:input {:type "button" :value id :id id :class "button"
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
  [:div {:id "data-layer-container" :class (hide-unhide @state/intro-visible)}
   [button "Temperature" swap! state/data-layer-atom util/toggle :Temperature]
   [button "Sea-level"   swap! state/data-layer-atom util/toggle :Sea-level]
   [button "Pests"       swap! state/data-layer-atom util/toggle :Pests]
   [button "Drought"     swap! state/data-layer-atom util/toggle :Drought]])

(defn view-selection-buttons
  "Buttons for choosing view"
  []
  [:div {:id "view-selection-container" :class (hide-unhide @state/intro-visible)}
   [button "Turkey" reset! state/earth-animation-fn world/show-turkey!]
   [button "World"  reset! state/earth-animation-fn world/spin-earth!]
   [button "Europe" reset! state/earth-animation-fn world/show-europe!]
   [button "Northpole Up" reset! state/earth-animation-fn world/northpole-up!]])

(defn compile-shader [s]
  (sh/make-shader-from-spec state/gl-ctx s))

(defn shader-selection-buttons
  "Buttons for choosing shader"
  []
  [:div {:id "shader-selection-container"}
   [button "Go to map"          swap!  state/intro-visible  #(swap! state/intro-visible hide-unhide)]
   [button "Standard shader"    reset! state/current-shader (compile-shader shaders/standard-shader-spec)]
   [button "Blend shader"       reset! state/current-shader (compile-shader shaders/blend-shader-spec)]
   [button "Temperature shader" reset! state/current-shader (compile-shader shaders/temperature-shader-spec)]])

(defn map-ui-blur []
  "What hides the map UI."
  [:div {:class @state/intro-visible :id "blur"}])

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:span
   [data-layer-buttons]
   [view-selection-buttons]
   [shader-selection-buttons]
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
