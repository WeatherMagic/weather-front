(ns weather-magic.core
  (:require
   [weather-magic.ui :as ui]
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.geom.gl.buffers :as buf]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.sphere :as s]
   [thi.ng.geom.attribs :as attr]
   [thi.ng.color.core :as col]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.vertex :as vertex]
   [thi.ng.glsl.lighting :as light]))

(enable-console-print!)
(def x (atom 0))
(def y (atom 0))
(def earth-view (atom 0))

;;; The below defonce's cannot and will not be reloaded by figwheel.
(defonce gl-ctx (gl/gl-context "main"))
(defonce view-rect  (gl/get-viewport-rect gl-ctx))
(defonce tex-ready (volatile! false))
(defonce tex (buf/load-texture
              gl-ctx {:callback (fn [tex img]
                              (.generateMipmap gl-ctx (:target tex))
                              (vreset! tex-ready true))
                  :src      "img/earth.jpg"
                  :filter   [glc/linear-mipmap-linear glc/linear]
                  :flip     false}))

(defonce camera (atom (cam/perspective-camera {:eye    (vec3 0 0 1.5)
                                         :fov    90
                                         :aspect view-rect})))
;;; On the other hand: The below def's and defn's can and will be reloaded by figwheel
;;; iff they're modified when the source code is saved.
(def shader-spec
  {:vs "void main() {
          vUV = uv;
          vNormal = normal;
          gl_Position = proj * view * model * vec4(position, 1.0);
        }"
   :fs (->> "void main() {
               float lam = lambert(surfaceNormal(vNormal, normalMat), normalize(lightDir));
               vec3 diffuse = texture2D(tex, vUV).rgb;
               vec3 col = ambientCol + diffuse * lightCol * lam * vec3(1.2, 1.2, 1.0);
               gl_FragColor = vec4(col, 1.0);
             }"
            (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
            (glsl/assemble))
   :uniforms {:model      [:mat4 M44]
              :view       :mat4
              :proj       :mat4
              :normalMat  [:mat4 (gl/auto-normal-matrix :model :view)]
              :tex        :sampler2D
              :lightDir   [:vec3 [1 0 1]]
              :lightCol   [:vec3 [1 1 1]]
              :ambientCol [:vec3 [0 0 0.1]]}
   :attribs  {:position :vec3
              :normal   :vec3
              :uv       :vec2}
   :varying  {:vUV      :vec2
              :vNormal  :vec3}
   :state    {:depth-test true}})

(def model
  (-> (s/sphere 1)
      (g/center)
      (g/as-mesh {:mesh    (glm/gl-mesh 4096 #{:uv :vnorm})
                  :res     32
                  :attribs {:uv    (attr/supplied-attrib
                                    :uv (fn [[u v]] (vec2 (- 1 u) v)))
                            :vnorm (fn [_ _ v _] (m/normalize v))}})))

(defn spin
  [t earth-view]
  (if (= earth-view "Europe")
    (do (reset! x 45) (reset! y 80))
    (do (reset! x 24.5) (reset! y t)))
  (-> M44
      (g/rotate-x (m/radians @x))
      (g/rotate-y (m/radians @y))))

(defn combine-model-shader-and-camera
  [model shader-spec camera]
  (println @camera)
  (-> model
      (gl/as-gl-buffer-spec {})
      (assoc :shader (sh/make-shader-from-spec gl-ctx shader-spec))
      (gl/make-buffers-in-spec gl-ctx glc/static-draw)
      (cam/apply @camera)))

(defn set-view []
  [:div
    [:input {:type "button" :value "Europe" :id "europe"
                 :on-click #(set! earth-view "Europe")}]
    [:input {:type "button" :value "Spinning" :id "spinning"
                   :on-click #(set! earth-view "Spinning")}]])

(defn mount-root []
  (reagent/render [set-view] (.getElementById js/document "buttons")))


(defn draw-frame! [t]
  (if @tex-ready
    (doto gl-ctx
      (gl/clear-color-and-depth-buffer 0 0 0 1 1)
      (gl/draw-with-shader (assoc-in (combine-model-shader-and-camera model shader-spec camera)
                                     [:uniforms :model] (spin t earth-view))))))

;; Start the demo only once.
(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))
;; Reagent UI cannot be mounted from a defonce if figwheel is to do its magic.
(def ui-mounted? (ui/mount-ui!))

;; This is a hook for figwheel, add stuff you want run after you save your source.
(defn on-js-reload [])
