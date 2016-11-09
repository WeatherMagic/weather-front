(ns weather-magic.core
  (:require
   [weather-magic.ui               :as ui]
   [weather-magic.state            :as state]
   [weather-magic.shaders          :as shaders]
   [weather-magic.event-handlers   :as event-handlers]
   [thi.ng.math.core               :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.core            :as gl]
   [weather-magic.models :as models]
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.webgl.animator  :as anim]
   [thi.ng.geom.gl.buffers         :as buf]
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

;;; The below defonce's cannot and will not be reloaded by figwheel.
(defonce tex-ready (volatile! false))

(defonce tex (buf/load-texture
              state/gl-ctx {:callback (fn [tex img]
                                        (.generateMipmap state/gl-ctx (:target tex))
                                        (vreset! tex-ready true))
                            :src      "img/earth.jpg"
                            :filter   [glc/linear-mipmap-linear glc/linear]
                            :flip     false}))

(defn set-model-matrix
  [t]
  (@state/earth-animation-fn state/earth-rotation t)
  (let [earth-rotation @state/earth-rotation]
    (-> M44
        (g/translate (:translation earth-rotation))
        (g/rotate-x (m/radians (:xAngle earth-rotation)))
        (g/rotate-y (m/radians (:yAngle earth-rotation)))
        (g/rotate-z (m/radians (:zAngle earth-rotation))))))

(defn combine-model-shader-and-camera
  [model shader-spec camera-atom]
  (-> model
      (gl/as-gl-buffer-spec {})
      (assoc :shader (sh/make-shader-from-spec state/gl-ctx shader-spec))
      (gl/make-buffers-in-spec state/gl-ctx glc/static-draw)
      (cam/apply @camera-atom)))

(defn draw-frame! [t]
  (if @tex-ready
    (doto state/gl-ctx
      (gl/clear-color-and-depth-buffer 0 0 0 1 1)
      (gl/draw-with-shader (update-in (combine-model-shader-and-camera model @state/shader-selector state/camera)
                                     [:uniforms] assoc :model (spin t) :frameCounter 1)))))

;; Start the demo only once.
(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))

;; Reagent UI cannot be mounted from a defonce if figwheel is to do its magic.
(def ui-mounted? (ui/mount-ui!))

(defonce hooked-up? (event-handlers/hook-up-events!))

;; This is a hook for figwheel, add stuff you want run after you save your source.
(defn on-js-reload [])
