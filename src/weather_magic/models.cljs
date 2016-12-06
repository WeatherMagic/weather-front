(ns weather-magic.models
  (:require
   [thi.ng.math.core      :as m]
   [thi.ng.geom.core      :as g]
   [thi.ng.geom.gl.core   :as gl]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.geom.vector    :refer [vec2]]
   [thi.ng.geom.sphere    :as s]
   [thi.ng.geom.plane     :as p]
   [thi.ng.geom.attribs   :as attr]
   [thi.ng.geom.rect      :as rect]))

(def sphere
  (-> (s/sphere 1)
      (g/as-mesh
       {:mesh    (glm/gl-mesh 8192 (set '(:uv :vnorm)))
        :res     64
        :attribs {:uv    (attr/supplied-attrib
                          :uv (fn [[u v]] (vec2 (- 1 u) v)))
                  :vnorm (fn [_ _ v _] (m/normalize v))}})
      (gl/as-gl-buffer-spec {})))

(def plane
  (-> (rect/rect 10 8)
      (g/as-mesh
       {:mesh    (glm/gl-mesh 4096 (set '(:uv :vnorm)))
        :res     32
        :attribs {:uv    (attr/supplied-attrib
                          :uv (fn [[u v]] (vec2 (- 1 u) v)))
                  :vnorm (fn [_ _ v _] (m/normalize v))}})
      (gl/as-gl-buffer-spec {})))
