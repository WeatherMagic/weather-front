(ns weather-magic.event-handlers
  (:require
   [weather-magic.state :as state]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.rect  :as rect]
   [thi.ng.geom.gl.core  :as gl]))

(defn zoom-camera
  "Returns the camera given in camera-map modified zooming by scroll-distance."
  [camera-map scroll-distance]
  (let [cur-val (:fov camera-map)]
    (cam/perspective-camera
     (assoc camera-map :fov (min 140 (+ cur-val (* cur-val scroll-distance 5.0E-4)))))))

(defn resize-handler [_]
  "Handles the aspect ratio of the webGL rendered world"
  (let [left-canvas (.getElementById js/document "left-canvas")
        actual-width (.-clientWidth left-canvas)
        actual-height (.-clientHeight left-canvas)
        webgl-width (.-width left-canvas)
        webgl-height (.-height left-canvas)]
    (when-not (and (= actual-width webgl-width) (= actual-height webgl-height))
      (set! (.-width (.-canvas state/gl-ctx)) actual-width)
      (set! (.-height (.-canvas state/gl-ctx)) actual-height)
      (set! (.-width (.-canvas state/gl-ctx2)) actual-width)
      (set! (.-height (.-canvas state/gl-ctx2)) actual-height)
      (swap! state/camera #(cam/perspective-camera
                            (assoc % :aspect (rect/rect actual-width actual-height))))
      (gl/set-viewport state/gl-ctx (:aspect @state/camera))
      (gl/set-viewport state/gl-ctx2 (:aspect @state/camera)))))

(defn hook-up-events!
  "Hook up all the application event handlers."
  []
  (.addEventListener
   (.getElementById js/document "canvases") "wheel"
   (fn [event] (swap! state/camera zoom-camera (.-deltaY event))) false)
  (.addEventListener js/window "load" resize-handler false)
  (.addEventListener js/window "resize" resize-handler false)
  true)
