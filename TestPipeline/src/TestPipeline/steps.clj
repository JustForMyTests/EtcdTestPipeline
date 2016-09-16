(ns TestPipeline.steps
  (:require [lambdacd.steps.shell :as shell]
            [lambdacd.steps.manualtrigger :as manualtrigger]
            [lambdacd-git.core :as lambdacd-git]))

(defn ls [args ctx]
  (shell/bash ctx (:cwd args) "ls"))

;---------------------------- Git steps ----------------------------------

(def repo-uri "https://github.com/JustForMyTests/EtcdTestPipeline.git")
(def repo-branch "master")


(defn wait-for-repo [args ctx]
  (lambdacd-git/wait-for-git ctx repo-uri
                             :ref (str "refs/heads/" repo-branch)
                             :ms-between-polls (* 60 1000)))       ; Polls repository for new commits every 1000min


(defn clone-repo [args ctx]
  (let [revision (:revision args)
        cwd (:cwd args)
        ref (or revision repo-branch)]
    (lambdacd-git/clone ctx repo-uri ref cwd)))

;---------------------- Compiling and Building ----------------------------

(defn copy-files [args ctx]
  (shell/bash ctx (:cwd args)
    "echo \"Pulling from repository\""
    ;simulate a pull by copying files in "/home/brendanyhy/workspace" into the temporary folder
    (str "cp -R /home/brendanyhy/workspace/. " (:cwd args))))


(defn build-code-files [args ctx]
  ;Build EtcdTestv2 WAR file

  ;This step compiles FillData.java and IOTest.java
  (shell/bash ctx (:cwd args)
    "mkdir -p ./IOTest/temp"
    "javac -d \"./IOTest/temp/\" -cp \"./IOTest/lib/*\" ./IOTest/src/EtcdTest/*"))

;------------------------ Run Tests -----------------------------

(defn get-parameters [args ctx]
  (manualtrigger/parameterized-trigger {:nodes {:desc "Number of nodes:"}
                                        :num {:desc "Number of entries:"}} ctx))


(defn start-Etcd-nodes [args ctx]
  (let [starting-script
        (case (:nodes args)
           "1" "rebootEtcd.sh"
           "3" "run3Etcd.sh"
           "5" "run5Etcd.sh"
           "7" "run7Etcd.sh"
           "all")
        address
        (case (:nodes args)
          "1" "localhost:4001"
          "localhost:4101")]
    (if (== (compare starting-script "all") 0)
      (println starting-script)
      (def temp (shell/bash ctx (:cwd args)
        (str "chmod +x \"" "scripts/" starting-script "\"")
        (str "./scripts/" starting-script))))
    (assoc temp
      :addr address
      :num (:num args)
      :nodes (:nodes args))
    ))


(defn fill-data [args ctx]
  (def temp (shell/bash ctx (:cwd args)
    (str "java -cp \"./IOTest/temp:./IOTest/lib/*\" EtcdTest.FillData " (:addr args) " " (:num args))))
  (assoc temp
    :nodes (:nodes args)))


(defn run-test [{cwd :cwd nodes :nodes} ctx]
  (let [locations
        (case nodes
          "1" "localhost:4001"
          "3" "localhost:4101 localhost:4201 localhost:4301"
          "5" "localhost:4101 localhost:4201 localhost:4301 localhost:4401 localhost:4501"
          "7" "localhost:4101 localhost:4201 localhost:4301 localhost:4401 localhost:4501 localhost:4601 localhost:4701"
          "default")]
    (shell/bash ctx cwd
      (str "java -cp \"./IOTest/temp:./IOTest/lib/*\" EtcdTest.IOTest " locations))
  ))


(defn some-step-that-echoes-foo [args ctx]
  (shell/bash ctx "/"
                  "chmod +x \"home/brendanyhy/myEtcdTest/scripts/test.sh\""
                  "./home/brendanyhy/myEtcdTest/scripts/test.sh")
  {:status :success :out "TESTING..."})

(defn some-step-that-echos-bar [{cwd :cwd} ctx]
  (shell/bash ctx cwd "ls"))

(defn some-failing-step [args ctx]
  (shell/bash ctx "/" "echo \"i am going to fail now...\"" "exit 1"))

