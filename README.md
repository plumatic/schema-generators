<img src="https://raw.github.com/wiki/plumatic/schema/images/logo.png" width="270" />

A Clojure(Script) library for data generation and generative testing with [https://github.com/plumatic/schema](Schema) and `clojure.test.check`.

[![Clojars Project](http://clojars.org/prismatic/schema-generators/latest-version.svg)](http://clojars.org/prismatic/schema-generators)

[![Circle CI](https://circleci.com/gh/plumatic/schema-generators.svg?style=svg)](https://circleci.com/gh/plumatic/schema-generators)

**This is an alpha release. The API and organizational structure are
subject to change. Comments and contributions are much appreciated.**

--

This library provides two experimental forms of automatic test data generation from schemas.

```clojure
(require '[schema-generators.complete :as c] '[schema-generators.generators :as g])

(g/sample 3 Animal)
;; => ({:name "", :barks? false, :type :dog}
;;     {:name "", :claws? false, :type :cat}
;;     {:name "\"|", :claws? false, :type :cat})

(g/generate Tree)
;; => {:value -8N, :children [{:value 5, :children [{:value -2N, :children []}]}
;;                            {:value -2, :children []}]}

(c/complete {:type :dog} Animal)
;; => {:name "nL@", :barks? false, :type :dog}
```

The `schema-generators.generators` namespace can compile Schemas into `clojure.test.check` generators.  All of the built-in
schemas are supported out of the box, and it is easy to extend to add new types or customize generation on a per-type basis.
See [`schema-generators.generators-test`](https://github.com/plumatic/schema/blob/master/test/clj/schema/experimental/generators_test.clj)
for some more complex examples.

Moreover, the `schema-generators.complete` namespace can build on generation to allow "completion" of partial data.  Whereas generators and
`clojure.test.check` are very useful tools for abstract property testing, `completers` are useful when we want to test the behavior of a
function on a *specific* complex data structure, where only some parts of the data structure are relevant for the function under test.
Completion supports all of the extensibility of generators, plus the ability to provide coercions to create very succinct helpers for
test data generation.  See [`schema-generators.complete-test`](https://github.com/plumatic/schema/blob/master/test/clj/schema/experimental/complete_test.clj)
for examples.

## Testing

* Clojure - `lein test`
* ClojureScript - [doo](https://github.com/bensu/doo) is used for running cljs tests. After [setting up your environment](https://github.com/bensu/doo#setting-up-environments),
run `lein doo {js-env} test`

## Community

Please feel free to join the Plumbing [mailing list](https://groups.google.com/forum/#!forum/prismatic-plumbing) to ask questions or discuss how you're using Schema.

We welcome contributions in the form of bug reports and pull requests; please see `CONTRIBUTING.md` in the repo root for guidelines.

## Supported Clojure versions

Schema-generators are currently supported on Clojure 1.7 and 1.8 and the latest version of ClojureScript.

## License

Distributed under the Eclipse Public License, the same as Clojure.
