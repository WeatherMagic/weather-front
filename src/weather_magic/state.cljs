(ns weather-magic.state
  (:require
   [weather-magic.world :as world]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [reagent.core :as reagent :refer [atom]]))

(def earth-animation-fn (atom world/spin))
(def earth-rotation (atom {:xAngle 24.5 :yAngle 0}))

(defonce gl-ctx (gl/gl-context "main"))
(defonce view-rect  (gl/get-viewport-rect gl-ctx))

(defonce camera (atom (cam/perspective-camera {:eye    (vec3 0 0 1.5)
                                               :fov    90
                                               :aspect view-rect})))
