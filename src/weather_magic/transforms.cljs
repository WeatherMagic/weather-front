(ns weather-magic.transforms
  (:require
   [clojure.walk                   :refer [postwalk]]
   [thi.ng.geom.core       :as g]
   [thi.ng.geom.matrix     :as mat :refer [M44]]
   [thi.ng.math.core       :as m]
   [thi.ng.geom.vector     :as v   :refer [vec2 vec3]]
   [thi.ng.math.core       :as m   :refer [PI HALF_PI TWO_PI]]))

(defn north-pole-rotation-around-z
  [earth-transform]
  (let [northpole-x (.-m10 earth-transform)
        northpole-y (.-m11 earth-transform)
        northpole-z (.-m12 earth-transform)
        northpole-y-norm (/ northpole-y (Math/hypot northpole-y northpole-x))]
    (* (Math/acos northpole-y-norm) (Math/sign northpole-x))))

(defn update-alignment-angle
  "Updating how much the globe should be rotated around the z axis to align northpole"
  [x-diff y-diff camera-position nr-of-steps earth-orientation]
  (let [camera-z-pos (aget (.-buf camera-position) 2)
        zoom-level (* (- camera-z-pos 1.1) (/ 4 5))
        future-earth-orientation (-> M44
                                     (g/rotate-z (* (Math/atan2 y-diff x-diff) -1))
                                     (g/rotate-y (m/radians (* (* (Math/hypot y-diff x-diff) zoom-level) 0.1)))
                                     (g/rotate-z (Math/atan2 y-diff x-diff))
                                     (m/* earth-orientation))]
    (/ (north-pole-rotation-around-z future-earth-orientation) nr-of-steps)))

(defn lat-lon-to-uv
  [lat lon]
  (let [u (+ (/ lon 360) 0.5)
        v (+ (/ lat 180) 0.5)]
    (vec2 u v)))

(defn lat-lon-to-model-coords
  "Converting latitude and longitude to model cordinates"
  [lat lon]
  (let [rlat (m/radians lat)
        rlon (m/radians lon)]
    {:x (* (* (Math/cos rlon) (Math/cos rlat)) -1)
     :y (Math/sin rlat)
     :z (* (Math/sin rlon) (Math/cos rlat))}))

(defn model-coords-to-lat-lon
  "Converting latitude and longitude to model cordinates"
  [coord]
  (let [ε 0.001
        xn (aget (.-buf coord) 2)
        yn (aget (.-buf coord) 1)
        zn (* (aget (.-buf coord) 0) -1)]
    {:lat (if (> yn (- 1 ε))
            90
            (if (< yn (- ε 1))
              -90
              (* (/ 180 PI) (Math/asin yn))))
     :lon (if (> (Math/abs zn) ε)
            (* (/ 180 PI) (Math/atan2 xn zn))
            (if (> (Math/abs yn) (- 1 ε))
              0
              (if (pos? xn)
                90
                -90)))}))

(defn get-center-model-coords
  ""
  [earth-orientation]
  (let [matrix (m/invert earth-orientation)
        model-x (.-m20 matrix)
        model-y (.-m21 matrix)
        model-z (.-m22 matrix)]
    (vec3 model-x model-y model-z)))

(defn get-lat-lon-map
  "Get the lat and lon on the format from lat/lon to lat/lon."
  [earth-orientation camera]
  ;; Truncate all numbers into whole integers.
  (let [from-lat -90
        to-lat 90
        from-lon -180
        to-lon 180]
    {:from-latitude from-lat
     :to-latitude to-lat
     :from-longitude from-lon
     :to-longitude to-lon}))

(defn get-texture-position-map
  "Get the positioning and scale information of the area the camera is
  looking at at this moment."
  [lat-lon-map]
  (let [from-uv (lat-lon-to-uv (:from-latitude  lat-lon-map)
                               (:from-longitude lat-lon-map))
        to-uv   (lat-lon-to-uv (:to-latitude    lat-lon-map)
                               (:to-longitude   lat-lon-map))
        scale   (vec2 (- (aget (.-buf to-uv) 0) (aget (.-buf from-uv) 0))
                      (- (aget (.-buf to-uv) 1) (aget (.-buf from-uv) 1)))]
    {:dataPos   (vec2 (aget (.-buf from-uv) 0)
                      (- 1 (aget (.-buf to-uv) 1)))
     :dataScale scale}))
