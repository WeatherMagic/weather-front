(ns weather-magic.state
  (:require
   [weather-magic.models  :as models]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.gl.core   :as gl]
   [weather-magic.shaders :as shaders]
   [thi.ng.geom.vector    :as v :refer [vec2 vec3]]
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
(defonce date-atom (atom {:year  {:value 2016 :min 1950 :max 2100}
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

(defonce model   (atom models/sphere))
(defonce texture (atom nil))

;; Counters for texture loading.
(defonce textures-loaded (volatile! 0))
(defonce textures-to-be-loaded (volatile! 0))
