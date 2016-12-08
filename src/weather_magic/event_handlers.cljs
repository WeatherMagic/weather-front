(ns weather-magic.event-handlers
  (:require
   [weather-magic.state :as state]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.rect  :as rect]
   [weather-magic.world :as world]
   [thi.ng.geom.gl.core  :as gl]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.core :as g]
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defonce mouse-pressed (atom false))
(defonce last-xy-pos (atom {:x-val 0 :y-val 0}))
(defonce relative-mousemovement (atom {:x-val 0 :y-val 0}))

(defn reset-zoom
  [camera-map]
  (cam/perspective-camera
   (assoc camera-map :fov 110)))

(defn resize-handler [_]
  "Handles the aspect ratio of the webGL rendered world"
  (let [left-canvas (.getElementById js/document "left-canvas")
        actual-width (.-clientWidth left-canvas)
        actual-height (.-clientHeight left-canvas)
        webgl-width (.-width left-canvas)
        webgl-height (.-height left-canvas)]
    (when-not (and (= actual-width webgl-width) (= actual-height webgl-height))
      (set! (.-width (.-canvas state/gl-ctx-left)) actual-width)
      (set! (.-height (.-canvas state/gl-ctx-left)) actual-height)
      (set! (.-width (.-canvas state/gl-ctx-right)) actual-width)
      (set! (.-height (.-canvas state/gl-ctx-right)) actual-height)
      (swap! state/camera-left #(cam/perspective-camera
                                 (assoc % :aspect (rect/rect actual-width actual-height))))
      (swap! state/camera-right #(cam/perspective-camera
                                  (assoc % :aspect (rect/rect actual-width actual-height))))
      (gl/set-viewport state/gl-ctx-left (:aspect @state/camera-left))
      (gl/set-viewport state/gl-ctx-right (:aspect @state/camera-right)))))

