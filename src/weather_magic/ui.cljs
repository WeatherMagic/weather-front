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

(defn go-from-landing-page
  [data-layer]
  (reset! state/blur-visible :hidden)
  (reset! state/landing-page-visible :hidden)
  (update-shader-and-data-layer (keyword data-layer) data-layer))

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
     [:h5 (clojure.string/capitalize (name year-month-key)) ": " (:value data)]
     [slider left-right-key year-month-key (:value data) (:min data) (:max data)]]))

(defn time-sliders []
  [:div {:id "time-slider-containers"}
   [:table {:class "time-sliders-left"}
    [:tr
     [:td [play-pause-button "LeftYear" toggle-play-stop state/date-atom :left :year :month]]
     [:td [slider-component :left :year]]]
    [:tr
     [:td [play-pause-button "LeftMonth" toggle-play-stop state/date-atom :left :month :year]]
     [:td [slider-component :left :month]]]]
   [:table {:class "time-sliders-right"}
    [:tr
     [:td [play-pause-button "RightYear" toggle-play-stop state/date-atom :right :year :month]]
     [:td [slider-component :right :year]]]
    [:tr
     [:td [play-pause-button "RightMonth" toggle-play-stop state/date-atom :right :month :year]]
     [:td [slider-component :right :month]]]]])

(defn map-ui-blur []
  "What hides the map UI."
  [:div {:class @state/blur-visible :id "blur"}])

(defn data-selection
  "Buttons for choosing which data layer to display"
  []
  [:div
   [:div {:id "data-selection-container" :class (hide-unhide @state/blur-visible)}
    [button "Data-selection" "selection-button" swap! state/data-menu-visible hide-unhide]]
   [:div {:id "data-menu-container" :class (str (name (hide-unhide @state/data-menu-visible)) " sidebar")}
    [close-button "x" "side-menu-button" close-side-menu state/data-menu-visible]
    [:h4 "Climate model"]
    [:select {:class "side-menu-button" :name "Climate Model" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:climate-model] (.-target.value event)))}
     [:option {:value "ICHEC-EC-EARTH"} "ICHEC-EC-EARTH"]
     [:option {:value "CNRM-CERFACS-CNRM-CM5"} "CNRM-CNRM-CM5"]
     [:option {:value "IPSL-IPSL-CM5A-MR"} "IPSL-IPSL-CM5A-MR"]]
    [:input {:type "button" :value "?" :class "help"}]
    [:div {:class "hidden-helper"}
     [:p "These are different climate prediction models produced by climate-institutes across the world. These models takes a lot of different in-parametres, levels of green house gases is one among them, and then simulates climate over the coming century."]
     [:ul
      [:li "ICHEC-EC-EARTH is an Irish model from the weather institute ICHEC."]
      [:li "CNRM-CERFACS-CNRM-CM5 is a french model from CNRM."]
      [:li "IPSL-IPSL-CM5A-MR is a french climate model from the institude IPSL."]]]
    [:h4 "Exhaust level"]
    [:select {:class "side-menu-button" :name "Exhaust-level" :on-change (fn [event] (swap! state/climate-model-info assoc-in [:exhaust-level] (.-target.value event)))}
     [:option {:value "rcp45"} "RCP 4.5"]
     [:option {:value "rcp85"} "RCP 8.5"]]
    [:input {:type "button" :value "?" :class "help"}]
    [:div {:class "hidden-helper"}
     [:p "These are different exhaust-levels of green house gases (GHGs) for which climate institutes predicts the future around. Both RCP4.5 and RCP8.5 are seen as likely cases, with RCP8.5 beeing a higher level of GHGs than RCP4.5. Try experimenting with these options and see how they affect the predicted climate of the earth. "]
     [:p "You can read more about these prediction models on " [:a {:href "https://en.wikipedia.org/wiki/Representative_Concentration_Pathways"} "Wikipedia"]]]
    [:h4 "Data type"]
    [button "No data" "side-menu-button" update-shader-and-data-layer :standard "temperature"]
    [button "Temperature" "side-menu-button" update-shader-and-data-layer :temperature "temperature"]
    [button "Precipitation" "side-menu-button" update-shader-and-data-layer :precipitation "precipitation"]]])

