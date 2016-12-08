(ns weather-magic.ui
  (:require
   [weather-magic.models   :as models]
   [weather-magic.state    :as state]
   [weather-magic.event-handlers :as event-handlers]
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

(defn toggle-play-stop
  [atom left-right-key year-month-key]
  (if (:play-mode (year-month-key (left-right-key @atom)))
    (swap! atom update-in [left-right-key year-month-key] merge {:play-mode false :play-mode-before-sliding false})
    (swap! atom update-in [left-right-key year-month-key] merge {:play-mode true :play-mode-before-sliding true})))

(defn button
  "Creates a button with a given HTML id which when clicked does func on atom with args."
  [id func atom & args]
  [:input {:type "button" :value id :id id :class "button"
           :on-click #(apply func atom args)}])

(defn play-pause-button
  "A button without text"
  [id func atom & args]
  [:input.play-pause {:type "button" :id id
                      :on-click #(apply func atom args)}])

(defn slider [left-right-key year-month-key value min max]
  [:input {:type "range" :value value :min min :max max
           :on-mouseDown  (fn [] (swap! state/date-atom assoc-in [left-right-key year-month-key :play-mode] false))
           :on-mouseUp  (fn [] (swap! state/date-atom assoc-in [left-right-key year-month-key :play-mode] (:play-mode-before-sliding (year-month-key (left-right-key @state/date-atom)))))
           :on-change (fn [event] (swap! state/date-atom assoc-in [left-right-key year-month-key :value]
                                         (int (.-target.value event))))}])

(defn slider-component [left-right-key year-month-key]
  (let [data (year-month-key (left-right-key @state/date-atom))]
    [:div {:class "time-slider"}
     [:span (clojure.string/capitalize (name year-month-key)) ": " (:value data)]
     [slider left-right-key year-month-key (:value data) (:min data) (:max data)]]))

(defn time-sliders []
  [:div {:id "time-slider-containers"}
   [:div {:class "time-sliders-left"}
    [play-pause-button "LeftYear" toggle-play-stop state/date-atom :left :year]
    [slider-component :left :year]
    [play-pause-button "LeftMonth" toggle-play-stop state/date-atom :left :month]
    [slider-component :left :month]]
   [:div {:class "time-sliders-right"}
    [play-pause-button "RightYear" toggle-play-stop state/date-atom :right :year]
    [slider-component :right :year]
    [play-pause-button "RightMonth" toggle-play-stop state/date-atom :right :month]
    [slider-component :right :month]]])

(defn map-ui-blur []
  "What hides the map UI."
  [:div {:class @state/intro-visible :id "blur"}])

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
   [button "Align" event-handlers/align-handler]
   [button "Reset!" event-handlers/reset-spin-handler]])

(defn shader-selection-buttons
  "Buttons for choosing shader"
  []
  [:div {:id "shader-selection-container"}
   [button "Go to map"          swap!  state/intro-visible  hide-unhide]
   [button "Standard shader"    reset! state/current-shader-key :standard]
   [button "Blend shader"       reset! state/current-shader-key :blend]
   [button "Temperature shader" reset! state/current-shader-key :temp]])

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:div
   [data-layer-buttons]
   [view-selection-buttons]
   [shader-selection-buttons]
   [time-sliders]
   [map-ui-blur]])

(defn mount-ui!
  "Place the user interface into the DOM."
  []
  ;; We mount the map-ui by default into our UI <span>. If we, for
  ;; example, add a welcome UI later on it might instead be the default
  ;; UI to be mounted.
  (reagent/render [map-ui] (.getElementById js/document "ui"))
  true) ; Return true.
