(ns rhythm.syntax.blocks 
  (:require [medley.core :as m]
            [rhythm.utils :as utils]))

(declare ->text-code-block set-child)

(defrecord CodeBlock [header child-id->child pos->child-id id])

(defn ->empty-code-block
  "Returns a new code block with an empty header and no children."
  []
  (->text-code-block ""))

(defn ->text-code-block
  "Returns a new code block with the given header and no children."
  [header]
  (->CodeBlock header {} [] (gensym)))

(defn child-blocks
  "Returns the children of a given code block."
  [block]
  (map (:child-id->child block) (:pos->child-id block)))

(defn child-by-id
  "Returns the child of a block with the given ID. If no such child is present, returns
   nil."
  [parent child-id]
  ((:child-id->child parent) child-id))

(defn child-pos
  "Returns the position of a child block inside of its parent block."
  [parent child]
  (utils/find-first-pos (:id child) (:pos->child-id parent)))

(defn insert-child
  "Return a new parent block with the child block inserted at a 
   given position."
  [parent child pos]
  (let [partial-new-parent (set-child parent child)
        new-pos->child-id (vec (m/insert-nth pos (:id child) (:pos->child-id parent)))
        new-parent (assoc partial-new-parent :pos->child-id new-pos->child-id)]
    new-parent))

(defn split-child-in-header
  "Split a child into two children at a given header position. The 
   second new child will keep the children of the original child."
  [parent child-id header-pos]
  (let [child (child-by-id parent child-id)
        header (:header child)
        new-header0 (subs header 0 header-pos)
        new-header1 (subs header header-pos)
        new-child0 (->text-code-block new-header0)
        new-child1 (assoc child :header new-header1)
        partial-new-parent (insert-child parent new-child0 (child-pos parent child))
        new-parent (set-child partial-new-parent new-child1)]
    new-parent))

(defn set-child
  "Add a new child to a parent block. The new child will not be given 
   a position within the parent unless a child with the same ID 
   already has a position, in which case the old child will be 
   replaced."
  [parent new-child]
  (assoc-in parent [:child-id->child (:id new-child)] new-child))

(defn update-header
  "Replace a block's header."
  [block new-header]
  (assoc block :header new-header))