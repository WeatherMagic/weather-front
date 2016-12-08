(ns weather-magic.state
  (:require
   [weather-magic.models           :as models]
   [weather-magic.shaders          :as shaders]
   [weather-magic.textures         :as textures]
   [thi.ng.geom.gl.camera          :as cam]
   [thi.ng.geom.gl.core            :as gl]
   [thi.ng.geom.gl.shaders         :as sh]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.vector             :refer [vec2 vec3]]
   [thi.ng.geom.matrix             :refer [M44]]
   [reagent.core                   :refer [atom]]))

;; Our WebGL context, given by the browser.
(defonce gl-ctx-left  (gl/gl-context "left-canvas"))
(defonce gl-ctx-right (gl/gl-context "right-canvas"))

;; Canvas sizes.
(defonce view-rect-left  (gl/get-viewport-rect gl-ctx-left))
(defonce view-rect-right (gl/get-viewport-rect gl-ctx-right))

(defonce camera-left (atom (cam/perspective-camera {:eye    (vec3 0 0 3.0)
                                                    :fov    70
                                                    :aspect (gl/get-viewport-rect gl-ctx-left)})))

(defonce camera-right (atom (cam/perspective-camera {:eye    (vec3 0 0 3.0)
                                                     :fov    70
                                                     :aspect (gl/get-viewport-rect gl-ctx-right)})))

(defonce background-camera-left (atom (cam/perspective-camera {:eye    (vec3 0 0 5.0)
                                                               :fov    70
                                                               :aspect (gl/get-viewport-rect gl-ctx-left)})))

(defonce background-camera-right (atom (cam/perspective-camera {:eye    (vec3 0 0 5.0)
                                                                :fov    70
                                                                :aspect (gl/get-viewport-rect gl-ctx-right)})))

;; What data is being displayed on the map right now?
(defonce data-layer-atom (atom #{}))

;; User input from the time slider UI.
(defonce date-atom (atom {:left  {:play-mode false
                                  :play-mode-before-sliding false
                                  :year  {:value 1950 :min 1950 :max 2100}
                                  :month {:value 1 :min 1 :max 12}}
                          :right {:play-mode false
                                  :play-mode-before-sliding false
                                  :year  {:value 1950 :min 1950 :max 2100}
                                  :month {:value 1 :min 1 :max 12}}}))

;; The function currently animating the earth.
(defonce earth-animation-fn (atom nil))

;; The current rotation of earth.
(defonce earth-orientation (atom M44))

;; Whether or not the landing page is visible.
(defonce intro-visible (atom :visible))

(defonce side-menu-visible (atom :visible))

(defonce climate-model-info (atom {:climate-model "ICHEC-EC-EARTH" :exhaust-level "rcp45"}))

;; The models with buffers prepared and ready for use by the program.
(def models {:left  {:sphere (gl/make-buffers-in-spec models/sphere gl-ctx-left  glc/static-draw)
                     :plane  (gl/make-buffers-in-spec models/plane  gl-ctx-left  glc/static-draw)}
             :right {:sphere (gl/make-buffers-in-spec models/sphere gl-ctx-right glc/static-draw)
                     :plane  (gl/make-buffers-in-spec models/plane  gl-ctx-right glc/static-draw)}})

(defonce current-model-key (atom :sphere))

(defonce textures-left        (atom (textures/load-base-textures gl-ctx-left)))
(defonce textures-right       (atom (textures/load-base-textures gl-ctx-right)))

(defonce base-texture-left    (atom (:earth @textures-left)))
(defonce base-texture-right   (atom (:earth @textures-right)))

(defonce dynamic-texture-keys (atom {:current (do (textures/load-data-into-atom-and-return-key! textures-left gl-ctx-left)
                                                  (textures/load-data-into-atom-and-return-key! textures-right gl-ctx-right))}))

(def shaders-left  {:space    (sh/make-shader-from-spec gl-ctx-left  shaders/space-shader-spec)
                    :standard (sh/make-shader-from-spec gl-ctx-left  shaders/standard-shader-spec)
                    :blend    (sh/make-shader-from-spec gl-ctx-left  shaders/blend-shader-spec)
                    :temp     (sh/make-shader-from-spec gl-ctx-left  shaders/temperature-shader-spec)})
(def shaders-right {:space    (sh/make-shader-from-spec gl-ctx-right shaders/space-shader-spec)
                    :standard (sh/make-shader-from-spec gl-ctx-right shaders/standard-shader-spec)
                    :blend    (sh/make-shader-from-spec gl-ctx-right shaders/blend-shader-spec)
                    :temp     (sh/make-shader-from-spec gl-ctx-right shaders/temperature-shader-spec)})
(defonce current-shader-key (atom :standard))

;; Used for determining frame delta, the time between each frame.
(defonce time-of-last-frame (volatile! 0))

(defonce model-coords (atom {:upper-left (vec3 0 0 0) :upper-right (vec3 0 0 0)
                             :lower-left (vec3 0 0 0) :lower-right (vec3 0 0 0)}))

(defonce lat-lon-coords (atom {:from-lat 0 :to-lat 0 :from-lon 0 :to-lon 0}))

(defonce pointer-zoom-info (atom {:delta-x 0 :delta-y 0 :total-steps 200 :current-step 0 :delta-zoom 0 :delta-z-angle 0}))

(defonce year-update (atom {:left {:time-of-last-update 0}
                            :right {:time-of-last-update 0}}))

(defonce texture-info (atom {:dataPos   (vec2 0 0)
                             :dataScale (vec2 0 0)}))

(defonce space-offset (atom (vec2 0 0)))

(defonce pan-speed (atom {:speed 0 :rel-x 0 :rel-y 0}))
