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
     vec4 texture = texture2D(tex, vUV);
     vec4 col = ambientCol + texture * lightCol
                * lam * vec4(1.2, 1.2, 1.2, 1.0);
     gl_FragColor = texture;
     //gl_FragColor = col;
   }")

(def blend-fs
 "void main() {
    vec4 texture = texture2D(tex, vUV);
    vec4 temperature;

    if(texture.g > 0.5) {
      temperature = vec4(1.0, 1.0 - texture.g, 0, 1.0);
    } else {
      temperature = vec4(texture.g, texture.g, 1.0, 1.0);
    }
    gl_FragColor = mix(temperature, texture, 0.5);
  }")


(def temperature-fs
 "void main() {

   vec4 texture = texture2D(tex, vUV);
   vec4 temperature;

   if(texture.g > 0.5) {
     temperature = vec4(1.0, 1.0 - texture.g, 0, 1.0);
   } else {
     temperature = vec4(texture.g, texture.g, 1.0, 1.0);
   }
    gl_FragColor = temperature;
  }")

;;; On the other hand: The below def's and defn's can and will be reloaded by figwheel
;;; iff they're modified when the source code is saved.
(def standard-shader-spec
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
              :lightCol   [:vec4 [1 1 1 1]]
              :ambientCol [:vec4 [0 0 0.1 1.0]]
              :frameCounter [:int 0]}

   :attribs  {:position :vec3
              :normal   :vec3
              :uv       :vec2}
   :varying  {:vUV      :vec2
              :vNormal  :vec3}
   :state    {:depth-test true}})


(def blend-shader-spec
 {:vs standard-vs
  :fs (->> blend-fs
           (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
           (glsl/assemble))
  :uniforms {:model      [:mat4 M44]
             :view       :mat4
             :proj       :mat4
             :normalMat  [:mat4 (gl/auto-normal-matrix :model :view)]
             :tex        :sampler2D
             :lightDir   [:vec3 [1 0 1]]
             :lightCol   [:vec4 [1 1 1 1]]
             :ambientCol [:vec4 [0 0 0.1 1.0]]
             :frameCounter [:int 0]}

  :attribs  {:position :vec3
             :normal   :vec3
             :uv       :vec2}
  :varying  {:vUV      :vec2
             :vNormal  :vec3}
  :state    {:depth-test true}})

(def temperature-shader-spec
 {:vs standard-vs
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
             :ambientCol [:vec4 [0 0 0.1 1.0]]
             :frameCounter [:int 0]}

  :attribs  {:position :vec3
             :normal   :vec3
             :uv       :vec2}
  :varying  {:vUV      :vec2
             :vNormal  :vec3}
  :state    {:depth-test true}})
