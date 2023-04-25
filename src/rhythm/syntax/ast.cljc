(ns rhythm.syntax.ast
  (:require [rhythm.syntax.blocks :as blocks]))

(defrecord AST [root])

(defn ->empty-ast
  "Returns an AST with containing only a root node with one empty child."
  []
  (let [empty-block (blocks/->empty-code-block)
        root-block (blocks/->CodeBlock nil [empty-block] :root)]
    (->AST root-block)))

(defn update-tree
  "Returns an AST with the given function and arguments applied to the root."
  [ast f & args]
  (update ast :root #(apply f % args)))