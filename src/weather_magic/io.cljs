(ns weather-magic.io)

(defn example-request-climate-data []
	#((go (let [temperature-data (<! (http/post "http://thor.hfelo.se/api/temperature"
                                          {:json-params {:from-year 2080
                                                         :to-year 2080
                                                         :from-month 6
                                                         :to-month 6
                                                         :from-longitude 32
                                                         :to-longitude 33
                                                         :zoom-level 1
                                                         :from-latitude 37
                                                         :to-latitude 38}}))]
      (prn (:body temperature-data))))))


(defn request-climate-data
  [dimension from-y to-y from-m to-m from-long to-long
   from-lat to-lat zoom-lev]
   
   (go (let [climate-data (<! (http/post (str "http://thor.hfelo.se/api/" dimension)
                                          {:json-params {:from-year from-y
                                                         :to-year to-y
                                                         :from-month from-m
                                                         :to-month to-m
                                                         :from-longitude from-long
                                                         :to-longitude to-long
                                                         :from-latitude from-lat
                                                         :to-latitude to-lat
                                                         :zoom-level 1}}))]
        (prn (:body climate-data)))))