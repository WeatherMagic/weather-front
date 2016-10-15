(ns weather-magic.ui
  (:require
   [weather-magic.state :as state]
   [thi.ng.geom.gl.camera :as cam]
   [weather-magic.world :as world]
   [reagent.core :as reagent :refer [atom]]))

(defn button
  "Creates a button with a given HTML id which when clicked does func on atom with value."
  [id func atom value]
  [:input {:type "button" :value id :id id
           :on-click #(func atom value)}])

(enable-console-print!) ; For being able to how see how data-layer set is changed
                        ; when pressing daya-layer buttons

(defn slider [key value min max]
  [:input {:type "range" :value value :min min :max max
           :on-change (fn [e]
                        (swap! state/date-atom assoc-in [key :value] (.-target.value e)))}])

(defn slider-component [key]
  (let [data (key @state/date-atom)]
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
           :on-click #((swap! state/data-layer-atom
                              (if (contains? @state/data-layer-atom data-layer) disj conj)
                              data-layer)
                       (println (str "data-layers to be visualized: "
                                     @state/data-layer-atom)))}])

(defn data-layer-buttons
  "Buttons for choosing which data layer to display"
  []
  [:div
   [data-layer-button "temp" "temperature changes"]
   [data-layer-button "water" "sea water level"]
   [data-layer-button "pests" "pests"]
   [data-layer-button "drought" "drought"]])

;; Blur canvas
(defn hide-unhide
  "Returns the inverse of hidden and visible. If :hidden is given, :visible is returned and vice versa."
  [hidden-or-not]
  (hidden-or-not {:hidden :visible :visible :hidden}))

(defn map-ui-blur []
  [:div {:class @state/intro-visible :id "blur"}])

(defn zoom-camera
  [camera-map scroll-distance]
  (let [current-value (:fov camera-map)]
    (cam/perspective-camera (assoc camera-map :fov (min 140 (+ current-value (* current-value scroll-distance 5.0E-4)))))))

(defonce scroll-event
  (.addEventListener (.getElementById js/document "main") "wheel" (fn [event] (swap! state/camera zoom-camera (.-deltaY event))) false))

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:span
   [data-layer-buttons]
   [button "Europe"   reset! state/earth-animation-fn world/show-europe]
   [button "Spinning" reset! state/earth-animation-fn world/spin]
   [button "Scroll"   swap!  state/camera #(cam/perspective-camera (update-in % [:fov] - 10))]
   [time-slider]
   [map-ui-blur]
   [button "Go to map" swap! state/intro-visible #(swap! state/intro-visible hide-unhide)]])

(defn mount-ui!
  "Place the user interface into the DOM."
  []
  ;; We mount the map-ui by default into our UI <span>. If we, for
  ;; example, add a welcome UI later on it might instead be the default
  ;; UI to be mounted.
  (reagent/render [map-ui] (.getElementById js/document "ui"))
  true) ; Return true.
