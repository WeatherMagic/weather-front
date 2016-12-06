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
                           (vreset! loaded true))
                         :error-callback
                         (fn [event]
                           (.error js/console "Failed to load image."
                                   (aget (.-path event) 0)))
                         :src    path
                         :filter [glc/linear glc/linear]
                         :cors   ""})]
    {:texture texture :loaded loaded}))

(defn load-texture-if-needed
  [textures gl-ctx & paths]
  "Load a texture from the given path into the given WebGL context and
  a reference to it along with an indicator as to whether the texture
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

(defn load-data
  "Loads data from thor into a texture. Returs a map with {:key
  str :map texture-map} where :key holds how to find the newly loaded
  texture in texture-map."
  [texture-map gl-ctx & {:keys [variable request-params] :or {variable "temperature"}}]
  (let [request-map (merge {:from-year        2083
                            :to-year          2083
                            :from-month       2
                            :to-month         2
                            :from-longitude  -14
                            :to-longitude     50
                            :from-latitude    30
                            :to-latitude      70
                            :return-dimension "[1400, 1600]"}
                           request-params)
        url (str "http://thor.hfelo.se/api/" variable
                 (util/map->query-string request-map))
        key (keyword (util/get-filename url))]
    {:key key :map (load-texture-if-needed texture-map gl-ctx url)}))

(defn load-data-into-atom-and-return-key!
  "Load a texture if needed and mutate the given atom to contain
  it. Return the key of the newly loaded texture."
  [texture-map-atom gl-ctx & {:keys [variable request-params] :or {variable "temperature"}}]
  (let [ret-val (load-data @texture-map-atom gl-ctx :variable variable :request-params request-params)]
    (swap! texture-map-atom merge (:map ret-val))
    (:key ret-val)))

(defn load-base-textures
  [gl-ctx]
  (load-texture-if-needed {} gl-ctx "img/earth.jpg" "img/trump.png" "img/space5.jpg"))
