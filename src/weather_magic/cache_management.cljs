(ns weather-magic.cache-management
  (:require
   [weather-magic.state    :as state]
   [weather-magic.textures :as textures]
   [thi.ng.geom.gl.core            :as gl])
  (:require-macros
   [weather-magic.macros   :refer [when-let*]]))

(defonce data-load-queued (atom false))

(defn trigger-data-load!
  "Get climate data for the area currently in view on the screen."
  [queue-if-already-loading]
  ;; Don't load a new texture if we're already loading one.
  (let [texture-keys @state/dynamic-texture-keys]
    (if-not (contains? texture-keys :next)
      (let [next-key (textures/load-data-for-current-viewport-and-return-key!
                      state/textures-left state/textures-right state/gl-ctx-left state/gl-ctx-right
                      @state/earth-orientation @state/camera-left @state/data-layer-atom)]
        (when-not (= (:current texture-keys) next-key)
          (swap! state/dynamic-texture-keys assoc :next next-key)))
      (when queue-if-already-loading
        (reset! data-load-queued true)))))

(defn rotate-in-next!
  "If the next texture is loaded, set it to be the current texture and unload the old."
  []
  (when-let* [next-key (:next    @state/dynamic-texture-keys)
              old-key  (:current @state/dynamic-texture-keys)]
             (when @(:loaded (next-key @state/textures-left))
               (swap! state/dynamic-texture-keys
                      #(-> % (assoc :current next-key) (dissoc :next)))
               (gl/release (:texture (old-key @state/textures-left)))
               (gl/release (:texture (old-key @state/textures-right)))
               (swap! state/textures-left  dissoc old-key)
               (swap! state/textures-right dissoc old-key)
               (when @data-load-queued
                 (reset! data-load-queued false)
                 (trigger-data-load! false))
               (when @(:failed (next-key @state/textures-left))
                 (swap! state/dynamic-texture-keys dissoc :next)
                 (swap! state/textures-left        dissoc next-key)
                 (swap! state/textures-right       dissoc next-key)
                 (when @data-load-queued
                   (reset! data-load-queued false)
                   (trigger-data-load! false))))))
