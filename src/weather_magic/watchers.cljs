(ns weather-magic.watchers
  (:require
   [thi.ng.math.core :as m]))

(defn mount-rotation-data-reload-watch
  [earth-orientation reload-fn]
  (let [orientation-of-last-request (volatile! @earth-orientation)]
    (add-watch earth-orientation :trigger-data-load
               (fn [key reference old-state new-state]
                 ;; When the z-axis has moved more than x.
                 ;; TODO: This arbitrary amount x should scale with zoom.
                 (when (> 0.999 (.-m22 (m/* new-state (m/invert @orientation-of-last-request))))
                   (vreset! orientation-of-last-request new-state)
                   (reload-fn))))))
