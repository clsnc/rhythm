(ns rhythm.syntax.ast
  (:require [medley.core :as m]
            [rhythm.syntax.blocks :as blocks]))

(defrecord AST [root])

(defn ->empty-ast
  "Returns an AST with containing only a root node with one empty child."
  []
  (let [empty-block (blocks/->empty-code-block)
        empty-block-id (:id empty-block)
        root-block (blocks/->CodeBlock nil {empty-block-id empty-block} [empty-block-id] :root)]
    (->AST root-block)))

(defn- block-path->ast-key-path
  "Returns a sequence of keys for following a given block ID path through an AST object."
  [path]
  (m/interleave-all
   path
   (repeat (dec (count path)) :child-id->child)))

(defn update-block
  "Returns an AST with the given operation applied to the block 
   at the given path."
  [ast path op]
  (let [ast-key-path (block-path->ast-key-path path)
        new-ast (update-in ast ast-key-path op)]
    new-ast))