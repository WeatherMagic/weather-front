(ns weather-magic.ui
  (:require
   [weather-magic.models   :as models]
   [weather-magic.state    :as state]
   [weather-magic.event-handlers :as event-handlers]
   [weather-magic.world    :as world]
   [weather-magic.shaders  :as shaders]
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

(defn go-from-landing-page
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
  [atom left-right-key year-month-key year-month-key-inv]
  (if (:play-mode (year-month-key (left-right-key @atom)))
    (swap! atom update-in [left-right-key year-month-key] merge {:play-mode false :play-mode-before-sliding false})
    (do (swap! atom update-in [left-right-key year-month-key] merge {:play-mode true :play-mode-before-sliding true})
        (swap! atom update-in [left-right-key year-month-key-inv] merge {:play-mode false :play-mode-before-sliding false}))))

(defn button
  "Creates a button with a given HTML id which when clicked does func on atom with args."
  [name id class func atom & args]
  [:input {:type "button" :value name :id id :class class
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
    [button "Data-selection" "" "selection-button" swap! state/data-menu-visible hide-unhide]]
   [:div {:id "data-menu-container" :class (hide-unhide @state/data-menu-visible)}
    [:a {:href "#" :class "closebtn" :value "X" :on-click #(swap! state/data-menu-visible hide-unhide)}]
    [:div {:id "side-menu-button-group-container"}
     [:div {:id "upper-side-menu-button-group"}
      [:select {:class "side-menu-button" :name "Climate Model" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:climate-model] (.-target.value event)))}
       [:option {:value "ICHEC-EC-EARTH"} "Climate model 1"]
       [:option {:value "CNRM-CERFACS-CNRM-CM5"} "Climate model 2"]
       [:option {:value "IPSL-IPSL-CM5A-MR"} "Climate model 3"]]
      [:select {:class "side-menu-button" :name "Exhaust-level" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:exhaust-level] (.-target.value event)))}
       [:option {:value "rcp45"} "Exhaust level 1"]
       [:option {:value "rcp85"} "Exhaust level 2"]]]
     [:div {:id "right-side-menu-offset"}]
     [:div {:id "lower-side-menu-button-group"}
      [button "Temperature" "" "side-menu-button" swap! state/data-layer-atom util/toggle :Temperature]
      [button "Precipitation" "" "side-menu-button" swap! state/data-layer-atom util/toggle :Precipitation]]]]])

(defn navigation-selection
  "Buttons for choosing which data layer to display"
  []
  [:div
   [:div {:id "nav-selection-container" :class (hide-unhide @state/blur-visible)}
    [button "Navigation" "" "selection-button" swap! state/navigation-menu-visible hide-unhide]]
   [:div {:id "navigation-menu-container" :class (hide-unhide @state/navigation-menu-visible)}
    [:a {:href "#" :class "closebtn" :value "X" :on-click #(swap! state/navigation-menu-visible hide-unhide)}]
    [:div {:id "side-menu-button-group-container"}
     [:div {:id "right-upper-side-menu-button-group"}
      [button "Spin-earth" "" "side-menu-button" reset! state/earth-animation-fn world/spin-earth!]
      [button "About" "" "side-menu-button" swap! state/landing-page-visible hide-unhide]]
     [:div {:id "right-lower-side-menu-button-group"}
      [button "Europe" "" "side-menu-button" set-static-view (vec3 45 80 0)]
      [button "Africa" "" "side-menu-button" set-static-view (vec3 5 75 0)]
      [button "South America" "" "side-menu-button" set-static-view (vec3 -20 150 0)]
      [button "North America" "" "side-menu-button" set-static-view (vec3 35 190 0)]
      [button "Oceania" "" "side-menu-button" set-static-view (vec3 -15 -40 0)]
      [button "Asia" "" "side-menu-button" set-static-view (vec3 35 -15 0)]]]]])

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
   [button "To map" "" "intro-button" go-from-landing-page]])

(defn map-ui
  "The UI displayed while the user interacts with the map."
  []
  [:div
   [data-selection]
   [navigation-selection]
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
