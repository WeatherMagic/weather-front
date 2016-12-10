(ns weather-magic.world
  (:require
   [weather-magic.models   :as models]
   [weather-magic.state    :as state]
   [weather-magic.textures :as textures]
   [thi.ng.geom.gl.camera  :as cam]
   [thi.ng.geom.core       :as g]
   [thi.ng.geom.matrix     :as mat :refer [M44]]
   [thi.ng.math.core       :as m   :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.vector     :as v   :refer [vec2 vec3]]))

(defn set-earth-rotation
  ""
  [rot-coords]
  (-> M44
      (g/rotate-x (m/radians (aget (.-buf rot-coords) 0)))
      (g/rotate-y (m/radians (aget (.-buf rot-coords) 1)))
      (g/rotate-z (m/radians (aget (.-buf rot-coords) 2)))))

(defn stop-spin!
  "Makes the earth stop spinning, aka the no-op function."
  [_])

(defn reset-zoom
  [camera-map]
  (cam/perspective-camera
   (assoc camera-map :eye (vec3 0 0 3.0))))

(defn show-static-view!
  "Rotates the sphere so that Europe is shown."
  [t]
  (reset! state/current-model-key :sphere)
  (reset! state/base-texture-left (:earth @state/textures-left))
  (reset! state/earth-orientation (set-earth-rotation @state/static-scene-coordinates))
  (reset! state/earth-animation-fn stop-spin!)
  (swap! state/camera-left reset-zoom)
  (swap! state/camera-right reset-zoom))

(defn northpole-up!
  "Rotates the sphere so that the northpole is up after panning."
  []
  (reset! state/current-model-key :sphere)
  (reset! state/base-texture-left (:earth @state/textures-left))
  (reset! state/earth-orientation M44))

(defn spin-earth!
  "Rotates the sphere indefinitely."
  [delta-time]
  (reset! state/current-model-key :sphere)
  (reset! state/base-texture-left (:earth @state/textures-left))
  (swap! state/space-offset (fn [atom] (vec2 (+ (aget (.-buf atom) 0) (/ (m/radians delta-time) TWO_PI)) (aget (.-buf atom) 1))))
  (reset! state/earth-orientation (-> M44
                                      (g/rotate-y (m/radians delta-time))
                                      (m/* @state/earth-orientation))))

(defn reset-spin!
  "Rotates the sphere so that the northpole is up after panning."
  [delta-time]
  (reset! state/current-model-key :sphere)
  (reset! state/base-texture-left (:earth @state/textures-left))
  (reset! state/earth-orientation M44)
  (reset! state/earth-animation-fn spin-earth!))

(defn after-pan-spin!
  "Rotates the sphere indefinitely."
  [delta-time]
  (let [rel-y (:rel-y @state/pan-speed)
        rel-x (:rel-x @state/pan-speed)
        current-speed (:speed @state/pan-speed)
        new-speed (- current-speed 0.2)
        camera-z-pos (aget (.-buf (:eye @state/camera-left)) 2)
        zoom-level (* (- camera-z-pos 1.1) (/ 4 5))]
    (swap! state/space-offset (fn [atom] (vec2 (+ (aget (.-buf atom) 0) (* current-speed (/ rel-x 100000))) (+ (aget (.-buf atom) 1) (* current-speed (/ rel-y 100000))))))
    (reset! state/earth-orientation (-> M44
                                        (g/rotate-z (* (Math/atan2 rel-y rel-x) -1))
                                        (g/rotate-y (m/radians (* (* (Math/hypot current-speed) zoom-level) 0.15)))
                                        (g/rotate-z (Math/atan2 rel-y rel-x))
                                        (m/* @state/earth-orientation)))
    (if (neg? new-speed)
      (reset! state/earth-animation-fn stop-spin!)
      (swap! state/pan-speed assoc :speed new-speed))))

(defn zoom-camera
  "Returns the camera given in camera-map modified zooming by scroll-distance."
  [camera-map delta-z]
  (let [current-camera-pos (:eye camera-map)
        current-z-camera-pos (aget (.-buf current-camera-pos) 2)
        new-camera-pos (if (and (neg? delta-z) (> delta-z -0.001)) current-camera-pos (vec3 0 0 (max (min (+ current-z-camera-pos delta-z) 3.0) 1.11)))]
    (cam/perspective-camera
     (assoc camera-map :eye new-camera-pos))))

(defn update-zoom-point-alignment
  "Updates the atom holding the rotation of the world"
  [rel-x rel-y delta-angle step]
  (let [camera-z-pos (aget (.-buf (:eye @state/camera-left)) 2)
        zoom-level (* (- camera-z-pos 1.1) (/ 4 5))]
    (swap! state/camera-left zoom-camera (:delta-zoom @state/pointer-zoom-info))
    (swap! state/camera-right zoom-camera (:delta-zoom @state/pointer-zoom-info))
    (swap! state/space-offset (fn [atom] (vec2 (+ (aget (.-buf atom) 0) (/ rel-x 1000)) (+ (aget (.-buf atom) 1) (/ rel-y 1000)))))
    (reset! state/earth-orientation (-> M44
                                        (g/rotate-z (* delta-angle step))
                                        (g/rotate-z (* (Math/atan2 rel-y rel-x) -1))
                                        (g/rotate-y (m/radians (* (* (Math/hypot rel-y rel-x) zoom-level 0.15))))
                                        (g/rotate-z (Math/atan2 rel-y rel-x))
                                        (g/rotate-z (* (* delta-angle (dec step)) -1))
                                        (m/* @state/earth-orientation)))))
(defn align-animation!
  "Function that handles alignment or zoom-alignment"
  []
  (let [delta-x (:delta-x @state/pointer-zoom-info)
        delta-y (:delta-y @state/pointer-zoom-info)
        total-steps (:total-steps @state/pointer-zoom-info)
        current-step (:current-step @state/pointer-zoom-info)
        delta-z-angle (:delta-z-angle @state/pointer-zoom-info)]
    (swap! state/pointer-zoom-info assoc-in [:current-step] (inc current-step))
    (when (= current-step total-steps)
      (reset! state/earth-animation-fn stop-spin!))
    (update-zoom-point-alignment delta-x delta-y delta-z-angle current-step)))

;; THIS IS BAD AND I SHOULD FEEL BAD.
(reset! state/earth-animation-fn spin-earth!)
