(ns weather-magic.cache-management
  (:require
   [weather-magic.state    :as state]
   [weather-magic.util     :as util]
   [weather-magic.textures :as textures]
   [thi.ng.geom.gl.core            :as gl])
  (:require-macros
   [weather-magic.macros   :refer [when-let*]]))

(defonce data-load-queued (atom {:left false :right false}))

(defn trigger-data-load!
  "Get climate data for the area currently in view on the screen."
  ([queue-if-already-loading]
    (trigger-data-load! :left queue-if-already-loading)
    (trigger-data-load! :right queue-if-already-loading))
  ([left-right-key queue-if-already-loading]
  (let [texture-keys (left-right-key @state/dynamic-texture-keys)
        time-data    (left-right-key @state/date-atom)
        gl-ctx       (if (= :left left-right-key) state/gl-ctx-left state/gl-ctx-right)
        texture-atom (if (= :left left-right-key) state/textures-left state/textures-right)]
    (if-not (contains? texture-keys :next)
      (let [next-key (textures/load-data-for-current-viewport-and-return-key!
                      texture-atom gl-ctx @state/earth-orientation 
                      @state/camera-right time-data @state/data-layer-atom)]
        (when-not (= (:current texture-keys) next-key)
          (swap! state/dynamic-texture-keys assoc-in [left-right-key :next] next-key)))
      (when queue-if-already-loading
        (swap! data-load-queued assoc left-right-key true))))))

(defn rotate-in-next!
  [left-right-key texture-atom]
  (when-let* [next-key (:next    (left-right-key @state/dynamic-texture-keys))
              old-key  (:current (left-right-key @state/dynamic-texture-keys))]
             (when @(:loaded (next-key @texture-atom))
               (swap! state/dynamic-texture-keys
                      #(-> % (assoc-in [left-right-key :current] next-key)
                           (util/dissoc-in [left-right-key :next])))
               (gl/release (:texture (old-key @texture-atom)))
               (swap! texture-atom  dissoc old-key)
               (when (left-right-key @data-load-queued)
                 (swap! data-load-queued assoc left-right-key false)
                 (trigger-data-load! false)))
             (when @(:failed (next-key @texture-atom))
               (swap! state/dynamic-texture-keys update-in [left-right-key :next] dissoc)
               (swap! texture-atom        dissoc next-key)
               (when (left-right-key @data-load-queued)
                 (swap! data-load-queued assoc left-right-key false)
                 (trigger-data-load! false)))))
