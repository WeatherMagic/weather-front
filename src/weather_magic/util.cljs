(ns weather-magic.util
  (:require
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]))

(defn transparent-println
  "Print something and return that something."
  [item]
  (println item)
  item)

(defn toggle
  "Add item to set if it's not in set, remove item from set if it's already in it.
   In other words, toggle the item in the set."
  [set item]
  (transparent-println
   ((if (contains? set item) disj conj) set item)))

(defn get-filename
  "Get the name of the file in the given path. Cuts out folder and
  file name extension. Returns nil if no file is found in the path."
  [path]
  ;; Match characters which aren't the literal '/' or '.' (the file
  ;; name), possibly followed by a '.' and some characters not '/' (the
  ;; filename extension), always ended with a line ending.
  (second (re-find #"([^/^\.]+)\.?[^/]*$" path)))

(defn lat-lon-to-cart
  "Converting latitude and longitude to model cordinates"
  [lat lon]
  (let [rlat (m/radians lat)
        rlon (m/radians lon)]
    {:x (* (Math/sin rlon) (Math/cos rlat))
     :y (Math/sin rlat)
     :z (* (Math/cos rlon) (Math/cos rlat))}))

(defn cart-to-lat-lon
  "Converting latitude and longitude to model cordinates"
  [x y z]
  (let [eps 0.001
        lon (atom 0)
        lat (atom 0)]
    (if (> (Math/abs z) eps)
      (reset! lon (* (/ 180 PI) (Math/atan2 x z)))
      (if (> (Math/abs y) (- 1 eps))
        (reset! lon 0)
        (if (pos? x)
          (reset! lon 90)
          (reset! lon -90))))
    (if (> y (- 1 eps))
      (reset! lat 90)
      (if (< y (+ -1 eps))
        (reset! lat -90)
        (reset! lat (* (/ 180 PI) (Math/asin y)))))
    {:lat @lat :lon @lon}))
