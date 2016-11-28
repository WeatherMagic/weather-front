(ns weather-magic.util
  (:require
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.vector  :refer [vec2]]))

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

(defn latlon-to-uv
  [lat lon]
  (let [u (+ (/ lon 360) 1/2)
        v (+ (/ lat 180) 1/2)]
    (vec2 u v)))

