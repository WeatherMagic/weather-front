(ns weather-magic.core
  (:require
   [weather-magic.ui               :as ui]
   [weather-magic.state            :as state]
   [weather-magic.shaders          :as shaders]
   [weather-magic.textures         :as textures]
   [weather-magic.event-handlers   :as event-handlers]
   [thi.ng.math.core               :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.core            :as gl]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.webgl.animator  :as anim]
   [thi.ng.geom.gl.shaders         :as sh]
   [thi.ng.geom.gl.glmesh          :as glm]
   [thi.ng.geom.gl.camera          :as cam]
   [thi.ng.geom.core               :as g]
   [thi.ng.geom.vector             :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix             :as mat :refer [M44]]
   [thi.ng.geom.sphere             :as s]
   [thi.ng.geom.attribs            :as attr]
   [thi.ng.color.core              :as col]
   [thi.ng.glsl.core               :as glsl :include-macros true]
   [thi.ng.glsl.vertex             :as vertex]
   [thi.ng.glsl.lighting           :as light]))

(enable-console-print!)

;;; On the other hand: The below def's and defn's can and will be reloaded by figwheel
;;; iff they're modified when the source code is saved.
(def shader-spec
  {:vs shaders/vs
   :fs (->> shaders/fs
            (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
            (glsl/assemble))
   :uniforms {:model      [:mat4 M44]
              :view       :mat4
              :proj       :mat4
              :normalMat  [:mat4 (gl/auto-normal-matrix :model :view)]
              :base       [:sampler2D 0] ; Specify which texture unit
              :trump      [:sampler2D 1] ; the uniform is bound to.
              :lightDir   [:vec3 [-1 -1 1]]
              :lightCol   [:vec3 [1 1 1]]
              :ambientCol [:vec3 [0 0 0.1]]}
   :attribs  {:position   :vec3
              :normal     :vec3
              :uv         :vec2}
   :varying  {:vUV        :vec2
              :vNormal    :vec3}
   :state    {:depth-test true}})

(defn set-model-matrix
  [t]
  (@state/earth-animation-fn t)
  (let [earth-orientation @state/earth-orientation]
    (-> M44
        (g/translate (:translation earth-orientation))
        (g/rotate-x (m/radians (:x-angle earth-orientation)))
        (g/rotate-y (m/radians (:y-angle earth-orientation)))
        (g/rotate-z (m/radians (:z-angle earth-orientation))))))

(defn combine-model-shader-and-camera
  [model shader-spec camera t]
  (-> model
      (gl/as-gl-buffer-spec {})
      (assoc :shader (sh/make-shader-from-spec state/gl-ctx shader-spec))
      (gl/make-buffers-in-spec state/gl-ctx glc/static-draw)
      (cam/apply camera)))

(defn draw-frame! [t]
  (when (= @state/textures-loaded @state/textures-to-be-loaded)
    (gl/bind @state/texture 0)
    (gl/bind textures/trump 1)
    (doto state/gl-ctx
      (gl/clear-color-and-depth-buffer 0 0 0 1 1)
      (gl/draw-with-shader (assoc-in (combine-model-shader-and-camera @state/model shader-spec @state/camera t)
                                     [:uniforms :model] (set-model-matrix t))))))

;; Start the demo only once.
(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))

;; Reagent UI cannot be mounted from a defonce if figwheel is to do its magic.
(def ui-mounted? (ui/mount-ui!))

(defonce hooked-up? (event-handlers/hook-up-events!))

;; This is a hook for figwheel, add stuff you want run after you save your source.
(defn on-js-reload [])
