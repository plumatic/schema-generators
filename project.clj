(defproject prismatic/schema-generators "0.1.4-SNAPSHOT"
  :description "Clojure(Script) library for data generation from schemas"
  :url "http://github.com/plumatic/schema-generators"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/test.check "0.9.0"]
                 [prismatic/schema "1.1.11"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/clojurescript "1.10.520"]]
                   :plugins [[lein-codox "0.9.4"]
                             [lein-release/lein-release "1.0.4"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}}

  :aliases {"all" ["with-profile" "dev:dev,1.9:dev,1.10"]
            "deploy" ["do" "clean," "deploy" "clojars"]
            "test" ["do"  "clean," "test," "doo" "node" "test" "once"]}

  :plugins [[lein-cljsbuild "1.1.7"]
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
