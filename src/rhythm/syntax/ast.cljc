(ns rhythm.syntax.ast
  (:require [rhythm.syntax.blocks :as blocks]))

(defrecord AST [root])

(defn ->empty-ast
  "Returns an AST with containing only a root node with one empty child."
  []
  (let [empty-block (blocks/->empty-code-block)
        root-block (blocks/->CodeBlock nil [empty-block] :root)]
    (->AST root-block)))

(defn update-block
  "Returns an AST with the given operation applied to the block 
   at the given path."
  [ast path op]
  (update ast :root blocks/update-descendant path op))