(defproject prismatic/schema-generators "0.1.4-SNAPSHOT"
  :description "Clojure(Script) library for data generation from schemas"
  :url "http://github.com/plumatic/schema-generators"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/test.check "0.9.0"]
                 [prismatic/schema "1.1.12"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.0"]
                                  [org.clojure/clojurescript
                                   #_"1.10.597" ;; PASS? WHY?
                                   "1.10.738" ;; FAIL
                                   #_"1.10.739" ;; FAIL
                                   #_"1.10.742" ;; FAIL
                                   #_"1.10.773" ;; FAIL
                                   ]]
                   :plugins [[lein-codox "0.9.4"]
                             [lein-release/lein-release "1.0.4"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}}

  :aliases {"all" ["with-profile" "dev:dev,1.9:dev,1.10"]
            "deploy" ["do" "clean," "deploy" "clojars"]
            "test" ["do"  "clean," "test," "doo" "node" "test" "once"]}

  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-doo "0.1.10"]]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :compiler {:output-to "target/js/schema_generators_dev.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}
                       {:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/js/schema_generators_test.js"
                                   :main schema-generators.runner
                                   :target :nodejs
                                   :optimizations :none}}
                       {:id "release"
                        :source-paths ["src"]
                        :compiler {:output-to "target/js/schema_generators.js"
                                   :optimizations :advanced}}]}

  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]

  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy"]}

  :codox {:source-uri "http://github.com/plumatic/schema-generators/blob/master/"}

  :signing {:gpg-key "66E0BF75"})
