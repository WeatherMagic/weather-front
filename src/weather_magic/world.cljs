(ns weather-magic.world)

(defn show-europe
  "Rotates the sphere so that Europe is shown."
  [earth-atom t]
  (reset! earth-atom {:xAngle 45 :yAngle 80}))

(defn spin
  "Rotates the sphere indefinitely."
  [earth-atom t]
  (reset! earth-atom {:xAngle 24.5 :yAngle t}))