(defn navigation-selection
  "Buttons for navigation"
  []
  [:div
   [button "Navigation" "selection-button" swap! state/navigation-menu-visible hide-unhide]
   [:div {:id "navigation-menu-container" :class (str (name (hide-unhide @state/navigation-menu-visible)) " sidebar")}
    [close-button "x" "side-menu-button" close-side-menu state/navigation-menu-visible]
    [:h4 "Location"]
    [button "Africa" "side-menu-button" world/get-to-view-angles -0.9418886 0.0472268 0.3325892 true]
    [button "Antarctica" "side-menu-button" world/get-to-view-angles 0.0 -1.0 0.0 false]
    [button "Arctic" "side-menu-button" world/get-to-view-angles 0.0 1.0 0.0 false]
    [button "Asia" "side-menu-button" world/get-to-view-angles 0.1583381 0.5093238 0.8458831 true]
    [button "Europe" "side-menu-button" world/get-to-view-angles -0.6378739 0.7512540 0.1695120 true]
    [button "North America" "side-menu-button" world/get-to-view-angles 0.1276255 0.7026068 -0.7000396 true]
    [button "Oceania" "side-menu-button" world/get-to-view-angles 0.5729999 -0.2510875 0.7801449 true]
    [button "South America" "side-menu-button" world/get-to-view-angles -0.4850580 -0.3197941 -0.8139106 true]
    [:h4 "Other"]
    [button "Spin-earth" "side-menu-button" reset! state/earth-animation-fn world/spin-earth!]
    [button "About" "side-menu-button" toggle-about-page state/about-page-visible state/blur-visible]]])

(defn compass []
  [:input {:type "button" :id "Compass" :class (hide-unhide @state/blur-visible)
           :on-click event-handlers/align-handler
           :style {:transform (str "rotate(" (util/north-pole-rotation-around-z @state/earth-orientation) "rad)")}}])

(defn landing-page
  "What the user sees when she arrives at the page."
  []
  [:div {:id "landing-page" :class (str (name @state/landing-page-visible) " full-page")}
   [:div
    [:h1 "Welcome to WeatherMagic!"]
    [:p "An interactive visualization of climate projections."
     "Here you can see the results of climate simulations from many institutes that are a part of the " [:a {:href "http://esgf.llnl.gov"} "ESGF"] "."]
    [:p "This software is made by engineering students at Linköping University as a CDIO project. If you are interested in this software, please contact any of the " [:a {:href "https://github.com/orgs/WeatherMagic/people"} "authors"] "."]]
   [:div
    [:h2 "What do you want to see?"]
    [button "Temperature"   "intro-button" go-from-landing-page "temperature"]
    [button "Precipitation" "intro-button" go-from-landing-page "precipitation"]]])

(defn about-page
  []
  [:div {:id "about-page" :class (str (name @state/about-page-visible) " full-page")}
   [close-button "x" "side-menu-button" toggle-about-page state/about-page-visible state/blur-visible]
   [:h1 "WeatherMagic"]
   [:p "A project built by the dedicated team consisting of:"]
   [:ul
    [:li "Alexander Poole"]
    [:li "Christian Luckey"]
    [:li "Hans-Filip Elo"]
    [:li "Magnus Ivarsson"]
    [:li "Magnus Wedberg"]
    [:li "Maja Ilestrand"]]
   [:p [:a {:href "https://github.com/WeatherMagic"} "WeatherMagic"] " was created by last year engineering students as a " [:a {:href "https://www.lith.liu.se/presentation/namnder/kb/protokoll-och-studentinformation/kb-protokoll/mars-2016/1.678546/KB_160316.pdf"} "CDIO"] " project at Linköping University. This software is created during a technical project with a goal of giving students, and others, a higher understanding of climate modelling as well as climate change. The project has delivered this front-end, called " [:a {:href "https://github.com/WeatherMagic/weather-front"} "Weather-Front"] " as well as a back-end software, called " [:a {:href "https://github.com/WeatherMagic/thor/"} "Thor"] ", which delivers data from climate simulations done by SMHI and other weather institutes."]
   [:h2 "Technologies"]
   [:p "These softwares are built using the following technologies. "]
   [:h3 "Weather-front"]
   [:ul
    [:li "ClojureScript"]
    [:li "Thi.ng Geom"]
    [:li "Figwheel"]
    [:li "WebGL"]]
   [:h3 "Thor"]
   [:ul
    [:li "Python 3"]
    [:li "Flask"]
    [:li "Numpy+scipy"]
    [:li "Memcached"]
    [:li "nginx"]
    [:li "uwsgi"]
    [:li "Pillow"]
    [:li "Climate data from NetCDF-files delivered by " [:a {:href "http://esgf.llnl.gov"} "ESGF"]]]
   [:h2 "Special thanks to"]
   [:ul
    [:li "Ingemar Ragnemalm (LiU) - All makt åt Ingemar, vår befriare."]
    [:li "Ola Leifler (LiU)"]
    [:li "Gustav Strandberg (SMHI)"]]
   [button "Stäng" "side-menu-button" toggle-about-page state/about-page-visible state/blur-visible]])

(defn scale-gradient []
  [:div {:class (str @state/data-layer-atom "-gradient gradient")}])
   
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
   [map-ui-blur]
   [scale-gradient]])

(defn mount-ui!
  "Place the user interface into the DOM."
  []
  ;; We mount the map-ui by default into our UI <span>. If we, for
  ;; example, add a welcome UI later on it might instead be the default
  ;; UI to be mounted.
  (reagent/render [map-ui] (.getElementById js/document "ui"))
  true) ; Return true.
