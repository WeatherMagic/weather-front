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

(defonce zoom-level (atom 110))
(defonce mouse-pressed (atom false))
(defonce last-xy-pos (atom {:x-val 0 :y-val 0}))
(defonce relative-mousemovement (atom {:x-val 0 :y-val 0}))

(defn zoom-camera
  "Returns the camera given in camera-map modified zooming by scroll-distance."
  [camera-map scroll-distance]
  (reset! zoom-level (:fov camera-map))
  (cam/perspective-camera
   (assoc camera-map :fov (min 140 (+ @zoom-level (* @zoom-level scroll-distance 5.0E-4))))))

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
                                     (g/rotate-y (m/radians (* (* (Math/hypot y-diff x-diff) @zoom-level) 1.0E-3)))
                                     (g/rotate-z (Math/atan2 y-diff x-diff))
                                     (m/* @state/earth-orientation))
        northpole-x (.-m10 future-earth-orientation)
        northpole-y (.-m11 future-earth-orientation)
        northpole-z (.-m12 future-earth-orientation)
        northpole-y-norm (/ northpole-y (Math/hypot northpole-y northpole-x))
        delta-angle (/ (* (Math/acos northpole-y-norm) (Math/sign northpole-x)) 100)]
    (swap! state/pointer-zoom-info assoc :delta-z-angle delta-angle)))

(defn update-pan
  "Updates the atom holding the rotation of the world"
  [rel-x rel-y]
  (reset! state/earth-orientation (-> M44
                                      (g/rotate-z (* (Math/atan2 rel-y rel-x) -1))
                                      (g/rotate-y (m/radians (* (* (Math/hypot rel-y rel-x 2) @zoom-level) 1.0E-3)))
                                      (g/rotate-z (Math/atan2 rel-y rel-x))
                                      (m/* @state/earth-orientation))))

(defn update-zoom-point-alignment
  "Updates the atom holding the rotation of the world"
  [rel-x rel-y delta-angle step delta-fov]
  (swap! state/camera-left zoom-camera -15.0)
  (swap! state/camera-right zoom-camera -15.0)
  (reset! state/earth-orientation (-> M44
                                      (g/rotate-z (* delta-angle step))
                                      (g/rotate-z (* (Math/atan2 rel-y rel-x) -1))
                                      (g/rotate-y (m/radians (* (* (Math/hypot rel-y rel-x) @zoom-level) 1.0E-3)))
                                      (g/rotate-z (Math/atan2 rel-y rel-x))
                                      (g/rotate-z (* (* delta-angle (dec step)) -1))
                                      (m/* @state/earth-orientation))))

(defn move-fcn
  "Handles the movements of the mouse during panning"
  [event]
  (let [last-pos @last-xy-pos
        current-x (.-clientX event)
        current-y (.-clientY event)
        rel-x (- current-x (:x-val last-pos))
        rel-y (- current-y (:y-val last-pos))]
    (update-pan rel-x rel-y))
  (reset! last-xy-pos {:x-val (.-clientX event) :y-val (.-clientY event)}))

(defn mouse-up
  "If the mouse is released during panning"
  [_]
  (reset! mouse-pressed false)
  (.removeEventListener (.getElementById js/document "canvases") "mousemove" move-fcn false))

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
        total-steps (:total-steps @state/pointer-zoom-info)]
    (update-alignment-angle x-diff y-diff)
    (swap! state/pointer-zoom-info assoc :state true :delta-fov (/ (- 120 (:fov @state/camera-left)) total-steps) :delta-x (/ x-diff total-steps) :delta-y (/ y-diff total-steps) :current-step 1)))

(defn pan-handler
  "Handles the mouse events for panning"
  [event]
  (reset! last-xy-pos {:x-val (.-clientX event) :y-val (.-clientY event)})
  (reset! mouse-pressed true)
  (reset! state/earth-animation-fn world/stop-spin!)
  (when (= @mouse-pressed true)
    (.addEventListener (.getElementById js/document "canvases") "mousemove" move-fcn false)
    (.addEventListener (.getElementById js/document "canvases") "mouseup" mouse-up false)))

(defn hook-up-events!
  "Hook up all the application event handlers."
  []
  (.addEventListener
   (.getElementById js/document "canvases") "wheel"
   (fn [event]
     (swap! state/camera-left zoom-camera (.-deltaY event))
     (swap! state/camera-right zoom-camera (.-deltaY event))) false)
  (.addEventListener js/window "load" resize-handler false)
  (.addEventListener js/window "resize" resize-handler false)
  (.addEventListener (.getElementById js/document "canvases") "mousedown" pan-handler false)
  (.addEventListener (.getElementById js/document "left-canvas") "dblclick" (fn [event] (pointer-zoom-handler event "left-canvas")) false)
  (.addEventListener (.getElementById js/document "right-canvas") "dblclick" (fn [event] (pointer-zoom-handler event "right-canvas")) false)
  true)
