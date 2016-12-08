(ns weather-magic.transforms
  (:require
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

(defn model-coords-from-corner
  ""
  [x-coord y-coord earth-orientation camera]
  (let [matrix (-> (m/invert earth-orientation)
                   (g/rotate-z (* (Math/atan2 y-coord x-coord) -1))
                   (g/rotate-y (m/radians (* (* (Math/hypot y-coord x-coord)
                                                (:fov camera)) 1.0E-3)))
                   (g/rotate-z (Math/atan2 y-coord x-coord)))
        model-x (.-m20 matrix)
        model-y (.-m21 matrix)
        model-z (.-m22 matrix)]
    (vec3 model-x model-y model-z)))

(defn get-model-coords
  "Returns the model-coords-boundaries."
  [earth-orientation camera]
  (let [canvas-element (.getElementById js/document "left-canvas")
        canvas-width (.-clientWidth canvas-element)
        canvas-height (.-clientHeight canvas-element)
        half-width (/ canvas-width 2)
        half-height (/ canvas-height 2)]
    {:upper-left (model-coords-from-corner (* half-width -1) (* half-height -1) earth-orientation camera)
     :upper-right (model-coords-from-corner half-width (* half-height -1) earth-orientation camera)
     :lower-left (model-coords-from-corner (* half-width -1) half-height earth-orientation camera)
     :lower-right (model-coords-from-corner half-width half-height earth-orientation camera)}))

(defn lat-lon-helper
  "Get model-coords and transform to latitude and longitude"
  [earth-orientation camera]
  (let [model-coords (get-model-coords earth-orientation camera)]
    {:upper-left  (model-coords-to-lat-lon (:upper-left  model-coords))
     :upper-right (model-coords-to-lat-lon (:upper-right model-coords))
     :lower-left  (model-coords-to-lat-lon (:lower-left  model-coords))
     :lower-right (model-coords-to-lat-lon (:lower-right model-coords))}))

(defn get-lat-lon-map
  "Get the lat and lon on the format from lat/lon to lat/lon"
  [earth-orientation camera]
  (let [coords (lat-lon-helper earth-orientation camera)]
    {:from-latitude  (min (:lat (:upper-left coords)) (:lat (:upper-right coords))
                          (:lat (:lower-left coords)) (:lat (:lower-right coords)))
     :to-latitude    (max (:lat (:upper-left coords)) (:lat (:upper-right coords))
                          (:lat (:lower-left coords)) (:lat (:lower-right coords)))
     :from-longitude (min (:lon (:upper-left coords)) (:lon (:upper-right coords))
                          (:lon (:lower-left coords)) (:lon (:lower-right coords)))
     :to-longitude   (max (:lon (:upper-left coords)) (:lon (:upper-right coords))
                          (:lon (:lower-left coords)) (:lon (:lower-right coords)))}))

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
