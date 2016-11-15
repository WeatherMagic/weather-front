(ns weather-magic.world
  (:require
   [weather-magic.models           :as models]
   [thi.ng.geom.gl.buffers         :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.vector             :as v]))

(defn show-europe!
  "Rotates the sphere so that Europe is shown."
  [earth-atom model-atom t]
  (reset! model-atom models/sphere)
  (swap! earth-atom assoc :x-angle 45 :y-angle 80))

(defn show-turkey!
  "Shows Turkey on a flat surface."
  [earth-atom model-atom t]
  (reset! model-atom models/plane)
  (swap! earth-atom assoc
         :x-angle 0
         :y-angle 0
         :z-angle 180
         :translation (v/vec3 2 1.5 0)))

(defn spin-earth!
  "Rotates the sphere indefinitely."
  [earth-atom model-atom t]
  (reset! model-atom models/sphere)
  (swap! earth-atom assoc :x-angle 24.5 :y-angle t))
