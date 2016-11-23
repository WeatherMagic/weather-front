(ns weather-magic.coordinate-transforms
(:require
 [weather-magic.state :as state]
 [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]))

(defn lat-lon-to-cart
  "Converting latitude and longitude to model cordinates"
  [lat lon]
  (let [rlat (m/radians lat)
        rlon (m/radians lon)]
    (reset! state/cart-point {:x (*( Math/sin rlon) (Math/cos rlat))
                        :y ( Math/sin rlat)
                        :z (*( Math/cos rlon) (Math/cos rlat) )})))

(defn cart-to-lat-lon
  "Converting latitude and longitude to model cordinates"
  [x y z]
  (let [eps 0.001]
  (if (> (Math/abs z) eps)
    (swap! state/geografic-point assoc :lon (* (/ 180 PI) (Math/atan2 x z)))
    (
     (if (> (Math/abs y) (- 1 eps) )
       (swap! state/geografic-point assoc :lon 0 )
       (
        (if (> x 0)
          (swap! state/geografic-point assoc :lon 90 )
          (swap! state/geografic-point assoc :lon -90 )
        )
       ))))))
