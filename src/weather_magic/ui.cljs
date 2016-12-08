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

(defn go-from-landing-poage
  []
  (swap! state/blur-visible hide-unhide)
  (swap! state/landing-page-visible hide-unhide))

(defn toggle-side-menu-visibility
  [atom]
  (swap! state/blur-visible hide-unhide)
  (swap! atom hide-unhide))

(defn update-climate-model-info
  [key input]
  (println key)
  (println input)
  (swap! state/climate-model-info assoc-in [key] input))

(defn toggle-play-stop
  [atom key]
  (if (:play-mode (key @atom))
    (swap! atom update-in [key] merge {:play-mode false :play-mode-before-sliding false})
    (swap! atom update-in [key] merge {:play-mode true :play-mode-before-sliding true})))

(defn button
  "Creates a button with a given HTML id which when clicked does func on atom with args."
  [name id func atom & args]
  [:input {:type "button" :value name :id id
           :on-click #(apply func atom args)}])

(defn play-pause-button
  "A button without text"
  [id func atom & args]
  [:input.play-pause {:type "button" :id id
                      :on-click #(apply func atom args)}])

(defn slider [key1 key2 value min max]
  [:input {:type "range" :value value :min min :max max
           :on-mouseDown  (fn [] (swap! state/date-atom assoc-in [key1 :play-mode] false))
           :on-mouseUp  (fn [] (swap! state/date-atom assoc-in [key1 :play-mode] (:play-mode-before-sliding (key1 @state/date-atom))))
           :on-change (fn [event] (swap! state/date-atom assoc-in [key1 key2 :value]
                                         (int (.-target.value event))))}])

(defn slider-component [key1 key2]
  (let [data (key2 (key1 @state/date-atom))]
    [:div {:class "time-slider"}
     [:span (clojure.string/capitalize (name key2)) ": " (:value data)]
     [slider key1 key2 (:value data) (:min data) (:max data)]]))

(defn time-sliders []
  [:div {:id "time-slider-containers"}
   [:div {:class "time-sliders-left"}
    [play-pause-button "LeftYear" toggle-play-stop state/date-atom :left]
    [slider-component :left :year]
    [play-pause-button "LeftMonth" toggle-play-stop state/date-atom :right]
    [slider-component :left :month]]
   [:div {:class "time-sliders-right"}
    [play-pause-button "RightYear" toggle-play-stop state/date-atom :right]
    [slider-component :right :year]
    [play-pause-button "RightMonth" toggle-play-stop state/date-atom :right]
    [slider-component :right :month]]])

(defn map-ui-blur []
  "What hides the map UI."
  [:div {:class @state/blur-visible :id "blur"}])

(defn data-selection
  "Buttons for choosing which data layer to display"
  []
  [:div
    [:div {:id "data-layer-container" :class (hide-unhide @state/blur-visible)}
      [button "Data-selection" "Data-selection" toggle-side-menu-visibility state/data-menu-visible]]
    [:div {:id "data-menu-container" :class (hide-unhide @state/data-menu-visible)}
      [:div {:id "side-menu-button-group"}
       [:select {:id "Data-selection" :name "Climate Model" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:climate-model] (.-target.value event))) :class "button"}
        [:option {:value "ICHEC-EC-EARTH"} "ICHEC-EC-EARTH"]
        [:option {:value "CNRM-CERFACS-CNRM-CM5"} "CNRM-CERFACS-CNRM-CM5"]
        [:option {:value "IPSL-IPSL-CM5A-MR"} "IPSL-IPSL-CM5A-MR"]]
       [:select {:id "Data-selection" :name "Exhaust-level" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:exhaust-level] (.-target.value event))) :class "button"}
        [:option {:value "rcp45"} "rcp45"]
        [:option {:value "rcp85"} "rcp85"]]
       [button "Temperature" "Data-selection" swap! state/data-layer-atom util/toggle :Temperature]
       [button "Precipitation" "Data-selection" swap! state/data-layer-atom util/toggle :Precipitation]]]])

(defn navigation-selection
  "Buttons for choosing which data layer to display"
  []
  [:div
   [:div {:id "navigation-layer-container" :class (hide-unhide @state/blur-visible)}
    [button "Navigation" "Data-selection" toggle-side-menu-visibility state/navigation-menu-visible]]
   [:div {:id "navigation-menu-container" :class (hide-unhide @state/navigation-menu-visible)}
    [:a {:href "#" :class "closebtn" :value "X" :on-click #(toggle-side-menu-visibility state/navigation-menu-visible)}]
    [:div {:class "side-menu-button-group"}
     [:select {:id "Data-selection" :name "Climate Model" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:climate-model] (.-target.value event)))}
      [:option {:value "ICHEC-EC-EARTH"} "ICHEC-EC-EARTH"]
      [:option {:value "CNRM-CERFACS-CNRM-CM5"} "CNRM-CERFACS-CNRM-CM5"]
      [:option {:value "IPSL-IPSL-CM5A-MR"} "IPSL-IPSL-CM5A-MR"]]
     [:select {:name "Exhaust-level" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:exhaust-level] (.-target.value event))) :class "button"}
      [:option {:value "rcp45"} "rcp45"]
      [:option {:value "rcp85"} "rcp85"]]
     [button "Temperature" "Data-selection" swap! state/data-layer-atom util/toggle :Temperature]
     [button "Precipitation" "Data-selection"   swap! state/data-layer-atom util/toggle :Precipitation]]]])

(defn view-selection-buttons
  "Buttons for choosing view"
  []
  [:div {:id "view-selection-container" :class (hide-unhide @state/blur-visible)}
   [button "Europe" reset! state/earth-animation-fn world/show-europe!]
   [button "About"  swap!  state/blur-visible hide-unhide]])

(defn compass []
  [:input {:type "button" :id "Compass" :class (hide-unhide @state/blur-visible)
           :on-click event-handlers/align-handler
           :style {:transform (str "rotate(" (util/north-pole-rotation-around-z @state/earth-orientation) "rad)")}}])

(defn landing-page
  "What the user sees when she arrives at the page."
  []
  [:div {:id "landing-page" :class @state/landing-page-visible}
   [:div
    [:h1 "Welcome to WeatherMagic!"]
    [:p "An interactive visualization of climate projections"]
    [:p "or How fucked art thou?"]]
   [button "To map" "To map" go-from-landing-poage]])

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:div
   [data-selection]
   [compass]
   [landing-page]
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
