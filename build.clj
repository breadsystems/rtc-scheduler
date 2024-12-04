(ns build
  (:require
    [clojure.tools.build.api :as b]))

(defn clean [_]
  (b/delete {:path "resources/public/admin/cljs-runtime"})
  (b/delete {:path "target"}))

(defn uberjar [_]
  (println "Cleaning target directory...")
  (clean nil)
  (println "Copying resources...")
  (b/copy-dir {:src-dirs ["resources"]
               :target-dir "target/classes"})
  (let [basis (b/create-basis {:project "deps.edn"})]
    (println "Compiling namespaces...")
    (b/compile-clj {:basis basis
                    :src-dirs ["src" "resources"]
                    :class-dir "target/classes"
                    :ns-compile '[rtc.main]})
    (println "Writing uberjar...")
    (b/uber {:class-dir "target/classes"
             :uber-file "target/rtc.jar"
             :basis basis
             :main 'rtc.main})
    (println "Generated target/rtc.jar")))
