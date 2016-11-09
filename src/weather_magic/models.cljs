(ns weather-magic.models
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.sphere :as s]
   [thi.ng.geom.plane :as p]
   [thi.ng.geom.attribs :as attr]
   [thi.ng.geom.rect :as rect]))

(defonce sphere
  (-> (s/sphere 1)
      (g/center)
      (g/as-mesh {:mesh    (glm/gl-mesh 4096 (set '(:uv :vnorm)))
                  :res     32
                  :attribs {:uv    (attr/supplied-attrib
                                    :uv (fn [[u v]] (vec2 (- 1 u) v)))
                            :vnorm (fn [_ _ v _] (m/normalize v))}})))

(defonce plane
  (g/as-mesh
    (rect/rect 4 3)
    {:mesh    (glm/gl-mesh 4096 (set '(:uv :vnorm)))
                  :res     32
                  :attribs {:uv    (attr/supplied-attrib
                                    :uv (fn [[u v]] (vec2 (- 1 u) v)))
                            :vnorm (fn [_ _ v _] (m/normalize v))}})))
