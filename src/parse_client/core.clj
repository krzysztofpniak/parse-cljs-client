(ns parse-client.core)

(ns tags.parse-cljs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(def toJSON #(.stringify js/JSON (clj->js %)))
(def fromJSON #(js->clj (.parse js/JSON %) :keywordize-keys true))

;(println (fromJSON "{\"asd\" : [1,2,3]}"))

(def parseConfig (atom {}))

(defn parseInitialize [appId appSecret]
  (let 
    [currentUser (fromJSON
                   (.getItem js/localStorage (str "Parse/" appId "/currentUser")))]
  	(reset! parseConfig {:appId appId :secret appSecret :currentUser currentUser}))
)

(defn login [username password]
  (go
    (let [result (:body (<! (http/get "https://api.parse.com/1/login" {
        :with-credentials? false
        :headers {
          "X-Parse-Application-Id" (:appId @parseConfig)
          "X-Parse-REST-API-Key" (:secret @parseConfig)
        }
        :query-params {
          :username username
          :password password
        }
      })))]
      (.setItem js/localStorage (str "Parse/" (:appId @parseConfig) "/currentUser") (toJSON result))
      (swap! parseConfig assoc :currentUser result)
      result
    )
  )
)

(defn current-user []
  (:currentUser @parseConfig))

(defn cloud-function
  (
   	[functionName]
  	(cloud-function functionName {}))
  (
   	[functionName params]
  	(go
    	(let [result (:body (<! (http/post (str "https://api.parse.com/1/functions/" functionName) {
        :with-credentials? false
        :headers {
          "X-Parse-Application-Id" (:appId @parseConfig)
          "X-Parse-REST-API-Key" (:secret @parseConfig)
          "X-Parse-Session-Token" (-> @parseConfig :currentUser :sessionToken)
        }
      	})))]
      	(:result result)
    	)
  	)
  )
)

(defn query [className params]
  (go
    (let [result (:body (<! (http/get (str "https://api.parse.com/1/classes/" className) {
        :with-credentials? false
        :headers {
          "X-Parse-Application-Id" (:appId @parseConfig)
          "X-Parse-REST-API-Key" (:secret @parseConfig)
          "X-Parse-Session-Token" (-> @parseConfig :currentUser :sessionToken)
        }
        :query-params (merge {} params)
      })))]
      (:results result)
    )
  )
)
