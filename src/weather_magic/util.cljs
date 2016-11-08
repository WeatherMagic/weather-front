(ns weather-magic.util
  (:require
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [weather-magic.state            :as state]))

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

(defn set-view
  "Change mesh and animation function depending on view"
  [model-atom new-model animation-func func texture context]
  (reset! model-atom new-model)
  (reset! animation-func func)
  (buf/load-texture context {:callback (fn [tex img]
                                          (.generateMipmap context (:target tex))
                                          (vreset! tex-ready-test true))
                              :src      texture
                              :filter   [glc/linear-mipmap-linear glc/linear]
                              :flip     false}))
