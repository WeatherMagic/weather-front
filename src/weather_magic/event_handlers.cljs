(ns weather-magic.event-handlers
  (:require
   [weather-magic.state :as state]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.rect  :as rect]
   [weather-magic.world :as world]
   [thi.ng.geom.gl.core  :as gl]
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defn zoom-camera
  "Returns the camera given in camera-map modified zooming by scroll-distance."
  [camera-map scroll-distance]
  (let [cur-val (:fov camera-map)]
    (cam/perspective-camera
     (assoc camera-map :fov (min 140 (+ cur-val (* cur-val scroll-distance 5.0E-4)))))))

(defn resize-handler [_]
  "Handles the aspect ratio of the webGL rendered world"
  (let [element (.getElementById js/document "main")
        actual-width (.-clientWidth element)
        actual-height (.-clientHeight element)
        webgl-width (.-width element)
        webgl-height (.-height element)]
    (when-not (and (= actual-width webgl-width) (= actual-height webgl-height))
      (set! (.-width (.-canvas state/gl-ctx)) actual-width)
      (set! (.-height (.-canvas state/gl-ctx)) actual-height)
      (swap! state/camera #(cam/perspective-camera
                            (assoc % :aspect (rect/rect actual-width actual-height))))
      (gl/set-viewport state/gl-ctx (:aspect @state/camera)))))

(defonce click-variable (atom false))
(defonce last-xy-pos (atom {:x-val 0 :y-val 0}))
(defonce relative-mousemovement (atom {:x-val 0 :y-val 0}))

(defn move-fcn [event]
  (let [last-pos @last-xy-pos current-x (.-clientX event) current-y (.-clientY event)]
    (reset! relative-mousemovement {:x-val (- current-x (:x-val last-pos)) :y-val (- current-y (:y-val last-pos))})
    (println "rel mousemov" relative-mousemovement))
  (reset! last-xy-pos {:x-val (.-clientX event) :y-val (.-clientY event)}))

(defn mouse-not-down [_]
  (println "mouse up")
  (reset! click-variable false)
  (.removeEventListener (.getElementById js/document "main") "mousemove"
                        move-fcn false))

(defn pan-handler [event]
  (reset! last-xy-pos {:x-val (.-clientX event) :y-val (.-clientY event)})
  (println @last-xy-pos)
  (reset! click-variable true)
  (println "mousedown")
  (reset! state/earth-animation-fn world/stop-spin)
  (when-not (= @click-variable false)
    (.addEventListener (.getElementById js/document "main") "mousemove"
                       move-fcn false)
    (.addEventListener (.getElementById js/document "main") "mouseup" mouse-not-down false)))

(defn hook-up-events!
  "Hook up all the application event handlers."
  []
  (.addEventListener (.getElementById js/document "main") "wheel"
                     (fn [event] (swap! state/camera zoom-camera (.-deltaY event))) false)
  (.addEventListener js/window "load" resize-handler false)
  (.addEventListener js/window "resize" resize-handler false)
  (.addEventListener (.getElementById js/document "main") "mousedown" pan-handler false)
  true)
