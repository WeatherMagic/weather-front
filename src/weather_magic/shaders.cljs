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
     vec3 diffuse = texture2D(tex, vUV).rgb;
     vec3 col = ambientCol + diffuse * lightCol
                * lam * vec3(1.2, 1.2, 2.0);
     gl_FragColor = vec4(col, 1.0);
   }")
