(ns weather-magic.state
  (:require
   [weather-magic.models  :as models]
   [weather-magic.textures  :as textures]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.gl.core   :as gl]
   [weather-magic.shaders :as shaders]
   [thi.ng.geom.vector    :as v :refer [vec2 vec3]]
   [reagent.core          :refer [atom]]))

;; Our WebGL context, given by the browser.
(defonce gl-ctx (gl/gl-context "main"))

;; How WebGL figures out its aspect ratio.
(defonce view-rect  (gl/get-viewport-rect gl-ctx))

(defonce camera (atom (cam/perspective-camera {:eye    (vec3 0 0 1.5)
                                               :fov    110
                                               :aspect (gl/get-viewport-rect gl-ctx)})))

;; What data is being displayed on the map right now?
(defonce data-layer-atom (atom #{}))

;; User input from the time slider UI.
(defonce date-atom (atom {:year  {:value 1950 :min 1950 :max 2100}
                          :month {:value 1 :min 1 :max 12}}))

;; The function currently animating the earth.
(defonce earth-animation-fn (atom nil))
;; The current rotation of earth.
(defonce earth-orientation (atom {:x-angle     24.5
                                  :y-angle     0
                                  :z-angle     0
                                  :translation (vec3 0 0 0)}))

;; Whether or not the landing page is visible.
(defonce intro-visible (atom :visible))

(defonce model        (atom models/sphere))

(defonce textures     (atom (textures/load-base-textures gl-ctx)))
(defonce base-texture (atom (:earth @textures)))

(defonce current-shader (atom shaders/standard-shader-spec))
