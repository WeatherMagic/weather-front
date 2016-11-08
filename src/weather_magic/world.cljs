(ns weather-magic.world
  (:require
    [thi.ng.geom.vector :as v :refer [vec2 vec3]]))

(defn show-europe
  "Rotates the sphere so that Europe is shown."
  [earth-atom t]
  (reset! earth-atom {:xAngle 45
                      :yAngle 80
                      :zAngle 0
                      :translation (vec3 0 0 0)}))

(defn spin
  "Rotates the sphere indefinitely."
  [earth-atom t]
  (reset! earth-atom {:xAngle 24.5
                      :yAngle t
                      :zAngle 0
                      :translation (vec3 0 0 0)}))

(defn show-turkey
  "Shows Turkey on a flat surface."
  [earth-atom t]
  (reset! earth-atom {:xAngle 0
                      :yAngle 0
                      :zAngle 180
                      :translation (vec3 2 1.5 0)}))
