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
  [:input {:type "button" :value id :id id :class "data-layer-button"
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
  [:div {:id "data-layer-container"}
   [button "Temperature" swap! state/data-layer-atom util/toggle :Temperature]
   [button "Sea-level"   swap! state/data-layer-atom util/toggle :Sea-level]
   [button "Pests"       swap! state/data-layer-atom util/toggle :Pests]
   [button "Drought"     swap! state/data-layer-atom util/toggle :Drought]])

(defn view-selection-buttons
  "Buttons for choosing view"
  []
  [:div {:id "view-selection-container"}
   [button "Europe"   reset! state/earth-animation-fn world/show-europe]
   [button "Spinning" reset! state/earth-animation-fn world/spin]])

;; Blur canvas
(defn hide-unhide
  "Returns the inverse of hidden and visible. If :hidden is given, :visible is returned and vice versa."
  [hidden-or-not]
  (hidden-or-not {:hidden :visible :visible :hidden}))

(defn map-ui-blur []
  [:div {:class @state/intro-visible :id "blur"}])

(defn close-blur-button []
  [:div
   [:input {:type "button" :value "Read more!"
            :on-click #(swap! blur-visible (fn [value] (hide-unhide value)))}]])

(defn zoom-camera
  [camera-map scroll-distance]
  (let [current-value (:fov camera-map)]
    (cam/perspective-camera (assoc camera-map :fov (min 140 (+ current-value (* current-value scroll-distance 5.0E-4)))))))

(defonce scroll-event
  (.addEventListener (.getElementById js/document "main") "wheel" (fn [event] (swap! state/camera zoom-camera (.-deltaY event))) false))

(defn resize-handler [event]
  (let [element (.getElementById js/document "main")
        actual-width (.-clientWidth element)
        actual-height (.-clientHeight element)
        webgl-width (.-width element)
        webgl-height (.-height element)]

;(println actual-width)
;(println actual-height)
;(println webgl-width)
;(println webgl-height)

  ;  (.log js/console event)
    (if-not (or (= actual-width webgl-width) (= actual-height webgl-height))
      (do (swap! state/camera assoc :aspect (rect/rect actual-width actual-height))

            (.-width (.-canvas state/gl-ctx)  actual-height))))
  ;  (println state/camera :aspect)
  (println "aspect second:")
  (println (:aspect @state/camera) )))

(defonce load-listener
  (.addEventListener js/window "load" resize-handler false))

(defonce resize-listener
  (.addEventListener js/window "resize" resize-handler false))

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:span
   [data-layer-buttons]
   [view-selection-buttons]
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
