(ns schema-generators.complete
  "Experimental support for 'completing' partial datums to match a schema."
  (:require
   [clojure.test.check.generators :as check-generators]
   [schema.spec.core :as spec]
   schema.spec.collection
   schema.spec.leaf
   schema.spec.variant
   [schema.coerce :as coerce]
   [schema.core :as s :include-macros true]
   [schema.utils :as utils]
   [schema-generators.generators :as generators]
   #?(:clj [schema.macros :as macros]))
  #?(:cljs (:require-macros [schema.macros :as macros])))

(def +missing+ ::missing)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private helpers

(defprotocol Completer
  (completer* [spec s sub-checker generator-opts completer-opts]
    "A function applied to a datum as part of coercion to complete missing fields.

    completer-opts:
      - `include-optional?` when set, entries with optional fields will be generated"))

(defn sample [g]
  (check-generators/generate g 10))

(extend-protocol Completer
  schema.spec.variant.VariantSpec
  (completer* [spec s sub-checker generator-opts completer-opts]
    (let [g (apply generators/generator s generator-opts)]
      (if (when-let [cs (utils/class-schema s)]
            (instance? schema.core.Record cs))
        (fn record-completer [x]
          (sub-checker (into (sample g) x)))
        (fn variant-completer [x]
          (if (= +missing+ x)
            (sample g)
            (sub-checker x))))))

  schema.spec.collection.CollectionSpec
  (completer* [spec s sub-checker generator-opts completer-opts]
    (if (instance? #?(:clj clojure.lang.APersistentMap :cljs cljs.core/PersistentArrayMap)
                   s) ;; todo: pluggable
      (let [g (apply generators/generator s generator-opts)
            required-filter (if (:include-optional? completer-opts)
                              identity
                              #(filter s/required-key? %))]
        (fn map-completer [x]
          (if (= +missing+ x)
            (sample g)
            (let [ks (distinct (concat (keys x)
                                       (->> s
                                            keys
                                            required-filter
                                            (map s/explicit-schema-key))))]
              (sub-checker
               (into {} (for [k ks] [k (get x k +missing+)])))))))
      (let [g (apply generators/generator s generator-opts)]
        (fn coll-completer [x]
          (if (= +missing+ x)
            (sample g)
            (sub-checker x))))))

  schema.spec.leaf.LeafSpec
  (completer* [spec s sub-checker generator-opts completer-opts]
    (let [g (apply generators/generator s generator-opts)]
      (fn leaf-completer [x]
        (if (= +missing+ x)
          (sample g)
          x)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(s/defn completer
  "Produce a function that simultaneously coerces, completes, and validates a datum."
  ([schema] (completer schema {}))
  ([schema coercion-matcher] (completer schema coercion-matcher {}))
  ([schema coercion-matcher leaf-generators]
   (completer schema coercion-matcher leaf-generators {}))
  ([schema
    coercion-matcher :- coerce/CoercionMatcher
    leaf-generators :- generators/LeafGenerators
    wrappers :- generators/GeneratorWrappers]
   (completer schema coercion-matcher leaf-generators wrappers {}))
  ([schema
    coercion-matcher :- coerce/CoercionMatcher
    leaf-generators :- generators/LeafGenerators
    wrappers :- generators/GeneratorWrappers
    complete-opts :- {s/Keyword s/Any}]
   (spec/run-checker
    (fn [s params]
      (let [c (spec/checker (s/spec s) params)
            coercer (or (coercion-matcher s) identity)
            completr (completer* (s/spec s) s c [leaf-generators wrappers] complete-opts)]
        (fn [x]
          (macros/try-catchall
           (let [v (coercer x)]
             (if (utils/error? v)
               v
               (completr v)))
           (catch t (macros/validation-error s x t))))))
    true
    schema)))

(defn complete
  "Fill in partial-datum to make it validate schema."
  [partial-datum & completer-args]
  ((apply completer completer-args) partial-datum))
