(ns rhythm.code.node
  (:require [rhythm.utils :as u]
            [clojure.core.rrb-vector :as v]
            [clojure.string :as string]))

(def MIN-TREE-DEPTH 3)

(def code-term? string?)

(defn- line->code-node
  "Converts a line of text to a code node."
  [line]
  (vec (string/split line " " -1)))

(defn text->code-node
  "Converts a string to a code node."
  [text]
  (let [lines (string/split text "\n" -1)
        subnodes (map line->code-node lines)]
    (vec subnodes)))

(defn- deep-concat-merge-2-nodes
  "Merges 2 nodes into 1. The last subnode of a and the first subnode of b are merged recursively."
  [a b]
  (if (code-term? a)
    (str a b)
    (let [combined-subnode (deep-concat-merge-2-nodes (peek a) (first b))
          a-start-slice (v/subvec a 0 (dec (count a)))
          b-end-slice (v/subvec b 1)]
      (v/catvec a-start-slice [combined-subnode] b-end-slice))))

(defn- deep-concat-merge-nodes
  "Merges multiple nodes into 1. The 2 subnodes at each joining edge (for nodes a and b, the last of 
   a and the first of b)are merged recursively."
  [& nodes]
  (reduce deep-concat-merge-2-nodes nodes))

(defn- side-deep-slice-node
  "Helper function that slices node and its subnodes along a given path. The slice that is kept 
   is determined by the term-slice, vec-slice, and vec-join functions."
  [node path term-slice vec-slice vec-join]
  (let [pos (first path)]
    (if (code-term? node)
      (term-slice node pos)
      (let [subnode-to-slice (node pos)
            sliced-subnode (side-deep-slice-node subnode-to-slice (rest path) term-slice vec-slice vec-join)
            kept-node-slice (vec-slice node pos)
            new-node (vec-join kept-node-slice sliced-subnode)]
        new-node))))

(defn- left-deep-slice-node
  "Slices node and its subnodes along a given path, returning the left (first) slice."
  [node path]
  (side-deep-slice-node node path
                        #(subs %1 0 %2)
                        #(v/subvec %1 0 %2)
                        conj))

(defn- right-deep-slice-node
  "Slices node and its subnodes along a given path, returning the right (second) slice."
  [node path]
  (side-deep-slice-node node path
                        subs
                        #(v/subvec %1 (inc %2))
                        u/vec-cons))

(defn wrap-node
  "Wraps node in num-layers new ancestor nodes. If num-layers is not provided, node will 
   be wrapped once."
  ([node]
   [node])

  ([node num-layers]
   (nth (iterate wrap-node node) num-layers)))

(defn replace-path-range
  "Replace a range of node and its subnodes with new-inner-node. The edges of the remaining 
   parts of node and its subnodes will be merged with the edges of new-inner-node and its subnodes."
  [node start-path end-path new-inner-node]
  (deep-concat-merge-nodes (left-deep-slice-node node start-path)
                           new-inner-node
                           (right-deep-slice-node node end-path)))

(defn replace-path-range-with-wrapped
  "Replace a range of node and its subnodes with new-inner-node. The edges of the remaining 
   parts of node and its subnodes will be merged with the edges of new-inner-node and its subnodes.
   new-inner-node is assumed to have edge depth MIN-TREE-DEPTH and will be deepened by wrapping 
   with new ancestor nodes to match the depth of start-path."
  [node start-path end-path new-inner-node]
  (let [num-wrap-layers (- (count start-path) MIN-TREE-DEPTH)
        wrapped-new-inner-node (wrap-node new-inner-node num-wrap-layers)]
    (replace-path-range node start-path end-path wrapped-new-inner-node)))