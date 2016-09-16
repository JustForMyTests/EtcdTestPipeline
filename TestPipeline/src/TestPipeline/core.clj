(ns TestPipeline.core
  (:require
      [TestPipeline.pipeline :as pipeline]
      [ring.server.standalone :as ring-server]
      [compojure.core :refer [routes]]
      [lambdacd.ui.ui-server :as ui]
      [lambdacd.runners :as runners]
      [lambdacd.util :as util]
      [lambdacd.core :as lambdacd]
      [lambdacd-git.core :as lambdacd-git]
      [clojure.tools.logging :as log])
  (:gen-class))

(defn -main [& args]
      (let [;; the home dir is where LambdaCD saves all data.
            ;; point this to a particular directory to keep builds around after restarting
            home-dir (util/create-temp-dir)
            config {:home-dir home-dir
                    :name "Etcd Test Pipeline"}
            ;; initialize and wire everything together
            pipeline (lambdacd/assemble-pipeline pipeline/pipeline-def config)]
            ;; create a Ring handler for the UI
            ;;app (ui/ui-for pipeline)]
            (lambdacd-git/init-ssh!)
            (log/info "LambdaCD Home Directory is " home-dir)
            ;; this starts the pipeline and runs one build after the other.
            ;; there are other runners and you can define your own as well.
            (runners/start-one-run-after-another pipeline)
            ;; start the webserver to serve the UI
            (ring-server/serve (routes
                                 (ui/ui-for pipeline)
                                 (lambdacd-git/notifications-for pipeline))
                               {:open-browser? false
                                 :port 8080})))
