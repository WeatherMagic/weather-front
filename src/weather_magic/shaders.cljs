(ns weather-magic.shaders
  (:require
   [thi.ng.geom.gl.core  :as gl]
   [thi.ng.glsl.core     :as glsl :include-macros true]
   [thi.ng.glsl.vertex   :as vertex]
   [thi.ng.glsl.lighting :as light]
   [thi.ng.geom.matrix   :as mat :refer [M44]]))

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
     vec4 diffuse = texture2D(base, vUV) + texture2D(trump, vUV);
     vec4 col = vec4(ambientCol, 1.0) + diffuse * vec4(lightCol, 1.0) * lam;
     gl_FragColor = col;
   }")

(def blend-fs
  "void main() {
     vec4 texture = texture2D(base, vUV);
     vec4 temperature;

     if(texture.g > 0.5) {
       temperature = vec4(1.0, 1.0 - texture.g, 0, 1.0);
     } else {
       temperature = vec4(texture.g, texture.g, 1.0, 1.0);
     }
     gl_FragColor = mix(temperature, texture, 0.5) + texture2D(trump, vUV);
  }")

(def temperature-fs
  "void main() {

    float temperatureTex1 = texture2D(base, vUV).r;
    float temperatureTex2 = texture2D(base, vUV).b;

    float temperature = mix(temperatureTex1, temperatureTex2, year/range);

    vec4 outColor;

    float threshold = fov/1000.0;
    if (fov > 45.0) {
      threshold = pow((90.0 - fov)/90.0, 3.0)/5.0 - 0.005;
    } 

    float alphaValue = clamp(15.0 / fov, 0.0, 1.0);

    if (mod(temperature, 0.1) < threshold && fov < 50.0 && temperature > 0.15) {
      if (temperature > 0.5 && temperature < 0.75) {
        outColor = vec4(0.5, 0.5, 0.5, alphaValue);
      } else if (temperature > 0.75) {
        outColor = vec4(0.0, 0.0, 0.0, alphaValue);
      } else if (temperature < 0.5) {
        outColor = vec4(1.0, 1.0, 1.0, alphaValue);
      }
    } else if(temperature > 0.5) {
      outColor = vec4(1.0, 1.0 - (2.0 * (temperature - 0.5)), 0, 1.0);
    } else {
      outColor = vec4(2.0 * temperature, 2.0 * temperature, 2.0 * (0.5 - temperature), 1.0);
    }
    gl_FragColor = outColor;
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
              :trump      [:sampler2D 1] ; the uniform is bound to.
              :lightDir   [:vec3 [1 0 1]]
              :lightCol   [:vec3 [1 1 1]]
              :ambientCol [:vec3 [0 0 0.1]]
              :year       :float
              :range      :float
              :fov        :float}

   :attribs  {:position :vec3
              :normal   :vec3
              :uv       :vec2}
   :varying  {:vUV      :vec2
              :vNormal  :vec3}
   :state    {:depth-test true}})

(def blend-shader-spec
  (assoc standard-shader-spec
         :fs (->> blend-fs
                  (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
                  (glsl/assemble))))

(def temperature-shader-spec
  (assoc standard-shader-spec
         :fs (->> temperature-fs
                  (glsl/glsl-spec-plain [vertex/surface-normal light/lambert])
                  (glsl/assemble))))
