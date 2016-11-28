(ns weather-magic.state
  (:require
   [weather-magic.models   :as models]
   [weather-magic.shaders  :as shaders]
   [weather-magic.textures :as textures]
   [thi.ng.geom.gl.camera  :as cam]
   [thi.ng.geom.gl.core    :as gl]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.vector     :as v   :refer [vec2 vec3]]
   [reagent.core                   :refer [atom]]
   [thi.ng.geom.matrix     :as mat :refer [M44]]))

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

(defonce corners (atom {:upper-left (vec3 0 0 0) :upper-right (vec3 0 0 0) :lower-left (vec3 0 0 0) :lower-right (vec3 0 0 0)}))                          

;; The function currently animating the earth.
(defonce earth-animation-fn (atom nil))

;; The current rotation of earth.
(defonce earth-orientation (atom M44))

;; Whether or not the landing page is visible.
(defonce intro-visible (atom :visible))

(defonce model        (atom models/sphere))

(defonce textures     (atom (textures/load-base-textures gl-ctx)))
(defonce base-texture (atom (:earth @textures)))

(def shaders {:standard (sh/make-shader-from-spec gl-ctx shaders/standard-shader-spec)
              :blend    (sh/make-shader-from-spec gl-ctx shaders/blend-shader-spec)
              :temp     (sh/make-shader-from-spec gl-ctx shaders/temperature-shader-spec)})
(defonce current-shader-key (atom :standard))

;; Used for determining frame delta, the time between each frame.
(defonce time-of-last-frame (volatile! 0))
