(ns weather-magic.state
  (:require
   [weather-magic.models  :as models]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.gl.core   :as gl]
   [weather-magic.shaders :as shaders]
   [weather-magic.textures :as textures]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.vector    :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix    :as mat :refer [M44]]
   [reagent.core          :refer [atom]]))

;; Our WebGL context, given by the browser.
(defonce gl-ctx-left (gl/gl-context "left-canvas"))

;; Another WebGL context
(defonce gl-ctx-right (gl/gl-context "right-canvas"))

;; How WebGL figures out its aspect ratio.
(defonce view-rect-left  (gl/get-viewport-rect gl-ctx-left))

;; How WebGL figures out its aspect ratio.
(defonce view-rect-right  (gl/get-viewport-rect gl-ctx-right))

(defonce camera-left (atom (cam/perspective-camera {:eye    (vec3 0 0 1.5)
                                                    :fov    110
                                                    :aspect (gl/get-viewport-rect gl-ctx-left)})))

(defonce camera-right (atom (cam/perspective-camera {:eye    (vec3 0 0 1.5)
                                                     :fov    110
                                                     :aspect (gl/get-viewport-rect gl-ctx-right)})))

;; What data is being displayed on the map right now?
(defonce data-layer-atom (atom #{}))

;; User input from the time slider UI.
(defonce date-atom (atom {:year  {:value 1950 :min 1950 :max 2100}
                          :month {:value 1 :min 1 :max 12}}))

;; The function currently animating the earth.
(defonce earth-animation-fn (atom nil))

;; The current rotation of earth.
(defonce earth-orientation (atom M44))

;; Whether or not the landing page is visible.
(defonce intro-visible (atom :visible))

(defonce model        (atom models/sphere))

(defonce textures-left     (atom (textures/load-base-textures gl-ctx-left)))

(defonce textures-right     (atom (textures/load-base-textures gl-ctx-right)))

(defonce base-texture-left (atom (:earth @textures-left)))

(defonce base-texture-right (atom (:earth @textures-right)))

(def shaders-left {:standard (sh/make-shader-from-spec gl-ctx-left shaders/standard-shader-spec-left)
                   :blend    (sh/make-shader-from-spec gl-ctx-left shaders/blend-shader-spec-left)
                   :temp     (sh/make-shader-from-spec gl-ctx-left shaders/temperature-shader-spec-left)})

(def shaders-right {:standard (sh/make-shader-from-spec gl-ctx-right shaders/standard-shader-spec-right)
                    :blend    (sh/make-shader-from-spec gl-ctx-right shaders/blend-shader-spec-right)
                    :temp     (sh/make-shader-from-spec gl-ctx-right shaders/temperature-shader-spec-right)})

(defonce current-shader-key (atom :standard))

;; Used for determining frame delta, the time between each frame.
(defonce time-of-last-frame (volatile! 0))
