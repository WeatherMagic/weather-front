(ns weather-magic.io
  (:require-macros
   [cljs.core.async.macros         :refer [go]])
  (:require
   [cljs-http.client               :as http]
   [cljs.core.async                :refer [<!]]))

(defn request-example-temp-data
  []
  (go (let [turkey-temp-data (<! (http/post (str "http://thor.hfelo.se/api/temperature")
                                            {:json-params {:from-year 2082
                                                           :to-year 2082
                                                           :from-month 6
                                                           :to-month 12
                                                           :from-longitude 1
                                                           :to-longitude 5
                                                           :from-latitude 37
                                                           :to-latitude 45
                                                           :return-dimension [2 4 3]}}))]
        (prn (:body turkey-temp-data)))))

(defn request-climate-data
  ([dimension return-dim from-long to-long
    from-lat to-lat from-y to-y from-m to-m] ; Month limits as arguments

  (go (let [climate-data (<! (http/post (str "http://thor.hfelo.se/api/" dimension)
                                        {:json-params {:from-year from-y
                                                       :to-year to-y
                                                       :from-month from-m
                                                       :to-month to-m
                                                       :from-longitude from-long
                                                       :to-longitude to-long
                                                       :from-latitude from-lat
                                                       :to-latitude to-lat}}))]
        climate-data))))


