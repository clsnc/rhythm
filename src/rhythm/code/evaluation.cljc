(ns rhythm.code.evaluation 
  (:require [rhythm.utils :as utils]))

(defn- _sum [args]
  {:clj-val (apply + (map :clj-val args))})

(def default-context
  {"sum" {:type :func
          :clj-val _sum}})

(defn term->obj
  "Returns a code object derived from a term."
  [term]
  (let [num (parse-long term)]
    (if num
      {:type :int
       :clj-val num}
      (default-context term))))

(defn eval-expr
  "Reduces a code expression to a code object."
  [expr]
  (if (vector? expr)
    (let [subresults (map eval-expr expr)
          [args op] (utils/split-off-last subresults)
          f (:clj-val op)
          result (when (ifn? f) (f args))]
      result)
    (term->obj expr)))