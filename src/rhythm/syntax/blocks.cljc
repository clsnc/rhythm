(ns rhythm.syntax.blocks 
  (:require [clojure.string :as string]
            [medley.core :as m]
            [rhythm.utils :as utils]))

(declare ->text-code-block count-children get-children replace-child)

(defrecord CodeBlock [header children id])
(defrecord CodeTreeRange [start-path start-offset end-path end-offset])

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

(defn text->blocks
  "Converts text to CodeBlocks. One CodeBlock is returned for each line of text."
  [text]
  (let [text-lines (string/split text "\n" -1)
        blocks (map ->text-code-block text-lines)]
    blocks))

(defn step-path-end
  "Adds step to the last entry in a path."
  [path step]
  (let [[path-start path-last] (utils/split-off-last path)
        new-path-last (+ path-last step)
        new-path (conj (vec path-start) new-path-last)]
    new-path))

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

(defn insert-children
  "Inserts new-children at start-pos in block."
  [block start-pos new-children]
  (replace-children block (utils/vec-concat
                           (get-children block 0 start-pos)
                           new-children
                           (get-children block start-pos))))

(defn get-child
  "Returns the child of parent at position child-pos. If no such block exists, 
   returns nil."
  [parent child-pos]
  (get-in parent [:children child-pos]))

(defn get-children
  "Returns a vector of children in a given position range inside a parent block."
  ([parent start-pos end-pos]
   (subvec (:children parent) start-pos end-pos))
  
  ([parent start-pos]
   (subvec (:children parent) start-pos)))

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

(defn remove-child
  "Removes the child at child-pos from parent."
  [parent child-pos]
  (remove-children parent child-pos (inc child-pos)))

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

(defn- update-descendant-parent
  "Updates the parent block of path by calling f with the parent, the child position, 
   and any supplied args."
  [ancestor desc-path f & args]
  (let [[desc-parent-path desc-pos] (utils/split-off-last desc-path)]
    (apply update-descendant ancestor desc-parent-path f desc-pos args)))

(defn insert-descendants
  "Inserts new-descs at insert-path in ancestor."
  [ancestor insert-path new-descs]
  (update-descendant-parent ancestor insert-path insert-children new-descs))

(defn remove-descendant
  "Removes the block at desc-path in ancestor."
  [ancestor desc-path]
  (update-descendant-parent ancestor desc-path remove-child))

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

(defn- merge-outer-nodes
  "Combines start-node with the first node of inner-nodes by prepending start-node's header 
   before start-offset to the inner node's header. Combines end-node with the last node of 
   inner-nodes by replacing the portion of end-node's header before end-offset with the inner 
   node's header. If inner-nodes has only 1 node, it will be combined with both start-node 
   and end-node."
  [start-node start-offset inner-nodes end-node end-offset]
  (let [first-inner-node (first inner-nodes)
        combined-start-header (str (subs (:header start-node) 0 start-offset)
                                   (:header first-inner-node))
        combined-first-node (replace-header first-inner-node combined-start-header)
        combined-start-inner-nodes (assoc inner-nodes 0 combined-first-node)
        last-inner-node (last combined-start-inner-nodes)
        combined-end-header (str (:header last-inner-node)
                                 (subs (:header end-node) end-offset))
        combined-last-node (replace-header end-node combined-end-header)
        combined-nodes (utils/assoc-last combined-start-inner-nodes combined-last-node)]
    combined-nodes))

(defn replace-range
  "Removes the portion of the tree between start-path and end-path and merges 
   the remainder of each block on start-path with the remainder of the block of 
   the same level originally on end-path."
  [root range new-nodes]
  (let [new-nodes (vec new-nodes)
        {:keys [start-path start-offset end-path end-offset]} range
        start-node (get-descendant root start-path)
        end-node (get-descendant root end-path)
        new-combined-nodes (merge-outer-nodes start-node start-offset new-nodes end-node end-offset)]
    (-> root
        (merge-trees start-path root end-path)
        (remove-descendant start-path)
        (insert-descendants start-path new-combined-nodes))))