(ns weather-magic.world
  (:require
   [weather-magic.models           :as models]
   [weather-magic.state            :as state]
   [weather-magic.textures         :as textures]
   [thi.ng.geom.gl.buffers         :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.core               :as g]
   [thi.ng.geom.matrix             :as mat :refer [M44]]
   [thi.ng.math.core               :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.vector             :as v :refer [vec3]]))

(defn show-europe!
  "Rotates the sphere so that Europe is shown."
  [t]
  (reset! state/model models/sphere)
  (reset! state/texture textures/earth)
  (reset! state/earth-orientation (-> M44
                                      (g/rotate-x (m/radians 45))
                                      (g/rotate-y (m/radians 80))
                                      (g/rotate-z (m/radians 0)))))

(defn northpole-up!
  "Rotates the sphere so that the northpole is up after panning."
  []
  (reset! state/model models/sphere)
  (reset! state/texture textures/earth)
  (reset! state/earth-orientation M44))

; If we decide to display maps on a flat surface we have to reset the translation when changing to world-view
(defn show-turkey!
  "Shows Turkey on a flat surface."
  [t]
  (reset! state/model models/plane)
  (reset! state/texture textures/turkey)
  (reset! state/earth-orientation (-> M44
                                      (g/translate (vec3 2 1.5 0))
                                      (g/rotate-x (m/radians 0))
                                      (g/rotate-y (m/radians 0))
                                      (g/rotate-z (m/radians 180)))))

(defn spin-earth!
  "Rotates the sphere indefinitely."
  [delta-time]
  (reset! state/model models/sphere)
  (reset! state/texture textures/earth)
  (reset! state/earth-orientation (-> M44
                                      (g/rotate-y (m/radians delta-time))
                                      (m/* @state/earth-orientation))))

(defn stop-spin
  "Makes the earth stop spinning"
  [_]
  (reset! state/earth-orientation @state/earth-orientation))

;; THIS IS BAD AND I SHOULD FEEL BAD.
(reset! state/earth-animation-fn spin-earth!)
