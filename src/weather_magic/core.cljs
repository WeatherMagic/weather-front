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

(defonce last-time (atom 0))

(defn set-model-matrix
  [delta-time]
  (@state/earth-animation-fn delta-time)
  (m/* M44 @state/earth-orientation))

(defn combine-model-shader-and-camera
  [model shader-spec camera t]
  (-> model
      (gl/as-gl-buffer-spec {})
      (assoc :shader (sh/make-shader-from-spec state/gl-ctx shader-spec))
      (gl/make-buffers-in-spec state/gl-ctx glc/static-draw)
      (cam/apply camera)))

(defn draw-frame! [t]
  (when (= @state/textures-loaded @state/textures-to-be-loaded)
    (if (or (:state @state/pointer-zoom-info) (:state @state/northpole-up-pressed))
      (let [delta-x (:delta-x @state/pointer-zoom-info)
            delta-y (:delta-y @state/pointer-zoom-info)
            delta-fov (:delta-fov @state/pointer-zoom-info)
            total-steps (:total-steps @state/pointer-zoom-info)
            current-step (:current-step @state/pointer-zoom-info)
            delta-angle (:delta-angle @state/pointer-zoom-info)
            range (- (:max (:year @state/date-atom)) (:min (:year @state/date-atom)))
            time (rem (int (* 5 t)) range)]
        (event-handlers/update-pan2 delta-x delta-y delta-angle current-step delta-fov)
        (swap! state/pointer-zoom-info assoc-in [:current-step] (inc current-step))
        (when (= current-step total-steps)
          (reset! state/pointer-zoom-info {:state false}))
        (gl/bind @state/texture 0)
        (gl/bind textures/trump 1)
        (doto state/gl-ctx
          (gl/clear-color-and-depth-buffer 0 0 0 1 1)
          (gl/draw-with-shader (assoc-in (assoc-in (assoc-in (combine-model-shader-and-camera @state/model @state/current-shader @state/camera t)
                                                             [:uniforms :model] (set-model-matrix (- t @last-time)))
                                                   [:uniforms :year] time) [:uniforms :range] range))))

      (let [range (- (:max (:year @state/date-atom)) (:min (:year @state/date-atom)))
            time (rem (int (* 5 t)) range)]
        (swap! state/date-atom assoc-in [:year :value] (+ (:min (:year @state/date-atom)) time))
        (gl/bind @state/texture 0)
        (gl/bind textures/trump 1)
        (doto state/gl-ctx
          (gl/clear-color-and-depth-buffer 0 0 0 1 1)
          (gl/draw-with-shader (assoc-in (assoc-in (assoc-in (combine-model-shader-and-camera @state/model @state/current-shader @state/camera t)
                                                             [:uniforms :model] (set-model-matrix (- t @last-time)))
                                                   [:uniforms :year] time) [:uniforms :range] range)))))
    (reset! last-time t)))

;; Start the demo only once.
(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))

;; Reagent UI cannot be mounted from a defonce if figwheel is to do its magic.
(def ui-mounted? (ui/mount-ui!))

(defonce hooked-up? (event-handlers/hook-up-events!))

;; This is a hook for figwheel, add stuff you want run after you save your source.
(defn on-js-reload [])
