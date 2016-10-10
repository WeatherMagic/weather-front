(ns weather-magic.state
  (:require
    [weather-magic.world :as world]
    [reagent.core :as reagent :refer [atom]]))

(defonce earth-animation-fn (atom world/spin))
(defonce earth-rotation (atom {:xAngle 24.5 :yAngle 0}))
