(ns TestPipeline.pipeline
  (:use [lambdacd.steps.control-flow]
        [TestPipeline.steps])
  (:require
        [lambdacd.steps.manualtrigger :as manualtrigger]))


(def pipeline-def
  `(
     (either
       manualtrigger/wait-for-manual-trigger   ; Manually start the pipeline to clone the head of the repository
       wait-for-repo)                          ; OR wait for a commit on the repository, which passes down the :revision
     (with-workspace
       clone-repo
       create-a-server
       build-code-files
       start-server
       get-parameters
       start-Etcd-nodes
       fill-data
       run-test)
  ))

