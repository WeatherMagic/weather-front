(ns weather-magic.io
  (:require-macros
   [cljs.core.async.macros         :refer [go]])
  (:require
   [cljs-http.client               :as http]
   [cljs.core.async                :refer [<!]]))

(enable-console-print!)

;; Requests example temperature data from thor webserver and prints it
(defn request-example-temp-data
  []
  (go (let [example-temp-data (<! (http/post (str "http://thor.hfelo.se/api/temperature")
                                             {:json-params {:from-year 2082
                                                            :to-year 2082
                                                            :from-month 6
                                                            :to-month 12
                                                            :from-longitude 1
                                                            :to-longitude 5
                                                            :from-latitude 37
                                                            :to-latitude 45
                                                            :return-dimension [2 4 3]}}))]
        (prn (:body example-temp-data)))))

;; Function that requests climate data from thor webserver
(defn http-data-request [dimension json-p]
  (go (let [climate-data (<! (http/post (str "http://thor.hfelo.se/api/" dimension) ; <! takes http response
                                        {:json-params json-p}))]
        climate-data)))

;; Function that construct a map of json-parameters from arguments and
;; requests climate data from thor webserver with that map
(defn request-climate-data
  ([dimension return-dim from-long to-long
    from-lat to-lat from-y to-y from-m to-m]

   (let [json-p {:return-dimension return-dim
                 :from-longitude from-long
                 :to-longitude to-long
                 :from-latitude from-lat
                 :to-latitude to-lat
                 :from-year from-y
                 :to-year to-y
                 :from-month from-m
                 :to-month to-m}]
     (http-data-request dimension json-p)))

  ([dimension return-dim from-long to-long
    from-lat to-lat from-y to-y] ; No month limits

   (let [json-p {:return-dimension return-dim
                 :from-longitude from-long
                 :to-longitude to-long
                 :from-latitude from-lat
                 :to-latitude to-lat
                 :from-year from-y
                 :to-year to-y}]
     (http-data-request dimension json-p))))
