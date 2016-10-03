(ns weather-magic.ui
  (:require
   [reagent.core :as reagent :refer [atom]]))

   ;; TIME-SLIDER
   (def date-atom (reagent/atom {:year {:value 2016 :min 1950 :max 2100} :month {:value 1 :min 1 :max 12}}))

   (defn slider [key value min max]
     [:input {:type "range" :value value :min min :max max
              :style {:width "100%"}
              :on-change (fn [e]
                           (swap! date-atom assoc-in [key :value] (.-target.value e)))}])

   (defn slider-component [key]
     (let [data (key @date-atom)]
      [:div {:class "time-slider" :id (name key)}
       [:span {:style {:color "white"}} (name key) ": " (:value data)]
       [slider key (:value data) (:min data) (:max data)]]))

   (defn time-slider []
     [:div
       [slider-component :year]
       [slider-component :month]])

(defn mount-ui!
  "Place the user interface into the DOM."
  []
  ;; We mount the map-ui by default into our UI <span>. If we, for
  ;; example, add a welcome UI later on it might instead be the default
  ;; UI to be mounted.
  (reagent/render [time-slider] (.getElementById js/document "ui"))
  true) ; Return true.
