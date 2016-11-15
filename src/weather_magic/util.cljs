(ns weather-magic.util
  (:require
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [weather-magic.state            :as state]
   [weather-magic.shaders          :as shaders]))

(defn transparent-println
  "Print something and return that something."
  [item]
  (println item)
  item)

(defn toggle
  "Add item to set if it's not in set, remove item from set if it's already in it.
   In other words, toggle the item in the set."
  [set item]
  (transparent-println
   ((if (contains? set item) disj conj) set item)))

(defonce tex-ready-test (volatile! false))
(defonce tex-ready-test2 (volatile! false))

(defn set-view
  "Change mesh and animation function depening on view"
  [model-atom new-model animation-func func texture]
  (reset! model-atom new-model)
  (reset! animation-func func)
  (buf/load-texture state/gl-ctx {:callback (fn [tex img]
                                         (.generateMipmap state/gl-ctx (:target tex))
                                         (vreset! tex-ready-test true))
                             :src      texture
                             :filter   [glc/linear-mipmap-linear glc/linear]
                             :flip     false})
  (buf/load-texture state/gl-ctx2 {:callback (fn [tex2 img]
                                        (.generateMipmap state/gl-ctx2 (:target tex2))
                                        (vreset! tex-ready-test2 true))
                             :src      texture
                             :filter   [glc/linear-mipmap-linear glc/linear]
                             :flip     false}))
(defn switch-shader
  "Function to switch shader used to visualize"
  [shader-left shader-right]
  (swap! state/shader-selector-left (fn [] shader-left))
  (swap! state/shader-selector-right (fn [] shader-right)))
