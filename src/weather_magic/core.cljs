(ns weather-magic.core
  (:require
   [weather-magic.ui               :as ui]
   [weather-magic.util               :as util]
   [weather-magic.state            :as state]
   [weather-magic.shaders          :as shaders]
   [weather-magic.textures         :as textures]
   [weather-magic.watchers         :as watchers]
   [weather-magic.transforms       :as transforms]
   [weather-magic.event-handlers   :as event-handlers]
   [weather-magic.cache-management :as cache]
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

(defn update-year-month-info
  [t left-right-key year-month-key]
  (let [min  (:min (year-month-key (left-right-key @state/date-atom)))
        range (- (:max (year-month-key (left-right-key @state/date-atom))) min)
        current-year (:value (year-month-key (left-right-key @state/date-atom)))
        last-year-update (:time-of-last-update (year-month-key (left-right-key @state/year-update)))
        delta-year (int (- t last-year-update))]
    (when (> delta-year 0.5)
      (swap! state/date-atom assoc-in [left-right-key year-month-key :value]
             (+ min (rem (- (+ current-year delta-year) min) range)))
      (swap! state/year-update assoc-in [left-right-key year-month-key :time-of-last-update] t)
      (cache/trigger-data-load! left-right-key false))))

(defn draw-in-context
  [gl-ctx camera background-camera base-texture textures shaders left-right-key year-month-key t]
  (let [range (- (:max  (year-month-key (left-right-key @state/date-atom)))
                 (:min  (year-month-key (left-right-key @state/date-atom))))
        time (- (:value (year-month-key (left-right-key @state/date-atom)))
                (:min   (year-month-key (left-right-key @state/date-atom))))
        texture-info (:placement ((:current (left-right-key @state/dynamic-texture-keys)) textures))]
    ;; Begin rendering when we have a background-texture of the earth.
    (when (and @(:loaded base-texture) @(:loaded (:trump textures)))
      (gl/bind (:texture (:space textures)) 0)
      ;; Draw the background.
      (doto gl-ctx
        (gl/clear-color-and-depth-buffer 0 0 0 1 1)
        (gl/draw-with-shader
         (-> (cam/apply (:plane (left-right-key state/models)) background-camera)
             (assoc :shader (:space shaders))
             (assoc-in [:uniforms :model] (-> M44 (g/rotate-z PI) (g/scale 1.148) (g/translate (vec3 -5 -4 -1))))
             (assoc-in [:uniforms :uvLeftRightOffset] (if (= left-right-key :left) (* 0 1.0) (* 0.5 1.0)))
             (assoc-in [:uniforms :uvOffset]   @state/space-offset))))
      (gl/bind (:texture base-texture) 0)
      ;; If the data from thor has been loaded, use that instead of trump.
      (if @(:loaded ((:current (left-right-key @state/dynamic-texture-keys)) textures))
        (gl/bind (:texture ((:current (left-right-key @state/dynamic-texture-keys)) textures)) 1)
        (gl/bind (:texture (:trump textures)) 1))
      ;; Do the actual drawing.
      (doto gl-ctx
        (gl/draw-with-shader
         (-> (cam/apply (:sphere (left-right-key state/models)) camera)
             (assoc :shader (@state/current-shader-key shaders))
             (assoc-in [:uniforms :model] (set-model-matrix (- t @state/time-of-last-frame)))
             (assoc-in [:uniforms :year]  time)
             (assoc-in [:uniforms :range] range)
             (assoc-in [:uniforms :eye] (:eye camera))
             (assoc-in [:uniforms :dataScale] (:dataScale texture-info))
             (assoc-in [:uniforms :dataPos]   (:dataPos   texture-info))))))))

(defn draw-frame! [t]
  ;; If the next texture is loaded, set it to be the current texture and unload the old.
  (cache/rotate-in-next! :left state/textures-left)
  (cache/rotate-in-next! :right state/textures-right)
  (if (:play-mode (:year (:left @state/date-atom)))
    (update-year-month-info t :left :year)
    (swap! state/year-update assoc-in [:left :year :time-of-last-update] t))
  (if (:play-mode (:month (:left @state/date-atom)))
    (update-year-month-info t :left :month)
    (swap! state/year-update assoc-in [:left :month :time-of-last-update] t))
  (if (:play-mode (:year (:right @state/date-atom)))
    (update-year-month-info t :right :year)
    (swap! state/year-update assoc-in [:right :year :time-of-last-update] t))
  (if (:play-mode (:month (:right @state/date-atom)))
    (update-year-month-info t :right :month)
    (swap! state/year-update assoc-in [:right :month :time-of-last-update] t))
  (if (or (:play-mode (:month (:left @state/date-atom))) (:play-mode (:month (:right @state/date-atom))))
    (do (draw-in-context state/gl-ctx-left @state/camera-left @state/background-camera-left @state/base-texture-left @state/textures-left state/shaders-left :left :month t)
        (draw-in-context state/gl-ctx-right @state/camera-right @state/background-camera-right @state/base-texture-right @state/textures-right state/shaders-right :right :month t))
    (do (draw-in-context state/gl-ctx-left @state/camera-left @state/background-camera-left @state/base-texture-left @state/textures-left state/shaders-left :left :year t)
        (draw-in-context state/gl-ctx-right @state/camera-right @state/background-camera-right @state/base-texture-right @state/textures-right state/shaders-right :right :year t)))
  (vreset! state/time-of-last-frame t))

;; Start the demo only once.
(defonce running
  (anim/animate (fn [t] (draw-frame! t) true)))

;; Reagent UI cannot be mounted from a defonce if figwheel is to do its magic.
(def ui-mounted? (ui/mount-ui!))

(defonce hooked-up? (event-handlers/hook-up-events!))

(watchers/mount-rotation-data-reload-watch state/earth-orientation
                                           #(cache/trigger-data-load! true))

(watchers/mount-climate-model-info-data-reload-watch state/climate-model-info
                                                     #(cache/trigger-data-load! true))

(watchers/mount-variable-data-reload-watch state/data-layer-atom
                                           #(cache/trigger-data-load! true))

;; This is a hook for figwheel, add stuff you want run after you save your source.
(defn on-js-reload [])
