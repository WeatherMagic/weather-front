(ns weather-magic.textures
  (:require
   [weather-magic.util             :as util]
   [thi.ng.geom.gl.buffers         :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [weather-magic.transforms       :as transforms]))

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
                                   (aget (.-path event) 0) event))
                         :src    path
                         :filter [glc/linear glc/linear]
                         :cors   ""})]
    {:texture texture :loaded loaded}))

(defn load-texture-if-needed
  [textures gl-ctx path & {:keys [key-fn] :or {key-fn #(keyword (util/get-filename %))}}]
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
        (let [name (key-fn path)]
          (when-not (contains? textures name)
            {name (load-texture gl-ctx path)}))))

(defn load-data
  "Loads data from thor into a texture.

  There are three optional associative arguments:

  :variable       - The type of data to request from the backend,
                    normally 'temperature' or 'percipitation'.
  :request-params - A map of arguments to be passed on to thor in the
                    HTTP GET request in the form of a query string.
  :placement      - Positioning data to be associated with the loaded data.

  Returs a map with {:key str :map texture-map} where :key holds how
  to find the newly loaded texture in texture-map."
  [texture-map gl-ctx & {:keys [variable placement request-params]
                         :or {variable "temperature"}}]
  (let [request-map (merge {:year              2083
                            :month             1
                            :from-longitude   -17
                            :to-longitude      50
                            :from-latitude     40
                            :to-latitude       80
                            :climate-model     "CNRM-CERFACS-CNRM-CM5"
                            :exhaust-level     "rcp45"
                            :height-resolution 1024}
                           request-params)
        query-string (util/map->query-string request-map)
        url (str "http://thor.hfelo.se/api/" variable query-string)
        key (keyword query-string)
        texture-map (load-texture-if-needed texture-map gl-ctx url :key-fn (fn [_] key))]
    {:key key
     :map (assoc-in texture-map [key :placement] placement)}))

(defn load-data-into-atom-and-return-key!
  "Load a texture if needed and mutate the given atom to contain
  it. Return the key of the newly loaded texture."
  [texture-map-atom gl-ctx & {:keys [variable request-params placement]
                              :or {variable "temperature"}}]
  (let [ret-val (load-data @texture-map-atom gl-ctx :variable variable
                           :request-params request-params :placement placement)]
    (swap! texture-map-atom merge (:map ret-val))
    (:key ret-val)))

(defn load-base-textures
  [gl-ctx]
  (-> {}
      (load-texture-if-needed gl-ctx "img/earth.jpg")
      (load-texture-if-needed gl-ctx "img/trump.png")))

(defn load-data-for-current-viewport-and-return-key!
  "AKA the tightly coupled monster function of doom with an argument
  list so large it eclipses the sun."
  [textures-left-atom textures-right-atom
   gl-ctx-left gl-ctx-right earth-orientation camera-left]
  (let [lat-lon-corners (transforms/get-lat-lon-map earth-orientation camera-left)
        placement       (transforms/get-texture-position-map lat-lon-corners)]
    (load-data-into-atom-and-return-key! textures-left-atom gl-ctx-left
                                         :request-params lat-lon-corners
                                         :placement placement)
    (load-data-into-atom-and-return-key! textures-right-atom gl-ctx-right
                                         :request-params lat-lon-corners
                                         :placement placement)))
