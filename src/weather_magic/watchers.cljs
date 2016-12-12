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

(defn mount-zoom-data-reload-watch
  [camera reload-fn]
  (let [cam-last-request (volatile! @camera)]
    (add-watch camera :trigger-data-load-zoom
               (fn [key reference old-state new-state]
                 (let [camera-z-pos-old (aget (.-buf (:eye @cam-last-request)) 2)
                       zoom-level-old (/ (- camera-z-pos-old 1.1) 3.0)
                       camera-z-pos-new (aget (.-buf (:eye new-state)) 2)
                       zoom-level-new (/ (- camera-z-pos-new 1.1) 3.0)]
                   (when (and (> zoom-level-old 0.16) (< zoom-level-new 0.16))
                     (vreset! cam-last-request new-state)
                     (reload-fn))
                   (when (and (> zoom-level-old 0.06) (< zoom-level-new 0.06))
                     (vreset! cam-last-request new-state)
                     (reload-fn))
                   (when (and (< zoom-level-old 0.16) (> zoom-level-new 0.16))
                     (vreset! cam-last-request new-state)
                     (reload-fn))
                   (when (and (< zoom-level-old 0.06) (> zoom-level-new 0.06))
                     (vreset! cam-last-request new-state)
                     (reload-fn)))))))
