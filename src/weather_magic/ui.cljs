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

(defonce animation-key-holder (atom {:spin-earth world/spin-earth! :europe world/show-europe!}))

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
  [atom key]
  (if (:play-mode (key @atom))
    (swap! atom update-in [key] merge {:play-mode false :play-mode-before-sliding false})
    (swap! atom update-in [key] merge {:play-mode true :play-mode-before-sliding true})))

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
    [:div {:id "data-selection-container" :class (hide-unhide @state/blur-visible)}
      [button "Data-selection" "" "selection-button" toggle-side-menu-visibility state/data-menu-visible]]
    [:div {:id "data-menu-container" :class (hide-unhide @state/data-menu-visible)}
      [:a {:href "#" :class "closebtn" :value "X" :on-click #(toggle-side-menu-visibility state/data-menu-visible)}]
      [:div {:id "side-menu-button-group"}
       [:select {:class "side-menu-button" :name "Climate Model" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:climate-model] (.-target.value event)))}
        [:option {:value "ICHEC-EC-EARTH"} "Climate model 1"]
        [:option {:value "CNRM-CERFACS-CNRM-CM5"} "Climate model 2"]
        [:option {:value "IPSL-IPSL-CM5A-MR"} "Climate model 3"]]
       [:select {:class "side-menu-button" :name "Exhaust-level" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:exhaust-level] (.-target.value event)))}
        [:option {:value "rcp45"} "Exhaust level 1"]
        [:option {:value "rcp85"} "Exhaust level 2"]]
       [button "Temperature" "" "side-menu-button" swap! state/data-layer-atom util/toggle :Temperature]
       [button "Precipitation" "" "side-menu-button" swap! state/data-layer-atom util/toggle :Precipitation]]]])

(defn navigation-selection
  "Buttons for choosing which data layer to display"
  []
  [:div
   [:div {:id "nav-selection-container" :class (hide-unhide @state/blur-visible)}
    [button "Navigation" "" "selection-button" toggle-side-menu-visibility state/navigation-menu-visible]]
   [:div {:id "navigation-menu-container" :class (hide-unhide @state/navigation-menu-visible)}
    [:a {:href "#" :class "closebtn" :value "X" :on-click #(toggle-side-menu-visibility state/navigation-menu-visible)}]
    [:div {:id "side-menu-button-group"}
     [:select {:class "side-menu-button" :name "Climate Model"}
      [:option {:value ":spin-earth"} "Spin earth"]
      [:option {:value ":europe"} "Europe"]]
     [button "Europe" "" "side-menu-button" reset! state/earth-animation-fn world/show-europe!]
     [button "Asia" "" "side-menu-button" reset! state/earth-animation-fn world/show-asia!]
     [button "Oceania" "" "side-menu-button" reset! state/earth-animation-fn world/show-oceania!]
     [button "Africa" "" "side-menu-button" reset! state/earth-animation-fn world/show-africa!]
     [button "South America" "" "side-menu-button" reset! state/earth-animation-fn world/show-south-america!]
     [button "North America" "" "side-menu-button" reset! state/earth-animation-fn world/show-north-america!]]]])

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
