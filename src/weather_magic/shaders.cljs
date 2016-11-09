(ns weather-magic.shaders
  (:require
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.vertex             :as vertex]
   [thi.ng.glsl.lighting           :as light]
   [thi.ng.geom.matrix             :as mat :refer [M44]]))

(def standard-vs
  "void main() {
     vUV = uv;
     vNormal = normal;
     gl_Position = proj * view * model * vec4(position, 1.0);
   }")

(def standard-fs
  "void main() {
     float lam = lambert(surfaceNormal(vNormal, normalMat),
                         normalize(lightDir));
     vec3 diffuse = texture2D(tex, vUV).rgb;
     vec3 col = ambientCol + diffuse * lightCol
                * lam * vec3(1.2, 1.2, 1.2);
     gl_FragColor = vec4(diffuse,1.0);
     //gl_FragColor = vec4(col, 1.0);
   }")


(def temperature-vs
 "void main() {
    vUV = uv;
    vNormal = normal;
    gl_Position = proj * view * model * vec4(position, 1.0);
  }")

(def temperature-fs
 "void main() {
    float lam = lambert(surfaceNormal(vNormal, normalMat),
                        normalize(lightDir));
    vec3 diffuse = texture2D(tex, vUV).rgb;
    float temperature = texture2D(tex, vUV).g;
    vec4 outColor;

    if(temperature > 0.5) {
      outColor = vec4(1.0, 1.0 - temperature, 0, 1.0);
    } else {
      outColor = vec4(temperature, temperature, 1.0, 1.0);
    }
    gl_FragColor = outColor;
  }")

;;; On the other hand: The below def's and defn's can and will be reloaded by figwheel
;;; iff they're modified when the source code is saved.
(def shader-spec
  {:vs standard-vs
   :fs (->> standard-fs
            (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
            (glsl/assemble))
   :uniforms {:model      [:mat4 M44]
              :view       :mat4
              :proj       :mat4
              :normalMat  [:mat4 (gl/auto-normal-matrix :model :view)]
              :tex        :sampler2D
              :lightDir   [:vec3 [1 0 1]]
              :lightCol   [:vec3 [1 1 1]]
              :ambientCol [:vec3 [0 0 0.1]]
              :frameCounter [:int 0]}

   :attribs  {:position :vec3
              :normal   :vec3
              :uv       :vec2}
   :varying  {:vUV      :vec2
              :vNormal  :vec3}
   :state    {:depth-test true}})


(def shader-spec2
 {:vs temperature-vs
  :fs (->> temperature-fs
           (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
           (glsl/assemble))
  :uniforms {:model      [:mat4 M44]
             :view       :mat4
             :proj       :mat4
             :normalMat  [:mat4 (gl/auto-normal-matrix :model :view)]
             :tex        :sampler2D
             :lightDir   [:vec3 [1 0 1]]
             :lightCol   [:vec3 [1 1 1]]
             :ambientCol [:vec3 [0 0 0.1]]
             :frameCounter [:int 0]}

  :attribs  {:position :vec3
             :normal   :vec3
             :uv       :vec2}
  :varying  {:vUV      :vec2
             :vNormal  :vec3}
  :state    {:depth-test true}})
