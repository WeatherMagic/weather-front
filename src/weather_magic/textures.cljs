(ns weather-magic.textures
  (:require
   [weather-magic.state            :as state]
   [thi.ng.geom.gl.buffers         :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]))

(defn load-texture [path]
  (vswap! state/textures-to-be-loaded inc)
  (buf/load-texture
   state/gl-ctx-left {:callback (fn [tex img]
                                  (.generateMipmap state/gl-ctx-left (:target tex))
                                  (vswap! state/textures-loaded inc))
                      :src      path
                      :filter   [glc/linear-mipmap-linear glc/linear]
                      :flip     false})
  (vswap! state/textures-to-be-loaded inc)
  (buf/load-texture
   state/gl-ctx-right {:callback (fn [tex img]
                                   (.generateMipmap state/gl-ctx-right (:target tex))
                                   (vswap! state/textures-loaded inc))
                       :src      path
                       :filter   [glc/linear-mipmap-linear glc/linear]
                       :flip     false}))

(defonce earth  (load-texture "img/earth.jpg"))
(defonce trump  (load-texture "img/trump.png"))
(defonce turkey (load-texture "img/turkey.jpg"))

;; THIS IS BAD AND I SHOULD FEEL BAD.
(reset! state/texture earth)
