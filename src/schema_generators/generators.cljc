(ns schema-generators.generators
  "Experimental support for compiling schemas to test.check generators."
  (:require
   [clojure.test.check.generators :as generators]
   [schema.spec.core :as spec :include-macros true]
   schema.spec.collection
   schema.spec.leaf
   schema.spec.variant
   [schema.core :as s :include-macros true]
   #?(:clj [schema.macros :as macros]))
  #?(:cljs (:require-macros [schema.macros :as macros] schema.core)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private helpers for composite schemas

(defn g-by [f & args]
  (generators/fmap
   (partial apply f)
   (apply generators/tuple args)))

(defn g-apply-by [f args]
  (generators/fmap f (apply generators/tuple args)))

(defn- sub-generator
  [{:keys [schema]}
   {:keys [subschema-generator #?@(:clj [^java.util.Map cache] :cljs [cache])] :as params}]
  (spec/with-cache cache schema
    (fn [d] (#'generators/make-gen (fn [r s] (generators/call-gen @d r (quot s 2)))))
    (fn [] (subschema-generator schema params))))


;; Helpers for collections

(declare elements-generator)

(defn element-generator [e params]
  (if (vector? e)
    (case (first e)
      :schema.spec.collection/optional
      (let [choices (if (:include-optional? params)
                      [(elements-generator (next e) params)]
                      [(generators/return nil) (elements-generator (next e) params)])]
        (generators/one-of choices))

      :schema.spec.collection/remaining
      (do (macros/assert! (= 2 (count e)) "remaining can have only one schema.")
          (generators/vector (sub-generator (second e) params))))
    (generators/fmap vector (sub-generator e params))))

(defn elements-generator [elts params]
  (->> elts
       (map #(element-generator % params))
       (apply generators/tuple)
       (generators/fmap (partial apply concat))))


(defprotocol CompositeGenerator
  (composite-generator [s params]))

(extend-protocol CompositeGenerator
  schema.spec.variant.VariantSpec
  (composite-generator [s params]
    (generators/such-that
     (fn [x]
       (let [pre (.-pre ^schema.spec.variant.VariantSpec s)
             post (.-post ^schema.spec.variant.VariantSpec s)]
         (not
          (or (pre x)
              (and post (post x))))))
     (generators/one-of
      (for [o (macros/safe-get s :options)]
        (if-let [g (:guard o)]
          (generators/such-that g (sub-generator o params))
          (sub-generator o params))))))

  ;; TODO: this does not currently capture proper semantics of maps with
  ;; both specific keys and key schemas that can override them.
  schema.spec.collection.CollectionSpec
  (composite-generator [s params]
    (generators/such-that
     (complement (.-pre ^schema.spec.collection.CollectionSpec s))
     (generators/fmap (:constructor s) (elements-generator (:elements s) params))))

  schema.spec.leaf.LeafSpec
  (composite-generator [s params]
    (macros/assert! false "You must provide a leaf generator for %s" s)))

(def Schema
  "A Schema for Schemas"
  (s/protocol s/Schema))

(def Generator
  "A test.check generator"
  s/Any)

(def LeafGenerators
  "A mapping from schemas to generating functions that should be used."
  (s/=> (s/maybe Generator) Schema))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public


(def +primitive-generators+
  #?(:clj {Double generators/double
           ;; using unchecked-float here will unfortunately generate a lot of
           ;; infinities, since lots of doubles are out of the float range
           Float (generators/fmap unchecked-float generators/double)
           Long generators/large-integer
           Integer (generators/fmap unchecked-int generators/large-integer)
           Short (generators/fmap unchecked-short generators/large-integer)
           Character (generators/fmap unchecked-char generators/large-integer)
           Byte (generators/fmap unchecked-byte generators/large-integer)
           Boolean generators/boolean}
     :cljs {js/Number (generators/one-of [generators/large-integer generators/double])
            js/Boolean generators/boolean}))

(def +simple-leaf-generators+
  (merge
   +primitive-generators+
   {s/Str generators/string-ascii
    s/Bool generators/boolean
    s/Num (generators/one-of [generators/large-integer generators/double])
    s/Int (generators/one-of
           [generators/large-integer
            #?@(:clj [(generators/fmap unchecked-int generators/large-integer)
                      (generators/fmap bigint generators/large-integer)])])
    s/Keyword generators/keyword
    #?(:clj clojure.lang.Keyword
       :cljs cljs.core/Keyword) generators/keyword
    s/Symbol (generators/fmap (comp symbol name) generators/keyword)
    #?(:clj Object :cljs js/Object) generators/any
    s/Any generators/any
    s/Uuid generators/uuid
    s/Inst (generators/fmap (fn [ms] (#?(:clj java.util.Date. :cljs js/Date.) ms)) generators/int)}
   #?(:clj (into {}
                 (for [[f ctor c] [[doubles double-array Double]
                                   [floats float-array Float]
                                   [longs long-array Long]
                                   [ints int-array Integer]
                                   [shorts short-array Short]
                                   [chars char-array Character]
                                   [bytes byte-array Byte]
                                   [booleans boolean-array Boolean]]]
                   [f (generators/fmap ctor
                                       (generators/vector
                                        (macros/safe-get
                                         +primitive-generators+ c)))])))))


(defn eq-generators [s]
  (when (instance? schema.core.EqSchema s)
    (generators/return (.-v ^schema.core.EqSchema s))))

(defn enum-generators [s]
  (when (instance? schema.core.EnumSchema s)
    (let [vs (vec (.-vs ^schema.core.EqSchema s))]
      (generators/fmap #(nth vs %) (generators/choose 0 (dec (count vs)))))))


(defn default-leaf-generators
  [leaf-generators]
  (some-fn
   leaf-generators
   +simple-leaf-generators+
   eq-generators
   enum-generators))

(defn always [x] (generators/return x))

(def GeneratorWrappers
  "A mapping from schemas to wrappers that should be used around the default
   generators."
  (s/=> (s/maybe (s/=> Generator Generator))
        Schema))

(defn such-that
  "Helper wrapper that filters to values that match predicate."
  [f]
  (partial generators/such-that f))

(defn fmap
  "Helper wrapper that maps f over all values."
  [f]
  (partial generators/fmap f))

(defn merged
  "Helper wrapper that merges some keys into a schema"
  [m]
  (fmap #(merge % m)))

(s/defn generator :- Generator
  "Produce a test.check generator for schema.

   leaf-generators must return generators for all leaf schemas, and can also return
   generators for non-leaf schemas to override default generation logic.

   constraints is an optional mapping from schema to wrappers for the default generators,
   which can impose constraints, fix certain values, etc."
  ([schema]
     (generator schema {}))
  ([schema leaf-generators]
     (generator schema leaf-generators {}))
  ([schema :- Schema
    leaf-generators :- LeafGenerators
    wrappers :- GeneratorWrappers]
   (generator schema leaf-generators wrappers {}))
  ([schema :- Schema
    leaf-generators :- LeafGenerators
    wrappers :- GeneratorWrappers
    auxilary-opts :- {s/Keyword s/Any}]
     (let [leaf-generators (default-leaf-generators leaf-generators)
           gen (fn [s params]
                 ((or (wrappers s) identity)
                  (or (leaf-generators s)
                      (composite-generator (s/spec s) params))))]
       (generators/fmap
        (s/validator schema)
        (gen schema (merge {:subschema-generator gen
                            :cache #?(:clj (java.util.IdentityHashMap.)
                                      :cljs (atom {}))}
                           auxilary-opts))))))

(s/defn sample :- [s/Any]
  "Sample k elements from generator."
  [k & generator-args]
  (generators/sample (apply generator generator-args) k))

(s/defn generate :- s/Any
  "Sample a single element of low to moderate size."
  [& generator-args]
  (generators/generate (apply generator generator-args) 10))
