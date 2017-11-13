(ns nlptools.tool.stopwords
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [duct.logger :refer [log]]
   [nlptools.tool.core :as tool]
   [nlptools.command :as cmd]
   [nlptools.model.core :as model])
  (:import
   (opennlp.tools.tokenize Tokenizer)
   ))


(def ukey
  "this unit key"
  :nlptools.tool/stopwords)

(def cmdkey
  "the command key for this unit"
  :tool.stopwords)

(derive ukey tool/corekey)




(def punctuation #{"," "." " " "?" "!"})


(defn split-words
  [text]
  (str/split text #"\s+"))

(defrecord StopwordsTool [stopwords tokenizer filepath logger]
  tool/Tool
  (build-tool! [this]
    (log @logger :info ::build-tool {:filepath filepath})
    (reset! stopwords (into (hash-set) (-> filepath
                                           slurp
                                           split-words
                                           ))))
  (set-logger! [this newlogger]
    (reset! logger newlogger))
  (apply-tool [this text]
    (log @logger :debug ::apply-tool {:text text})
    (->> text
         str/lower-case
         (.tokenize ^Tokenizer (model/get-model tokenizer))
         (remove punctuation)
         (remove @stopwords))))

(defmethod ig/init-key ukey [_ spec]
  (let [{:keys [filepath logger tokenizer] :or {filepath (io/resource "stop_words.ro")}} spec]
    (log logger :debug ::init)
    (let [tool (->StopwordsTool (atom nil) tokenizer filepath (atom nil))]
      (tool/set-logger! tool logger)
      (tool/build-tool! tool)
      tool)))

(defmethod cmd/help cmdkey [_]
  (str (name cmdkey) " - remove stopwords from the input"))

(defmethod cmd/syntax cmdkey [_]
  (str "nlptools " (name cmdkey) " -t TEXT"))

(defmethod cmd/run cmdkey [_ options summary]
  (let [opts  (cmd/set-config options)
        config (merge (cmd/make-logger opts)
                      {ukey {:tokenizer (ig/ref :nlptools.model.tokenizer/simple)
                             :logger (ig/ref :duct.logger/timbre)}
                       :nlptools.model.tokenizer/simple {:logger  (ig/ref :duct.logger/timbre)}})
        system (ig/init (cmd/prep-igconfig config))
        stopwords (get system ukey)
        text (get opts :text "")]
    (printf "text         : %s,\nw/o stopwords: %s\n" text (str/join " "(tool/apply-tool stopwords text)))
    (ig/halt! system)
    0))
