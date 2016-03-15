(ns schema-generators.runner
  (:require
   [cljs.test :as test]
   [doo.runner :refer-macros [doo-tests doo-all-tests]]
            [schema-generators.generators-test]
            [schema-generators.complete-test]
            ))

(doo-tests 'schema-generators.generators-test
           'schema-generators.complete-test)
