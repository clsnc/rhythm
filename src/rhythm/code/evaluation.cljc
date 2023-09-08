(ns rhythm.code.evaluation 
  (:require [rhythm.utils :as utils]
            [rhythm.code.node :as node]))

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

(defn eval-node
  "Evaluates a code node to a code object."
  [node]
  (if (node/code-term? node)
    (term->obj node)
    (let [subresults (map eval-node node)
          [args op] (utils/split-off-last subresults)
          f (:clj-val op)
          result (when (ifn? f) (f args))]
      result)))