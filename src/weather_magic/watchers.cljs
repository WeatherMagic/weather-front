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

(defn on-change-date-watch
  [date-atom reload-fn]
  (let [date-atom-last-request (volatile! @date-atom)]
    (add-watch date-atom :trigger-data-load-date
               (fn [key reference old-state new-state]
                 (when-not (= (:value (:year (:left @date-atom-last-request))) (:value (:year (:left new-state))))
                   (vreset! date-atom-last-request new-state)
                   (reload-fn :left true))
                 (when-not (= (:value (:month (:left @date-atom-last-request))) (:value (:month (:left new-state))))
                   (vreset! date-atom-last-request new-state)
                   (reload-fn :left true))
                 (when-not (= (:value (:year (:right @date-atom-last-request))) (:value (:year (:right new-state))))
                   (vreset! date-atom-last-request new-state)
                   (reload-fn :right true))
                 (when-not (= (:value (:month (:right @date-atom-last-request))) (:value (:month (:right new-state))))
                   (vreset! date-atom-last-request new-state)
                   (reload-fn :right true))))))
