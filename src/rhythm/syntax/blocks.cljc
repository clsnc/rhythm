(ns rhythm.syntax.blocks 
  (:require [medley.core :as m]
            [rhythm.utils :as utils]))

(declare ->text-code-block replace-child valid-child-pos?)

(defrecord CodeBlock [header children id])

(defn ->empty-code-block
  "Returns a new code block with an empty header and no children."
  []
  (->text-code-block ""))

(defn ->text-code-block
  "Returns a new code block with the given header and no children."
  [header]
  (->CodeBlock header [] (gensym)))

(defn block-path->key-path
  "Returns a sequence of keys for following a given block ID path through a code tree."
  [path]
  (m/interleave-all 
   (repeat (count path) :children)
   path))

(defn child-blocks
  "Returns the children of a given code block."
  [block]
  (:children block))

(defn count-children
  "Returns the number of children in a block."
  [block]
  (count (:children block)))

(defn get-child
  "Returns the child of parent at position child-pos. If no such block exists, 
   returns nil."
  [parent child-pos]
  (get-in parent [:children child-pos]))

(defn insert-child
  "Return a new parent block with the child block inserted at a 
   given position."
  [parent child pos]
  (update parent :children #(vec (m/insert-nth pos child %))))

(defn remove-child
  "Removes a child with a given position from a block if the child exists."
  [parent child-pos]
  (update parent :children #(vec (m/remove-nth child-pos %))))

(defn get-descendant
  "Returns the block at desc-path. desc-path is followed downward from 
   the ancestor block."
  [ancestor desc-path]
  (let [desc-key-path (block-path->key-path desc-path)]
    (get-in ancestor desc-key-path)))

(defn update-descendant
  "Updates the block at desc-path with f and supplied args. If the path 
   is empty, f will be applied directly to ancestor. desc-path is followed 
   downward from ancestor."
  [ancestor desc-path f & args]
  (let [desc-key-path (block-path->key-path desc-path)]
    (apply utils/update-root-or-in ancestor desc-key-path f args)))

(defn remove-descendant
  "Removes the block at desc-path. desc-path is followed downward from ancestor."
  [ancestor desc-path]
  (let [[desc-parent-path desc-pos] (utils/split-off-last desc-path)]
    (update-descendant ancestor desc-parent-path remove-child desc-pos)))

(defn insert-descendant
  "Inserts a block into the block at parent-path. desc will have position pos 
   within its new parent block. parent-path is followed downward from ancestor."
  [ancestor parent-path desc pos]
  (update-descendant ancestor parent-path insert-child desc pos))

(defn move-descendant
  "Moves the block at start-path to be a child of the block at new-parent-path 
   with position dest-pos. start-path and new-parent-path are followed downward 
   from ancestor."
  [ancestor start-path new-parent-path dest-pos]
  (let [desc (get-descendant ancestor start-path)]
    (-> ancestor
        (remove-descendant start-path)
        (insert-descendant new-parent-path desc dest-pos))))

(defn move-child-inside-preceding-sibling
  "Move a child down the tree to become a grandchild, where its parent is its former 
   preceding sibling. If the child has no preceding sibling, no changes will be made."
  [parent moving-child-pos]
  (let [stable-child-pos (dec moving-child-pos)]
    (if (valid-child-pos? parent stable-child-pos) 
      (let [stable-child (get-child parent stable-child-pos)
            new-moving-child-pos (count-children stable-child)]
        (move-descendant parent [moving-child-pos] [stable-child-pos] new-moving-child-pos))
      parent)))

(defn split-child-in-header
  "Split a child into two children at a given header position. The 
   second new child will keep the children of the original child."
  [parent child-pos header-pos]
  (let [child (get-child parent child-pos)
        [new-header0 new-header1] (utils/split-str-at-pos (:header child) header-pos)
        new-child0 (->text-code-block new-header0)
        new-child1 (assoc child :header new-header1)]
    (-> parent
        (replace-child child-pos new-child1)
        (insert-child new-child0 child-pos))))

(defn replace-child
  "Replace the child at a given position."
  [parent child-pos new-child]
  (assoc-in parent [:children child-pos] new-child))

(defn update-header
  "Replace a block's header."
  [block new-header]
  (assoc block :header new-header))

(defn valid-child-pos?
  "Checks whether a parent has a child with position child-pos."
  [parent child-pos]
  (and (<= 0 child-pos) (< child-pos (count-children parent))))