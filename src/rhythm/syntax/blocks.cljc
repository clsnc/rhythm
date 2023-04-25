(ns rhythm.syntax.blocks 
  (:require [medley.core :as m]
            [rhythm.utils :as utils]))

(declare ->text-code-block count-children get-children replace-child)

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

(defn replace-child
  "Replace the child at a given position."
  [parent child-pos new-child]
  (assoc-in parent [:children child-pos] new-child))

(defn replace-children 
  "Replaces all of a parent block's children with a new vector of children."
  [parent new-children]
  (assoc parent :children new-children))

(defn copy-children
  "Copies children in a given position range in old-parent to the end of new-parent. 
   If positions are omitted, all children will be copied."
  ([new-parent old-parent]
   (copy-children new-parent old-parent 0))
  
  ([new-parent old-parent start-pos]
   (copy-children new-parent old-parent start-pos (count-children old-parent)))
  
  ([new-parent old-parent start-pos end-pos]
   (let [copied-children (get-children old-parent start-pos end-pos)
         combined-children (utils/vec-concat (:children new-parent) copied-children)]
     (replace-children new-parent combined-children))))

(defn count-children
  "Returns the number of children in a block."
  [block]
  (count (:children block)))

(defn get-child
  "Returns the child of parent at position child-pos. If no such block exists, 
   returns nil."
  [parent child-pos]
  (get-in parent [:children child-pos]))

(defn get-children
  "Returns a vector of children in a given position range inside a parent block."
  [parent start end]
  (subvec (:children parent) start end))

(defn replace-header
  "Replace a block's header."
  [block new-header]
  (assoc block :header new-header))

(defn remove-children
  "Removes children in a given range of positions from a parent block."
  ([parent remove-start-pos remove-end-pos]
   (update parent :children utils/vec-remove-range remove-start-pos remove-end-pos)) 
  
  ([parent remove-start-pos]
   (remove-children parent remove-start-pos (count-children parent))))

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

(defn replace-descendant
  "Replace a descendant of ancestor at path desc-path with new-desc."
  [ancestor desc-path new-desc]
  (let [[desc-parent-path desc-pos] (utils/split-off-last desc-path)]
    (update-descendant ancestor desc-parent-path replace-child desc-pos new-desc)))

(defn- merge-blocks
  "Returns a block with block0's children before b0-end-pos followed by 
   block1's children starting with b1-start-pos. The new block will have block0's 
   header."
  [block0 b0-end-pos block1 b1-start-pos]
  (-> block0
      (remove-children b0-end-pos)
      (copy-children block1 b1-start-pos)))

(defn- not-nil-child&pos
  "Returns a [child child-pos] tuple if child-pos is not nil. Returns [<empty block> 0] 
   if child-pos is nil."
  [parent child-pos]
  (if child-pos
    [(get-child parent child-pos) child-pos]
    [(->empty-code-block) 0]))

(defn- merge-trees
  "Slices off the portion of the tree under start-root beginning with start-path and 
   merges it with the portion of the tree under end-root begining with end-path. Returns 
   a single combined root."
  [start-root start-path end-root end-path]
  (let [[start-child-pos & start-path-rest] start-path
        [start-child start-child-pos] (not-nil-child&pos start-root start-child-pos)
        [end-child-pos & end-path-rest] end-path
        [end-child end-child-pos] (not-nil-child&pos end-root end-child-pos)
        merged-root (merge-blocks start-root start-child-pos end-root end-child-pos)]
    (if (and (empty? start-path) (empty? end-path))
      merged-root
      (let [merged-child (merge-trees start-child start-path-rest end-child end-path-rest)
            merged-tree (replace-child merged-root start-child-pos merged-child)]
        merged-tree))))

(defn replace-range
  "Removes the portion of the tree between start-path and end-path and merges 
   the remainder of each block on start-path with the remainder of the block of 
   the same level originally on end-path."
  [root start-path start-offset end-path end-offset new-text]
  (let [start-node (get-descendant root start-path)
        end-node (get-descendant root end-path)
        merged-header (str (subs (:header start-node) 0 start-offset)
                           new-text
                           (subs (:header end-node) end-offset))
        merged-node (replace-header end-node merged-header)]
    (-> root
        (merge-trees start-path root end-path)
        (replace-descendant start-path merged-node))))