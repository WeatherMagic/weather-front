(ns weather-magic.shaders)

(def vs
  "void main() {
     vUV = uv;
     vNormal = normal;
     gl_Position = proj * view * model * vec4(position, 1.0);
   }")

(def fs
  "void main() {
     float lam = lambert(surfaceNormal(vNormal, normalMat),
                         normalize(lightDir));
     vec3 diffuse = mix(texture2D(tex1, vUV).rgb, texture2D(tex2, vUV).rgb, fade);
     vec3 col = ambientCol + diffuse * lightCol
                * lam * vec3(1.2, 1.2, 1.2);
     gl_FragColor = vec4(col, 1.0);
   }")
