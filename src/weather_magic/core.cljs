(ns weather-magic.core
  (:require
   [weather-magic.ui               :as ui]
   [weather-magic.state            :as state]
   [weather-magic.shaders          :as shaders]
   [weather-magic.transforms       :as transforms]
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

(defn enable-shader-alpha-blending []
  (gl/prepare-render-state state/gl-ctx-left
                           {:blend true
                            :blend-fn [glc/src-alpha
                                       glc/one-minus-src-alpha]})
  (gl/prepare-render-state state/gl-ctx-right
                           {:blend true
                            :blend-fn [glc/src-alpha
                                       glc/one-minus-src-alpha]}))

(defn update-year-info
  [t left-right-key]
  (let [min  (:min (:year (left-right-key @state/date-atom)))
        range (- (:max (:year (left-right-key @state/date-atom))) min)
        current-year (:value (:year (left-right-key @state/date-atom)))
        last-year-update (:time-of-last-update (:year (left-right-key @state/year-update)))
        delta-year (int (- (* 5 t) last-year-update))]
    (when (> delta-year 0.5)
      (swap! state/date-atom assoc-in [left-right-key :year :value] (+ min (rem (- (+ current-year delta-year) min) range)))
      (swap! state/year-update assoc-in [left-right-key :year :time-of-last-update] (* 5 t)))))

(defn update-month-info
  [t left-right-key]
  (let [min  (:min (:month (left-right-key @state/date-atom)))
        range (- (:max (:month (left-right-key @state/date-atom))) min)
        current-year (:value (:month (left-right-key @state/date-atom)))
        last-year-update (:time-of-last-update (:month (left-right-key @state/year-update)))
        delta-month (int (- (* 1 t) last-year-update))]
    (when (> delta-month 0.5)
      (swap! state/date-atom assoc-in [left-right-key :month :value] (+ min (rem (- (+ current-year delta-month) min) range)))
      (swap! state/year-update assoc-in [left-right-key :month :time-of-last-update] (* 1 t)))))

(defn draw-in-context
  [gl-ctx camera background-camera base-texture textures shaders left-right-key year-month-key t]
  (let [range (- (:max  (year-month-key (left-right-key @state/date-atom)))
                 (:min  (year-month-key (left-right-key @state/date-atom))))
        time (- (:value (year-month-key (left-right-key @state/date-atom)))
                (:min   (year-month-key (left-right-key @state/date-atom))))]
    ;; Begin rendering when we have a background-texture of the earth.
    (when (and @(:loaded base-texture) @(:loaded (:trump textures)))
      (gl/bind (:texture (:space5 textures)) 0)
      ;; Do the actual drawing.
      (doto gl-ctx
        (gl/clear-color-and-depth-buffer 0 0 0 1 1)
        (gl/draw-with-shader
         (-> (cam/apply (:plane (left-right-key state/models)) background-camera)
             (assoc :shader (:space shaders))
             (assoc-in [:uniforms :model] (-> M44 (g/rotate-z PI) (g/scale 4.109) (g/translate (vec3 -5 -4 -1))))
             (assoc-in [:uniforms :uvLeftRightOffset] (if (= left-right-key :left) (* 0 1.0) (* 0.5 1.0)))
             (assoc-in [:uniforms :uvOffset]   @state/space-offset))))
      (gl/bind (:texture base-texture) 0)
      (if @(:loaded ((:current @state/dynamic-texture-keys) textures))
        (gl/bind (:texture ((:current @state/dynamic-texture-keys) textures)) 1)
        (gl/bind (:texture (:trump textures)) 1))
      (doto gl-ctx
        (gl/draw-with-shader
         (-> (cam/apply (@state/current-model-key (left-right-key state/models)) camera)
             (assoc :shader (@state/current-shader-key shaders))
             (assoc-in [:uniforms :model] (set-model-matrix (-  (* 5 t) @state/time-of-last-frame)))
             (assoc-in [:uniforms :year]  time)
             (assoc-in [:uniforms :range] range)
             (assoc-in [:uniforms :fov] (:fov camera))
             (assoc-in [:uniforms :dataScale] (vec2 0.05 0.05))
             (assoc-in [:uniforms :dataPos] (vec2 0.51 0.2))))))))

(defn draw-frame! [t]
  (transforms/update-lat-lon)
  (if (:play-mode (:year (:left @state/date-atom)))
    (update-year-info t :left)
    (swap! state/year-update assoc-in [:left :year :time-of-last-update] (* 5 t)))
  (if (:play-mode (:month (:left @state/date-atom)))
    (update-month-info t :left)
    (swap! state/year-update assoc-in [:left :month :time-of-last-update] (* 1 t)))
  (if (:play-mode (:year (:right @state/date-atom)))
    (update-year-info t :right)
    (swap! state/year-update assoc-in [:right :year :time-of-last-update] (* 5 t)))
  (if (:play-mode (:month (:right @state/date-atom)))
    (update-month-info t :right)
    (swap! state/year-update assoc-in [:right :month :time-of-last-update] (* 1 t)))
  (draw-in-context state/gl-ctx-left @state/camera-left @state/background-camera-left @state/base-texture-left @state/textures-left state/shaders-left :left :year t)
  (draw-in-context state/gl-ctx-right @state/camera-right @state/background-camera-right @state/base-texture-right @state/textures-right state/shaders-right :right :year t)
  (vreset! state/time-of-last-frame (* 5 t)))

;; Start the demo only once.
(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))

;; Reagent UI cannot be mounted from a defonce if figwheel is to do its magic.
(def ui-mounted? (ui/mount-ui!))

(defonce hooked-up? (event-handlers/hook-up-events!))

;; This is a hook for figwheel, add stuff you want run after you save your source.
(defn on-js-reload [])
