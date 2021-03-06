(ns nlptools.module.db
  (:require
   [clojure.java.jdbc :as j]
   [hikari-cp.core :as hik]
   [integrant.core :as ig]
  ))

(defprotocol SQL
  (query [this sql row-fun resultset-fn]))


(defrecord SQLDb [dbspec]
  SQL
  (query [this sql row-fn resultset-fn]
    (let [row-fn-map (if row-fn {:row-fn row-fn})
         resultset-fn-map (if resultset-fn {:result-set-fn resultset-fn})]
        (j/query dbspec sql (merge row-fn-map resultset-fn-map)))))

(defmethod ig/init-key :nlptools.module/db [_ spec]
    (->SQLDb {:datasource (hik/make-datasource (dissoc spec :logger))}))


(defmethod ig/halt-key! :nlptools.module/db [_ db]
  (hik/close-datasource (get-in db [:dbspec :datasource])))
