(ns weather-magic.ui
  (:require
   [weather-magic.models   :as models]
   [weather-magic.state    :as state]
   [weather-magic.event-handlers :as event-handlers]
   [weather-magic.world    :as world]
   [weather-magic.shaders  :as shaders]
   [thi.ng.geom.gl.camera :as cam]
   [weather-magic.util     :as util]
   [reagent.core           :as reagent :refer [atom]]
   [thi.ng.geom.vector     :as v       :refer [vec2 vec3]]
   [thi.ng.geom.gl.shaders :as sh]))

(enable-console-print!)

(defn set-static-view
  [rot-coords]
  (reset! state/earth-animation-fn world/show-static-view!)
  (reset! state/static-scene-coordinates rot-coords))

(defn hide-unhide
  "Returns the inverse of hidden and visible. If :hidden is given, :visible is returned and vice versa."
  [hidden-or-not]
  (hidden-or-not {:hidden :visible :visible :hidden}))

(defn toggle-about-page
  [atom1 atom2]
  (swap! atom1 hide-unhide)
  (swap! atom2 hide-unhide))

(defn close-side-menu
  [side-menu]
  (swap! side-menu hide-unhide)
  (swap! state/blur-visible (fn [] :hidden))
  (swap! state/about-page-visible (fn [] :hidden)))

(defn go-from-landing-page
  []
  (swap! state/blur-visible (fn [] :hidden))
  (swap! state/landing-page-visible (fn [] :hidden)))

(defn update-climate-model-info
  [key input]
  (swap! state/climate-model-info assoc-in [key] input))

(defn toggle-play-stop
  [atom left-right-key year-month-key year-month-key-inv]
  (if (:play-mode (year-month-key (left-right-key @atom)))
    (swap! atom update-in [left-right-key year-month-key] merge {:play-mode false :play-mode-before-sliding false})
    (do (swap! atom update-in [left-right-key year-month-key] merge {:play-mode true :play-mode-before-sliding true})
        (swap! atom update-in [left-right-key year-month-key-inv] merge {:play-mode false :play-mode-before-sliding false}))))

(defn update-shader-and-data-layer
  [shader data-layer]
  (reset! state/current-shader-key shader)
  (reset! state/data-layer-atom data-layer))

(defn button
  "Creates a button with a given HTML id which when clicked does func on atom with args."
  [name class func atom & args]
  [:input {:type "button"
           :value name
           :id (clojure.string/replace name " " "")
           :class class
           :on-click #(apply func atom args)}])

(defn play-pause-button
  "A button without text"
  [id func atom & args]
  [:input.play-pause {:type "button" :id id
                      :on-click #(apply func atom args)}])

(defn close-button
  "A close button"
  [name class func atom & args]
  [:input.closebtn {:type "button"
                    :value name
                    :id (clojure.string/replace name " " "")
                    :class class
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
    [play-pause-button "LeftYear" toggle-play-stop state/date-atom :left :year :month]
    [slider-component :left :year]
    [play-pause-button "LeftMonth" toggle-play-stop state/date-atom :left :month :year]
    [slider-component :left :month]]
   [:div {:class "time-sliders-right"}
    [play-pause-button "RightYear" toggle-play-stop state/date-atom :right :year :month]
    [slider-component :right :year]
    [play-pause-button "RightMonth" toggle-play-stop state/date-atom :right :month :year]
    [slider-component :right :month]]])

(defn map-ui-blur []
  "What hides the map UI."
  [:div {:class @state/blur-visible :id "blur"}])

(defn data-selection
  "Buttons for choosing which data layer to display"
  []
  [:div
   [:div {:id "data-selection-container" :class (hide-unhide @state/blur-visible)}
    [button "Data-selection" "selection-button" swap! state/data-menu-visible hide-unhide]]
   [:div {:id "data-menu-container" :class (hide-unhide @state/data-menu-visible)}
    [:div {:id "closebtn" :class "data"}
     [close-button "x" "side-menu-button" close-side-menu state/data-menu-visible]]
    [:div {:id "side-menu-button-group-container"}
     [:div {:id "upper-side-menu-button-group"}
      [:select {:class "side-menu-button" :name "Climate Model" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:climate-model] (.-target.value event)))}
       [:option {:value "ICHEC-EC-EARTH"} "Climate model 1"]
       [:option {:value "CNRM-CERFACS-CNRM-CM5"} "Climate model 2"]
       [:option {:value "IPSL-IPSL-CM5A-MR"} "Climate model 3"]]
      [:select {:class "side-menu-button" :name "Exhaust-level" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:exhaust-level] (.-target.value event)))}
       [:option {:value "rcp45"} "Exhaust level 1"]
       [:option {:value "rcp85"} "Exhaust level 2"]
       [:option {:value "historical"} "Historical"]]]
     [:div {:id "right-side-menu-offset"}]
     [:div {:id "lower-side-menu-button-group"}
      [button "Standard" "side-menu-button" update-shader-and-data-layer :standard "temperature"]
      [button "Temperature" "side-menu-button" update-shader-and-data-layer :temperature "temperature"]
      [button "Precipitation" "side-menu-button" update-shader-and-data-layer :precipitation "precipitation"]]]]])

(defn navigation-selection
  "Buttons for navigation"
  []
  [:div
   [:div {:id "nav-selection-container" :class (hide-unhide @state/blur-visible)}
    [button "Navigation" "selection-button" swap! state/navigation-menu-visible hide-unhide]]
   [:div {:id "navigation-menu-container" :class (hide-unhide @state/navigation-menu-visible)}
    [:div {:id "closebtn" :class "nav"}
     [close-button "x" "side-menu-button" close-side-menu state/navigation-menu-visible]]
    [:div {:id "side-menu-button-group-container"}
     [:div {:id "right-upper-side-menu-button-group"}
      [button "Spin-earth" "side-menu-button" reset! state/earth-animation-fn world/spin-earth!]
      [button "About" "side-menu-button" toggle-about-page state/about-page-visible state/blur-visible]]
     [:div {:id "right-lower-side-menu-button-group"}
      [button "Arctic" "side-menu-button" world/get-to-view-angles 0.0 1.0 0.0 false]
      [button "Europe" "side-menu-button" world/get-to-view-angles -0.6378739 0.7512540 0.1695120 true]
      [button "Africa" "side-menu-button" world/get-to-view-angles -0.9418886 0.0472268 0.3325892 true]
      [button "South America" "side-menu-button" world/get-to-view-angles -0.4850580 -0.3197941 -0.8139106 true]
      [button "North America" "side-menu-button" world/get-to-view-angles 0.1276255 0.7026068 -0.7000396 true]
      [button "Oceania" "side-menu-button" world/get-to-view-angles 0.5729999 -0.2510875 0.7801449 true]
      [button "Asia" "side-menu-button" world/get-to-view-angles 0.1583381 0.5093238 0.8458831 true]
      [button "Antarctica" "side-menu-button" world/get-to-view-angles 0.0 -1.0 0.0 false]]]]])

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
   [button "To map" "intro-button" go-from-landing-page]])

(defn about-page
  "What the user sees when she arrives at the page."
  []
  [:div {:id "landing-page" :class @state/about-page-visible}
   [:div
    [:h1 "What makes mangel mangel"]
    [:p "Mangel is the nickname of one of the group members"]
    [:p "Mangel likes to watch documentaries"]
    [:p "Mangel is the same person as Magnuzo"]
    [:p "Mangel is love."]
    [:p "Mangel is life"]]])

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:div
   [data-selection]
   [navigation-selection]
   [compass]
   [landing-page]
   [about-page]
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