(defn update-alignment-angle
  "Updating how much the globe should be rotated around the z axis to align northpole"
  [x-diff y-diff]
  (let [future-earth-orientation (-> M44
                                     (g/rotate-z (* (Math/atan2 y-diff x-diff) -1))
                                     (g/rotate-y (m/radians (* (* (Math/hypot y-diff x-diff) (:fov @state/camera-left)) 1.0E-3)))
                                     (g/rotate-z (Math/atan2 y-diff x-diff))
                                     (m/* @state/earth-orientation))
        northpole-x (.-m10 future-earth-orientation)
        northpole-y (.-m11 future-earth-orientation)
        northpole-z (.-m12 future-earth-orientation)
        northpole-y-norm (/ northpole-y (Math/hypot northpole-y northpole-x))
        delta-angle (/ (* (Math/acos northpole-y-norm) (Math/sign northpole-x)) (:total-steps @state/pointer-zoom-info))]
    (swap! state/pointer-zoom-info assoc :delta-z-angle delta-angle)))

(defn update-pan
  "Updates the atom holding the rotation of the world"
  [rel-x rel-y]
  (let [camera-z-pos (aget (.-buf (:eye @state/camera-left)) 2)
        zoom-level (* (- camera-z-pos 1.1) (/ 4 5))]
    (reset! state/earth-orientation (-> M44
                                        (g/rotate-z (* (Math/atan2 rel-y rel-x) -1))
                                        (g/rotate-y (m/radians (* (* (Math/hypot rel-y rel-x) zoom-level) 0.15)))
                                        (g/rotate-z (Math/atan2 rel-y rel-x))
                                        (m/* @state/earth-orientation)))))

(defn align-handler []
  (update-alignment-angle 0 0)
  (swap! state/pointer-zoom-info assoc :delta-x 0 :delta-y 0 :current-step 0 :delta-zoom 0)
  (reset! state/earth-animation-fn world/align-animation!))

(defn reset-spin-handler
  []
  (swap! state/camera-left reset-zoom)
  (swap! state/camera-right reset-zoom)
  (reset! state/earth-animation-fn world/reset-spin!))

(defn move-fn
  "Handles the movements of the mouse during panning"
  [event]
  (let [last-pos @last-xy-pos
        current-x (.-clientX event)
        current-y (.-clientY event)
        rel-x (- current-x (:x-val last-pos))
        rel-y (- current-y (:y-val last-pos))]
    (swap! state/space-offset (fn [atom] (vec2 (+ (aget (.-buf atom) 0) (/ rel-x 1000)) (+ (aget (.-buf atom) 1) (/ rel-y 1000)))))
    (update-pan rel-x rel-y)
    (swap! state/pan-speed assoc :speed (min (Math/hypot rel-x rel-y) 40) :rel-y rel-y :rel-x rel-x)
    (reset! last-xy-pos {:x-val current-x :y-val current-y})))

(defn mouse-up
  "If the mouse is released during panning"
  [_]
  (reset! state/earth-animation-fn world/after-pan-spin!)
  (reset! mouse-pressed false)
  (.removeEventListener (.getElementById js/document "canvases") "mousemove" move-fn false))

(defn pointer-zoom-handler
  "Rotates the globe to the point which is dubble clicked"
  [event canvas]
  (let [x-pos (.-clientX event)
        y-pos (.-clientY event)
        canvas-element (.getElementById js/document canvas)
        canvas-width (.-clientWidth canvas-element)
        canvas-height (.-clientHeight canvas-element)
        window-element (.getElementById js/document "canvases")
        window-width (.-clientWidth window-element)
        window-height (.-clientHeight window-element)
        x-diff (if (= canvas "right-canvas") (- (+ (/ window-width 2) (/ canvas-width 2)) x-pos) (- (/ canvas-width 2) x-pos))
        y-diff (- (/ window-height 2) y-pos)
        total-steps (:total-steps @state/pointer-zoom-info)
        camera-z-pos (aget (.-buf (:eye @state/camera-left)) 2)
        zoom-distance (* (* (- camera-z-pos 1.1) (/ 4 5)) -1)]
    (update-alignment-angle x-diff y-diff)
    (swap! state/pointer-zoom-info assoc
           :delta-x (/ x-diff total-steps)
           :delta-y (/ y-diff total-steps)
           :current-step 0
           :delta-zoom (/ zoom-distance total-steps)))
  (reset! state/earth-animation-fn world/align-animation!))

(defn pan-handler
  "Handles the mouse events for panning"
  [event]
  (swap! state/pan-speed assoc :speed 0)
  (reset! last-xy-pos {:x-val (.-clientX event) :y-val (.-clientY event)})
  (reset! mouse-pressed true)
  (reset! state/earth-animation-fn world/stop-spin!)
  (when (= @mouse-pressed true)
    (.addEventListener (.getElementById js/document "canvases") "mousemove" move-fn false)
    (.addEventListener (.getElementById js/document "canvases") "mouseup" mouse-up false)
    (.addEventListener (.getElementById js/document "canvases") "mouseleave" mouse-up false)))

(defn zoom-to-mouse
  [event canvas]
  (let [x-pos (.-clientX event)
        y-pos (.-clientY event)
        canvas-element (.getElementById js/document canvas)
        canvas-width (.-clientWidth canvas-element)
        canvas-height (.-clientHeight canvas-element)
        window-element (.getElementById js/document "canvases")
        window-width (.-clientWidth window-element)
        window-height (.-clientHeight window-element)
        x-diff (if (= canvas "right-canvas") (- (+ (/ window-width 2) (/ canvas-width 2)) x-pos) (- (/ canvas-width 2) x-pos))
        y-diff (- (/ window-height 2) y-pos)
        total-steps (:total-steps @state/pointer-zoom-info)
        camera-z-pos (aget (.-buf (:eye @state/camera-left)) 2)
        zoom-level (* (- camera-z-pos 1.1) (/ 4 5))
        delta-x (* zoom-level (/ x-diff 80))
        delta-y (* zoom-level (/ y-diff 80))
        zoom-distance (* (* (.-deltaY event) zoom-level) 1.0E-3)]
    (swap! state/camera-left world/zoom-camera zoom-distance)
    (swap! state/camera-right world/zoom-camera zoom-distance)
    (if (neg? zoom-distance)
      (update-pan delta-x delta-y)
      (when (< camera-z-pos 3.0)
        (update-pan (* delta-x -1) (* delta-y -1))))))

(defn hook-up-events!
  "Hook up all the application event handlers."
  []
  (.addEventListener (.getElementById js/document "left-canvas") "wheel" (fn [event] (zoom-to-mouse event "left-canvas")))
  (.addEventListener (.getElementById js/document "right-canvas") "wheel" (fn [event] (zoom-to-mouse event "right-canvas")))
  (.addEventListener js/window "load" resize-handler false)
  (.addEventListener js/window "resize" resize-handler false)
  (.addEventListener (.getElementById js/document "canvases") "mousedown" pan-handler false)
  (.addEventListener (.getElementById js/document "left-canvas") "dblclick" (fn [event] (pointer-zoom-handler event "left-canvas")) false)
  (.addEventListener (.getElementById js/document "right-canvas") "dblclick" (fn [event] (pointer-zoom-handler event "right-canvas")) false)
  true)
