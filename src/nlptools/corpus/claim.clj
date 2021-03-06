(ns nlptools.corpus.claim
  (:require
  [integrant.core :as ig]
  [clojure.java.io :as io]
  [clojure.string :as str]
  [duct.logger :refer [log]]
  [clojure.test :refer :all]
  [clojure.spec.test.alpha :as stest]
  [clojure.spec.gen.alpha :as gen]
  [nlpcore.protocols :as core]
  [nlpcore.spec :as nsp]
  [nlptools.module.db :as db]
  [nlptools.corpus.core :as corpus])
  (:import
   [org.jsoup Jsoup]))


(def ukey
  "this unit key"
  :nlptools.corpus/claim)

(def cmdkey
  "the command key for this unit"
  :corpus.claim)

(derive ukey corpus/corekey)



(defn strip-html-tags
  "Function strips HTML tags from string."
  [s]
  (.text (Jsoup/parse s)))

(defn filter-row [row]
  (-> row
      :text
      strip-html-tags))

(defn write-corpus! [filename, resultset]
  (with-open [^java.io.BufferedWriter w (clojure.java.io/writer filename)]
    (reduce  (fn [total ^String row]
               (if (str/blank? row)
                 total
                 (do
                   (.write w row)
                   (.newLine w)
                   (inc total))))
             0 resultset)))


(defrecord ClaimCorpus [id filepath db logger]
  core/Corpus
  (build-corpus! [this]
    (log @logger :info ::creating-corpus {:file filepath})
    (db/query db ["select sheet_text as text from  sheets where subsidiary_id = 1"]
            filter-row
            (partial write-corpus! filepath))
    this)
  (get-corpus [this] filepath))
 

(extend ClaimCorpus
  core/Module
  core/default-module-impl)

(defmethod ig/init-key ukey [_ spec]
  (let [{:keys [id db filepath logger]} spec
        corpus (->ClaimCorpus id filepath db (atom nil))]
    (core/set-logger! corpus logger)
    corpus))



