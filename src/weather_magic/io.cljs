(ns weather-magic.io
  (:require-macros
   [cljs.core.async.macros         :refer [go]])
  (:require
   [cljs-http.client               :as http]
   [cljs.core.async                :refer [<!]]))

(enable-console-print!)

(defn request-climate-data
  "Requests climate data from thor webserver of dimension and size
   specified by input [dimension json-p]"
  [dimension json-p]
  (go (let [climate-data (<! (http/post (str "http://thor.hfelo.se/api/" dimension) ; <! takes http response
                                        {:json-params json-p}))]
        climate-data)))

(defn request-example-temp-data
  "Requests example temperature data from thor webserver and prints it"
  []
  (let [data   {:from-year 2082
                :to-year 2082
                :from-month 6
                :to-month 12
                :from-longitude 1
                :to-longitude 5
                :from-latitude 37
                :to-latitude 45
                :return-dimension [2 4 3]}]
    (let [climate-data (request-climate-data "temperature" data)]
      (go (println (<! climate-data))))))