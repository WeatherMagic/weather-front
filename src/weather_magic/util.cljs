(ns weather-magic.util
  (:require
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.vector  :refer [vec2]]
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
  ;; name), possibly followed by a '.' and some characters not '/' and
  ;; '.' (the filename extension), always ended with a line ending.
  (second (re-find #"([^/^\.]+)\.?[^/^\.]*$" path)))

(defn map->query-string
  "Turns a map into a query string, {:a 2 :b 10} -> '?a=2&b=10'"
  [map]
  (str "?" (clojure.string/join "&" (for [[key value] map]
                                      (str (name key) "=" value)))))

(defn north-pole-rotation-around-z
  [earth-transform]
  (let [northpole-x      (.-m10 earth-transform)
        northpole-y      (.-m11 earth-transform)
        northpole-z      (.-m12 earth-transform)
        northpole-y-norm (/ northpole-y (Math/hypot northpole-y northpole-x))]
    (* (Math/acos northpole-y-norm) (Math/sign northpole-x))))


(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))
