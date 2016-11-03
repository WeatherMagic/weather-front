(ns weather-magic.event-handlers
  (:require
   [weather-magic.state :as state]
   [thi.ng.geom.gl.camera :as cam]))

(defn zoom-camera
  "Returns the camera given in camera-map modified zooming by scroll-distance."
  [camera-map scroll-distance]
  (let [cur-val (:fov camera-map)]
    (cam/perspective-camera
     (assoc camera-map :fov (min 140 (+ cur-val (* cur-val scroll-distance 5.0E-4)))))))

(defn hook-up-events!
  "Hook up all the application event handlers."
  []
  (.addEventListener
   (.getElementById js/document "main") "wheel"
   (fn [event] (swap! state/camera zoom-camera (.-deltaY event))) false)
  true)
