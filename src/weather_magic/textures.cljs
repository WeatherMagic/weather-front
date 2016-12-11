(ns weather-magic.textures
  (:require
   [weather-magic.util             :as util]
   [thi.ng.geom.gl.buffers         :as buf]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [weather-magic.transforms       :as transforms]))

(enable-console-print!)

(defn load-texture [gl-ctx path]
  "Loads a texture from path and places it in a map along with a
  volatile indicating whether or not the texture has been loaded
  like: {:texture T :loaded (volatile! false)}"
  (let [loaded (volatile! false)
        failed (volatile! false)
        texture (buf/load-texture
                 gl-ctx {:callback
                         (fn [tex img]
                           (vreset! loaded true))
                         :error-callback
                         (fn [event]
                           (vreset! failed true)
                           (.warn js/console "Failed to load image."
                                  (if (exists? (.-path event))
                                    (aget (.-path event) 0)
                                    (.-target event))))
                         :src    path
                         :filter [glc/linear glc/linear]
                         :cors   ""
                         :format glc/rgba})]
    {:texture texture :loaded loaded :failed failed}))

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

  The third argument is a map in which we use the following keys:

  :variable       - The type of data to request from the backend,
                    normally 'temperature' or 'precipitation'.
  :request-params - A map of arguments to be passed on to thor in the
                    HTTP GET request in the form of a query string.
  :placement      - Positioning data to be associated with the loaded data.

  Returns a map with {:key str :map texture-map} where :key holds how
  to find the newly loaded texture in texture-map."
  [texture-map gl-ctx {variable :variable request-params :request-params placement :placement
                       :or {variable "temperature"}}]
  (println "request year: " (:year request-params))
  (let [request-map (merge {:year              2083
                            :month             12
                            :from-longitude    5
                            :to-longitude      58
                            :from-latitude     61
                            :to-latitude       73
                            :climate-model     "ICHEC-EC-EARTH"
                            :exhaust-level     "rcp45"
                            :height-resolution 1024}
                           (if (< (:year request-params) 2006)
                             (assoc request-params :exhaust-level "historical")
                             request-params))
        query-string (util/map->query-string request-map)
        url (str "http://thor.hfelo.se/api/" variable query-string)
        key (keyword query-string)
        texture-map (load-texture-if-needed texture-map gl-ctx url :key-fn (fn [_] key))]
    (println "after load-texture-if-needed")
    {:key key
     :map (assoc-in texture-map [key :placement] placement)}))

(defn load-data-into-atom-and-return-key!
  "Load a texture if needed and mutate the given atom to contain
  it. Return the key of the newly loaded texture."
  [texture-map-atom gl-ctx options]

  (let [ret-val (load-data @texture-map-atom gl-ctx options)]
    (swap! texture-map-atom merge (:map ret-val))
    (:key ret-val)))

(defn load-base-textures
  [gl-ctx]
  (-> {}
      (load-texture-if-needed gl-ctx "img/earth.jpg")
      (load-texture-if-needed gl-ctx "img/trump.png")
      (load-texture-if-needed gl-ctx "img/space.jpg")))

(defn load-data-for-current-viewport-and-return-key!
  "AKA the tightly coupled monster function of doom with an argument
  list so large it eclipses the sun."
  [textures-atom gl-ctx earth-orientation camera current-time-data]
  (let [lat-lon-corners (transforms/get-lat-lon-map earth-orientation camera)
        placement       (transforms/get-texture-position-map lat-lon-corners)]
    (load-data-into-atom-and-return-key! textures-atom gl-ctx
                                         {:request-params (merge lat-lon-corners {:year (:value (:year current-time-data))
                                                                                  :month (:value (:month current-time-data))})
                                          :placement placement})))
