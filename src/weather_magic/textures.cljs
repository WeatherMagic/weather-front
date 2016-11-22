(ns weather-magic.textures
  (:require
   [weather-magic.util             :as util]
   [thi.ng.geom.gl.buffers         :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]))

(defn load-texture [gl-ctx path]
  "Loads a texture from path and places it in a map along with a
  volatile indicating whether or not the texture has been loaded
  like: {:texture T :loaded (volatile! false)}"
  (let [loaded (volatile! false)
        texture (buf/load-texture
                 gl-ctx {:callback
                         (fn [tex img]
                           (.generateMipmap gl-ctx (:target tex))
                           (vreset! loaded true))
                         :src      path
                         :filter   [glc/linear-mipmap-linear glc/linear]})]
    {:texture texture :loaded loaded}))

(defn load-texture-if-needed
  [gl-ctx textures & paths]
  "Load a texture from the given path into the given WebGL context and
  a reference to it along with an inticator as to whether the texture
  has loaded or not into the given map.

  If the given path is '/img/earth.jpg' the texture will be entered
  into the map with the key :earth and it's value will be another map
  with two keys, :loaded and :texture where :loaded is the boolean
  true or false whether the texture is ready to use and :texture
  eventually holds the actual texture ready for use.

  So (load-texture X {} 'earth.png') will return:
    {:earth {:texture T :loaded (volatile! false)}}
  where :loaded will turn true once the load is complete."
  (into textures
        (for [path paths]
          (let [name (util/get-filename path)]
            (when-not (contains? textures name)
              {(keyword name) (load-texture gl-ctx path)})))))

(defn load-base-textures
  [gl-ctx]
  (load-texture-if-needed gl-ctx {} "img/earth.jpg" "img/trump.png"))
