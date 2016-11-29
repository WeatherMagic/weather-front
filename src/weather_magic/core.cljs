(ns weather-magic.core
  (:require
   [weather-magic.ui               :as ui]
   [weather-magic.state            :as state]
   [weather-magic.shaders          :as shaders]
   [weather-magic.textures         :as textures]
   [weather-magic.event-handlers   :as event-handlers]
   [thi.ng.math.core               :as m   :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.gl.core            :as gl]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.webgl.animator  :as anim]
   [thi.ng.geom.gl.shaders         :as sh]
   [thi.ng.geom.gl.glmesh          :as glm]
   [thi.ng.geom.gl.camera          :as cam]
   [thi.ng.geom.core               :as g]
   [thi.ng.geom.vector             :as v   :refer [vec2 vec3]]
   [thi.ng.geom.matrix             :as mat :refer [M44]]
   [thi.ng.geom.sphere             :as s]
   [thi.ng.geom.attribs            :as attr]
   [thi.ng.color.core              :as col]
   [thi.ng.glsl.core               :as glsl :include-macros true]
   [thi.ng.glsl.vertex             :as vertex]
   [thi.ng.glsl.lighting           :as light]))

(enable-console-print!)

(defn set-model-matrix
  [delta-time]
  (@state/earth-animation-fn delta-time)
  (m/* M44 @state/earth-orientation))

(defn combine-model-and-camera
  [model camera gl-ctx t]
  (-> model
      (gl/as-gl-buffer-spec {})
      (gl/make-buffers-in-spec gl-ctx glc/static-draw)
      (cam/apply camera)))

(defn align-animation
  "Function that handles alignment or zoom-alignment"
  []
  (let [delta-x (:delta-x @state/pointer-zoom-info)
        delta-y (:delta-y @state/pointer-zoom-info)
        delta-fov (:delta-fov @state/pointer-zoom-info)
        total-steps (:total-steps @state/pointer-zoom-info)
        current-step (:current-step @state/pointer-zoom-info)
        delta-z-angle (:delta-z-angle @state/pointer-zoom-info)]
    (event-handlers/update-zoom-point-alignment delta-x delta-y delta-z-angle current-step delta-fov)
    (swap! state/pointer-zoom-info assoc-in [:current-step] (inc current-step))
    (when (= current-step total-steps)
      (swap! state/pointer-zoom-info assoc :state false))))

(defn enable-shader-alpha-blending []
  (gl/prepare-render-state state/gl-ctx-left
                           {:blend true
                            :blend-fn [glc/src-alpha
                                       glc/one-minus-src-alpha]})
  (gl/prepare-render-state state/gl-ctx-right
                           {:blend true
                            :blend-fn [glc/src-alpha
                                       glc/one-minus-src-alpha]}))

(defn draw-frame! [t]
  (when (:state @state/pointer-zoom-info)
    (align-animation))
  (when (and @(:loaded @state/base-texture-left) @(:loaded (:trump @state/textures-left)))
    (let [range (- (:max (:year @state/date-atom)) (:min (:year @state/date-atom)))
          time (rem (int (* 5 t)) range)]
      (swap! state/date-atom assoc-in [:year :value] (+ (:min (:year @state/date-atom)) time))
      (gl/bind (:texture @state/base-texture-left) 0)
      (gl/bind (:texture (:trump @state/textures-left)) 1)
      (doto state/gl-ctx-left
        (gl/clear-color-and-depth-buffer 0 0 0 1 1)
        (gl/draw-with-shader
         (-> (combine-model-and-camera @state/model @state/camera-left state/gl-ctx-left t)
             (assoc :shader (@state/current-shader-key state/shaders-left))
             (assoc-in [:uniforms :model] (set-model-matrix (- t @state/time-of-last-frame)))
             (assoc-in [:uniforms :year]  time)
             (assoc-in [:uniforms :range] range)
             (assoc-in [:uniforms :fov] (:fov @state/camera-left)))))))
  (when (and @(:loaded @state/base-texture-right) @(:loaded (:trump @state/textures-right)))
    (let [range (- (:max (:year @state/date-atom)) (:min (:year @state/date-atom)))
          time (rem (int (* 5 t)) range)]
      (swap! state/date-atom assoc-in [:year :value] (+ (:min (:year @state/date-atom)) time))
      (gl/bind (:texture @state/base-texture-right) 0)
      (gl/bind (:texture (:trump @state/textures-right)) 1)
      (doto state/gl-ctx-right
        (gl/clear-color-and-depth-buffer 0 0 0 1 1)
        (gl/draw-with-shader
         (-> (combine-model-and-camera @state/model @state/camera-right state/gl-ctx-right t)
             (assoc :shader (@state/current-shader-key state/shaders-right))
             (assoc-in [:uniforms :model] (set-model-matrix (- t @state/time-of-last-frame)))
             (assoc-in [:uniforms :year]  time)
             (assoc-in [:uniforms :range] range)
             (assoc-in [:uniforms :fov] (:fov @state/camera-right))))))
    (vreset! state/time-of-last-frame t)))

;; Start the demo only once.
(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))

;; Reagent UI cannot be mounted from a defonce if figwheel is to do its magic.
(def ui-mounted? (ui/mount-ui!))

(defonce hooked-up? (event-handlers/hook-up-events!))

;; This is a hook for figwheel, add stuff you want run after you save your source.
(defn on-js-reload [])
