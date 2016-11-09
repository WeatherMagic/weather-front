(ns weather-magic.shaders)

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
