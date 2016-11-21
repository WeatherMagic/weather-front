(ns weather-magic.world
  (:require
   [weather-magic.models           :as models]
   [weather-magic.state            :as state]
   [weather-magic.textures         :as textures]
   [thi.ng.geom.gl.buffers         :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.vector             :as v :refer [vec3]]))

(defn show-europe!
  "Rotates the sphere so that Europe is shown."
  [t]
  (reset! state/model models/sphere)
  (reset! state/texture textures/earth)
  (swap!  state/earth-orientation assoc
          :x-angle 45 :y-angle 80 :z-angle 0 :translation (vec3 0 0 0)))

(defn show-turkey!
  "Shows Turkey on a flat surface."
  [t]
  (reset! state/model models/plane)
  (reset! state/texture textures/turkey)
  (swap!  state/earth-orientation assoc
          :x-angle 0  :y-angle 0 :z-angle 180 :translation (vec3 2 1.5 0)))

(defn spin-earth!
  "Rotates the sphere indefinitely."
  [t]
  (reset! state/model models/sphere)
  (reset! state/texture textures/earth)
  (swap!  state/earth-orientation assoc
          :x-angle 24 :y-angle t :z-angle 0 :translation (vec3 0 0 0)))

;; THIS IS BAD AND I SHOULD FEEL BAD.
(reset! state/earth-animation-fn spin-earth!)
