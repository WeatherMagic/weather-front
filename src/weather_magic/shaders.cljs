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
     vec4 diffuse = texture2D(base, vUV) + texture2D(trump, vUV);
     vec4 col = vec4(ambientCol, 1.0) + diffuse * vec4(lightCol, 1.0) * lam;
     gl_FragColor = col;
   }")
