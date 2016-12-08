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

     vec4 outColor;

     if (mod(temperature, 0.078125) < 0.003 && eye.z < 1.2) {
       if (temperature > 0.5) {
         outColor = vec4(0.5, 0.5, 0.5, 1.0);
       } else if (temperature < 0.5) {
         outColor = vec4(1.0, 1.0, 1.0, 1.0);
       }
     } else if(temperature > 0.5) {
       temperature = clamp(temperature, 0.5, 0.8);
       outColor = vec4(1.0,-3.333 * temperature + 1.998, 0.0, 1.0);
     } else {
       temperature = clamp(temperature, 0.2, 0.5);
       outColor = vec4(6.41 * (temperature-0.34), 6.41 * (temperature-0.34), (-6.41 * temperature + 3.205), 1.0);
     }

     float textureAlpha = texture2D(data, (vUV - dataPos) / dataScale).a;

     if (textureAlpha < 1.0) {
      outColor = vec4(0.0, 0.0, 0.0, 0.0);
     }    

     vec4 baseColor = vec4(ambientCol, 1.0) + baseTexture * vec4(lightCol, 1.0) * lam; 

     vec4 mixColor = baseColor * 0.6 + outColor * textureAlpha * 0.4;

     gl_FragColor = mixColor; 
   }")

(def blend-fs
  "void main() {
     float lam = lambert(surfaceNormal(vNormal, normalMat), normalize(lightDir));
     vec4 mapDiffuse = texture2D(base, vUV);
     float temp = texture2D(data, vUV).r * 3.0;
     gl_FragColor = vec4(ambientCol, 1.0) + mapDiffuse * vec4(lightCol, 1.0) * lam * 0.0001 + vec4(temp, temp, temp, 1.0);

  }")

(def temperature-fs
  "void main() {

    float temperatureTex1 = texture2D(base, vUV).b;
    float temperatureTex2 = texture2D(base, vUV).r;

    float temperature = mix(temperatureTex1, temperatureTex2, year/range);

    vec4 outColor;

    float threshold = eye.z/10.0;
    if (eye.z > 45.0) {
      threshold = pow((90.0 - eye.z)/90.0, 3.0)/5.0 - 0.005;
    }

    float alphaValue = 1.0;

    if (mod(temperature, 0.1) < threshold && eye.z < 50.0 && temperature > 0.15) {
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
    vec3 outcolor = outColor.rgb;

    gl_FragColor = vec4(outcolor, alphaValue);
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
