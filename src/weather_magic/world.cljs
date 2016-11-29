(ns weather-magic.world
  (:require
   [weather-magic.models   :as models]
   [weather-magic.state    :as state]
   [weather-magic.textures :as textures]
   [thi.ng.geom.gl.camera  :as cam]
   [thi.ng.geom.core       :as g]
   [thi.ng.geom.matrix     :as mat :refer [M44]]
   [thi.ng.math.core       :as m]
   [thi.ng.geom.vector     :as v   :refer [vec3]]))

(defn show-europe!
  "Rotates the sphere so that Europe is shown."
  [t]
  (reset! state/model models/sphere)
  (reset! state/base-texture-left (:earth @state/textures-left))
  (reset! state/earth-orientation (-> M44
                                      (g/rotate-x (m/radians 45))
                                      (g/rotate-y (m/radians 80))
                                      (g/rotate-z (m/radians 0)))))

(defn northpole-up!
  "Rotates the sphere so that the northpole is up after panning."
  []
  (reset! state/model models/sphere)
  (reset! state/base-texture-left (:earth @state/textures-left))
  (reset! state/earth-orientation M44))

; If we decide to display maps on a flat surface we have to reset the translation when changing to world-view
(defn show-turkey!
  "Shows Turkey on a flat surface."
  [t]
  (swap!  state/textures-left merge
          (textures/load-texture-if-needed state/gl-ctx-left @state/textures-left "img/turkey.jpg"))
  (reset! state/model models/plane)
  (reset! state/base-texture-left (:turkey @state/textures-left))
  (reset! state/earth-orientation (-> M44
                                      (g/translate (vec3 2 1.5 0))
                                      (g/rotate-x (m/radians 0))
                                      (g/rotate-y (m/radians 0))
                                      (g/rotate-z (m/radians 180)))))

(defn spin-earth!
  "Rotates the sphere indefinitely."
  [delta-time]
  (reset! state/model models/sphere)
  (reset! state/base-texture-left (:earth @state/textures-left))
  (reset! state/earth-orientation (-> M44
                                      (g/rotate-y (m/radians delta-time))
                                      (m/* @state/earth-orientation))))

(defn reset-spin!
  "Rotates the sphere so that the northpole is up after panning."
  [delta-time]
  (reset! state/model models/sphere)
  (reset! state/base-texture-left (:earth @state/textures-left))
  (reset! state/earth-orientation M44)
  (reset! state/earth-animation-fn spin-earth!))

(defn stop-spin!
  "Makes the earth stop spinning"
  [_]
  (reset! state/earth-orientation @state/earth-orientation))

(defn show-turkey
  "Shows Turkey on a flat surface."
  [earth-atom t]
  (reset! earth-atom {:xAngle 0
                      :yAngle 0
                      :zAngle 180
                      :translation (vec3 2 1.5 0)}))

(defonce zoom-level (atom 110))

(defn zoom-camera
  "Returns the camera given in camera-map modified zooming by scroll-distance."
  [camera-map scroll-distance]
  (reset! zoom-level (:fov camera-map))
  (cam/perspective-camera
   (assoc camera-map :fov (min 140 (+ @zoom-level (* @zoom-level scroll-distance 5.0E-4))))))

(defn update-zoom-point-alignment
  "Updates the atom holding the rotation of the world"
  [rel-x rel-y delta-angle step delta-fov]
  (swap! state/camera-left zoom-camera (:delta-zoom @state/pointer-zoom-info))
  (swap! state/camera-right zoom-camera (:delta-zoom @state/pointer-zoom-info))
  (reset! state/earth-orientation (-> M44
                                      (g/rotate-z (* delta-angle step))
                                      (g/rotate-z (* (Math/atan2 rel-y rel-x) -1))
                                      (g/rotate-y (m/radians (* (* (Math/hypot rel-y rel-x) @zoom-level) 1.0E-3)))
                                      (g/rotate-z (Math/atan2 rel-y rel-x))
                                      (g/rotate-z (* (* delta-angle (dec step)) -1))
                                      (m/* @state/earth-orientation))))
(defn align-animation!
  "Function that handles alignment or zoom-alignment"
  []
  (let [delta-x (:delta-x @state/pointer-zoom-info)
        delta-y (:delta-y @state/pointer-zoom-info)
        delta-fov (:delta-fov @state/pointer-zoom-info)
        total-steps (:total-steps @state/pointer-zoom-info)
        current-step (:current-step @state/pointer-zoom-info)
        delta-z-angle (:delta-z-angle @state/pointer-zoom-info)]
    (swap! state/pointer-zoom-info assoc-in [:current-step] (inc current-step))
    (when (= current-step total-steps)
      (reset! state/earth-animation-fn stop-spin!))
    (update-zoom-point-alignment delta-x delta-y delta-z-angle current-step delta-fov)))

;; THIS IS BAD AND I SHOULD FEEL BAD.
(reset! state/earth-animation-fn spin-earth!)
