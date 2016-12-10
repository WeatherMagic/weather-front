(ns weather-magic.transforms
  (:require
   [clojure.walk                   :refer [postwalk]]
   [thi.ng.geom.core       :as g]
   [thi.ng.geom.matrix     :as mat :refer [M44]]
   [thi.ng.math.core       :as m]
   [thi.ng.geom.vector     :as v   :refer [vec2 vec3]]
   [thi.ng.math.core       :as m   :refer [PI HALF_PI TWO_PI]]))

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
  (let [center-model-coords (get-center-model-coords earth-orientation)
        center-lat-lon-coords (model-coords-to-lat-lon center-model-coords)
        lat-coords (* (Math/round (/ (:lat center-lat-lon-coords) 10)) 10)
        lon-coords (* (Math/round (/ (:lon center-lat-lon-coords) 10)) 10)
        camera-z-pos (aget (.-buf (:eye camera)) 2)
        zoom-level (/ (- camera-z-pos 1.1) 3.0)
        zoom-rect (if (> zoom-level 0.05) 25 15)
        from-lat (if (> zoom-level 0.1) -90 (max (- lat-coords zoom-rect) -90))
        to-lat (if (> zoom-level 0.1) 90 (min (+ lat-coords zoom-rect) 90))
        from-lon (if (> zoom-level 0.1) -180 (max (- lon-coords (* zoom-rect 2)) -180))
        to-lon (if (> zoom-level 0.1) 180 (min (+ lon-coords (* zoom-rect 2)) 180))]
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

(defn north-pole-rotation-around-z
  [earth-transform]
  (let [northpole-x (.-m10 earth-transform)
        northpole-y (.-m11 earth-transform)
        northpole-z (.-m12 earth-transform)
        northpole-y-norm (/ northpole-y (Math/hypot northpole-y northpole-x))]
    (* (Math/acos northpole-y-norm) (Math/sign northpole-x))))
