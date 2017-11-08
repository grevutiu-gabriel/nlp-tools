(ns nlptools.model.tokenizer
  (:require
   [integrant.core :as ig]
   [clojure.java.io :as io]
   [duct.logger :refer [log]]
   [nlptools.model.core :refer [Model]]
   [nlptools.command :as cmd])
  (:import
   (opennlp.tools.tokenize TokenizerME
                           SimpleTokenizer
                           WhitespaceTokenizer
                           TokenizerModel
                           TokenSampleStream
                           TokenizerFactory)
   (opennlp.tools.util PlainTextByLineStream
                       TrainingParameters
                       MarkableFileInputStreamFactory)
   ))


(defrecord TokModel [binfile, trainfile, language, model, logger]
  Model
  (load-model [this]
    (log @logger :debug ::load-model {:file binfile})
    (reset! model (TokenizerModel. (io/as-file binfile))))
  (train-model [this]
    (log @logger :debug ::train {:file trainfile :lang language})
    (reset! model (TokenizerME/train (TokenSampleStream.
                                      (PlainTextByLineStream.
                                       (MarkableFileInputStreamFactory. (io/file trainfile)) "UTF-8"))
                                     (TokenizerFactory. language nil false nil)
                                     (doto (TrainingParameters.)
                                       (.put TrainingParameters/ITERATIONS_PARAM "100")
                                       (.put TrainingParameters/CUTOFF_PARAM     "5")))))
  (save-model [this]
    (log @logger :debug ::save-model {:file binfile})
    (.serialize ^TokenizerModel @model (io/as-file binfile)))
  (get-model [this]
    @model)
  (set-logger! [this newlogger]
    (reset! logger newlogger))
  )

(defrecord SimpleTokModel [logger]
  Model
  (load-model [this]
    (log @logger :debug ::load-model {:action :no-action}))
  (train-model [this]
    (log @logger :debug ::train {:action :no-action}))
  (save-model [this]
    (log @logger :debug ::save-model {:action :no-action}))
  (get-model [this]
    SimpleTokenizer/INSTANCE)
  (set-logger! [this newlogger]
    (reset! logger newlogger))
  )

(defrecord WhitespaceTokModel [model, logger]
  Model
  (load-model [this]
    (log @logger :debug ::load-model {:action :no-action}))
  (train-model [this]
    (log @logger :debug ::train  {:action :no-action}))
  (save-model [this]
    (log @logger :debug ::save-model {:action :no-action}))
  (get-model [this]
    WhitespaceTokenizer/INSTANCE)
  (set-logger! [this newlogger]
    (reset! logger newlogger))
  )



(derive :nlptools.model/tokenizer :nlptools/model)
(derive :nlptools.model.tokenizer/simple :nlptools.model/tokenizer)


(defmethod ig/init-key ::simple [_ spec]
  (let [{:keys [logger]} spec
        tokenizer (->SimpleTokModel (atom nil))]
    (log logger :info ::init-simple-tokenizer)
    (.set-logger! tokenizer logger)
    tokenizer))



