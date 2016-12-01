(ns weather-magic.transforms
  (:require
   [weather-magic.state    :as state]
   [weather-magic.world    :as world]
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
  "Updating how much the globe should be rotated around the z axis to align northpole"
  [x-coord y-coord]
  (let [matrix (-> (m/invert @state/earth-orientation)
                   (g/rotate-z (* (Math/atan2 y-coord x-coord) -1))
                   (g/rotate-y (m/radians (* (* (Math/hypot y-coord x-coord) @world/zoom-level) 1.0E-3)))
                   (g/rotate-z (Math/atan2 y-coord x-coord)))
        model-x (.-m20 matrix)
        model-y (.-m21 matrix)
        model-z (.-m22 matrix)]
    (vec3 model-x model-y model-z)))

(defn update-model-coords
  "Updates the model-coords-boundaries"
  []
  (let [canvas-element (.getElementById js/document "left-canvas")
        canvas-width (.-clientWidth canvas-element)
        canvas-height (.-clientHeight canvas-element)
        half-width (/ canvas-width 2)
        half-height (/ canvas-height 2)]
    (swap! state/model-coords assoc :upper-left (model-coords-from-corner (* half-width -1) (* half-height -1))
           :upper-right (model-coords-from-corner half-width (* half-height -1))
           :lower-left (model-coords-from-corner (* half-width -1) half-height)
           :lower-right (model-coords-from-corner half-width half-height))))

(defn update-lat-lon
  "Get model-coords and transform to latitude and longitude"
  []
  (update-model-coords)
  (swap! state/lat-lon-coords assoc :upper-left (model-coords-to-lat-lon (:upper-left @state/model-coords))
         :upper-right (model-coords-to-lat-lon (:upper-right @state/model-coords))
         :lower-left (model-coords-to-lat-lon (:lower-left @state/model-coords))
         :lower-right (model-coords-to-lat-lon (:lower-right @state/model-coords))))
