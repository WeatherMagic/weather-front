(ns weather-magic.shaders
  (:require
   [thi.ng.geom.gl.core  :as gl]
   [thi.ng.glsl.core     :as glsl :include-macros true]
   [thi.ng.glsl.vertex   :as vertex]
   [thi.ng.glsl.lighting :as light]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.matrix   :as mat :refer [M44]]))

(def space-vs
  "void main() {
     vUV         = uv;
     vNormal     = normal;
     gl_Position = proj * view * model * vec4(position, 1.0);
   }")

(def space-fs
  "void main() {
     gl_FragColor = texture2D(starsTex, vec2(mod(uvLeftRightOffset + (uvOffset.x + vUV.x) / 2.0, 1.0), mod((uvOffset.y + vUV.y) / 2.0, 1.0)));
   }")

(def standard-vs
  "void main() {
     vUV         = uv;
     vNormal     = normal;
     gl_Position = proj * view * model * vec4(position, 1.0);
   }")

(def standard-fs
  "void main() {
     float lam = lambert(surfaceNormal(vNormal, normalMat),
                         normalize(lightDir));

     float temperature = texture2D(data, mod((vUV - dataPos), 1.0) / dataScale).r;
     vec4 baseTexture = texture2D(base, vUV);

     vec4 temperatureColor;

     if (mod(temperature, 0.078125) < 0.003 && eye.z < 1.2) {
       if (temperature > 0.5) {
         temperatureColor = vec4(0.5, 0.5, 0.5, 1.0);
       } else if (temperature < 0.5) {
         temperatureColor = vec4(1.0, 1.0, 1.0, 1.0);
       }
     } else if(temperature > 0.5) {
       temperature = clamp(temperature, 0.5, 0.8);
       temperatureColor = vec4(1.0,
                               clamp(-3.333 * temperature + 2.67, 0.0, 1.0),
                               0.0, 1.0);
     } else {
       temperature = clamp(temperature, 0.344, 0.5);
       temperatureColor = vec4(clamp(6.41 * temperature - 2.205, 0.0, 1.0),
                               clamp(6.41 * temperature - 2.205, 0.0, 1.0),
                               clamp((-6.41 * temperature + 3.205), 0.0, 1.0), 
                               1.0);
     }

     float textureAlpha = texture2D(data, (vUV - dataPos) / dataScale).a;

     vec4 baseColor = vec4(ambientCol, 1.0) + baseTexture * vec4(lightCol, 1.0) * lam; 

     vec4 mixColor = baseColor * 0.6 + temperatureColor * textureAlpha * 0.4;

     gl_FragColor = mixColor; 
   }")

(def standard-shader-spec
  {:vs standard-vs
   :fs (->> standard-fs
            (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
            (glsl/assemble))
   :uniforms {:model      [:mat4 M44]
              :view       :mat4
              :proj       :mat4
              :normalMat  [:mat4 (gl/auto-normal-matrix :model :view)]
              :base       [:sampler2D 0] ; Specify which texture unit
              :data       [:sampler2D 1] ; the uniform is bound to.
              :lightDir   [:vec3 [1 0 1]]
              :lightCol   [:vec3 [1 1 1]]
              :ambientCol [:vec3 [0 0 0.1]]
              :year       :float
              :range      :float
              :eye        :vec3
              :dataScale  :vec2
              :dataPos    :vec2}

   :attribs  {:position   :vec3
              :normal     :vec3
              :uv         :vec2}
   :varying  {:vUV        :vec2
              :vNormal    :vec3}
   :state    {:depth-test true}})

(def space-shader-spec
  {:vs space-vs
   :fs (->> space-fs
            (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
            (glsl/assemble))
   :uniforms {:model              [:mat4 M44]
              :view               :mat4
              :proj               :mat4
              :normalMat          [:mat4 (gl/auto-normal-matrix :model :view)]
              :starsTex           [:sampler2D 0] ; Specify which texture unit
              :uvLeftRightOffset  :float
              :uvOffset           :vec2}

   :attribs  {:position   :vec3
              :normal     :vec3
              :uv         :vec2}
   :varying  {:vUV        :vec2
              :vNormal    :vec3}
   :state    {:depth-test true}})
