(ns rhythm.code.tree
  (:require [rhythm.utils :as utils]
            [rhythm.code.node :as node]))

(def default-arr-tree [["1" "2" "3" "sum"]
                       ["1" "3" "5" "7" "11" "sum"]])

(defn- tree-id-path
  "Returns the path of IDs from the root of tree to the node with ID id."
  [tree id]
  (let [root-id (:id (:root tree))
        ;; Do not include the root in the ancestry.
        ;; Note that if the root ID is never found, this will loop infinitely. Check for this 
        ;; the app ever hangs permanently.
        ancestry (take 100 (take-while #(not= % root-id) (iterate (:id->parent-id tree) id)))
        path (reverse ancestry)]
    path))

(defn- node-id-path->pos-path
  "Converts a path of descendant node IDs in node to a path of node positions in their parents."
  [node [subnode-id & rest-id-path]]
  (if subnode-id
    (let [subnode (node/child-by-id node subnode-id)
          subnode-pos (node/child-pos-from-id node subnode-id)]
      (conj (node-id-path->pos-path subnode rest-id-path) subnode-pos))
    '()))

(defn- tree-id-path->pos-path
  "Converts a path of node IDs in the root of tree to a path of node positions in their parents."
  [tree id-path]
  (node-id-path->pos-path (:root tree) id-path))

(defn- tree-pos-path
  "Returns the path of positions from the root of tree to the node with ID id."
  [tree id]
  (tree-id-path->pos-path tree (tree-id-path tree id)))

(defn- include-node-descendants-in-parent-id-map
  "Returns an updated id->parent-id map with an entry for each descendant of node."
  [node id->parent-id]
  (let [node-id (:id node)
        id->child (:id->child node)]
    (loop [[subnode-id & rest-subnode-ids] (:pos->id node)
           id->parent-id id->parent-id]
      (if subnode-id
        (let [subnode (id->child subnode-id)
              id->parent-id (assoc id->parent-id subnode-id node-id)
              id->parent-id (if (node/code-term? subnode)
                              id->parent-id
                              (include-node-descendants-in-parent-id-map subnode id->parent-id))]
          (recur rest-subnode-ids id->parent-id))
        id->parent-id))))

(defn- node->parent-id-map
  "Returns a map from each descendant ID of node to the ID of its parent."
  [node]
  (include-node-descendants-in-parent-id-map node {}))

(defn- vec-node->code-node+parent-id-map
  "Returns a code node and its associated id->parent-id map constructed from a vector 
   representation of a node."
  [vec-node id->parent-id]
  (if (string? vec-node)
    [(node/->new-code-term vec-node) id->parent-id]
    (let [node-id (gensym)]
      (loop [[arr-subnode & arr-subnodes] vec-node
             id->parent-id id->parent-id
             pos->id []
             id->child {}]
        (if arr-subnode
          (let [[subnode id->parent-id] (vec-node->code-node+parent-id-map arr-subnode id->parent-id)
                subnode-id (:id subnode)
                id->parent-id (assoc id->parent-id subnode-id node-id)
                pos->id (conj pos->id subnode-id)
                id->child (assoc id->child subnode-id subnode)]
            (recur arr-subnodes id->parent-id pos->id id->child))
          [(node/->code-node pos->id id->child node-id) id->parent-id])))))

(defn vec-tree->code-tree
  "Returns a code tree from a vector representation of a code tree."
  [vec-tree]
  (let [[root id->parent-id] (vec-node->code-node+parent-id-map vec-tree {})]
    {:root root
     :id->parent-id id->parent-id}))

(defn code-tree->vec-tree
  "Converts a code tree to a vector tree."
  [code-tree]
  (node/code-node->vec-node (:root code-tree)))

(def default-code-tree (vec-tree->code-tree default-arr-tree))

(defn- concat-2-terms
  "Concatenates 2 terms a and b into a single term retaining the ID of b."
  [a b]
  (update b :text #(str (:text a) %)))

(defn- inner-node-slice
  "Helper function that slices node at pos. Returns 1 of the 2 slices as node with ID new-id. 
   The returned slice is determined by choose-kept-vec, a function that accepts and returns a 
   seqable of two items. If they are returned unchanged, this function will return the first 
   slice of the node. If they are reversed, it will return the second slice."
  [choose-kept-vec new-id node pos]
  (let [old-pos->id (:pos->id node)
        start+end-pos (utils/vec-split-at pos old-pos->id)
        [keep-pos->id, remove-pos->id] (choose-kept-vec start+end-pos)
        old-id->child (:id->child node)
        new-id->child (apply dissoc old-id->child remove-pos->id)]
    (node/->code-node keep-pos->id new-id->child new-id)))

(defn- inner-node-start-slice
  "Returns a node with a new ID containing only the children before pos."
  [node pos]
  (inner-node-slice identity (gensym) node pos))

(defn- inner-node-end-slice
  "Returns node but with all children before pos removed."
  [node pos]
  (inner-node-slice reverse (:id node) node pos))

(defn- term-start-slice
  "Returns a term with a new ID containing only the text before pos."
  [term pos]
  (node/->code-term (subs (:text term) 0 pos) (gensym)))

(defn- term-end-slice
  "Returns term but with text before pos removed."
  [term pos]
  (node/->code-term (subs (:text term) pos) (:id term)))

(defn- start-join-slice-child
  "Helper function that joins a sliced child to a sliced node when the start portion 
   of that node is being kept."
  [node child]
  (node/append-child node child))

(defn- end-join-slice-child
  "Helper function that joins a sliced child to a sliced node when the end portion of that 
   node is being kept."
  [node child]
  (node/replace-child-at-pos node 0 child))

(defn- slice-node
  "Helper function that slices a node along a given position path and offset. Whether the start 
   or end slice is returned is determined by the functions passed as inner-node-slice, term-slice, 
   and join-slice-child."
  [inner-node-slice term-slice join-slice-child node path offset]
  (let [[slice-pos & path-rest] path
        slice-child (node/nth-child node slice-pos)
        new-slice-child (if (empty? path-rest)
                          (term-slice slice-child offset)
                          (slice-node inner-node-slice term-slice join-slice-child
                                      slice-child path-rest offset))
        node-kept-slice (join-slice-child (inner-node-slice node slice-pos)
                                          new-slice-child)]
    node-kept-slice))

(defn- start-slice-node
  "Slices a node along a given positon path and offset and returns the start slice."
  [root path offset]
  (slice-node inner-node-start-slice term-start-slice start-join-slice-child
              root path offset))

(defn- end-slice-node
  "Slices a node along a given position path and offset and returns the end slice."
  [root path offset]
  (slice-node inner-node-end-slice term-end-slice end-join-slice-child
              root path offset))

(defn- concat-2-nodes
  "Returns a new node that is the concatenation of 2 inner nodes a and b with the ID of a."
  [a b]
  (-> a
      (update :pos->id utils/vec-concat (:pos->id b))
      (update :id->child into (:id->child b))))

(defn- merge-join-2-nodes
  "Joins 2 nodes together, concatenating the end nodes and subnodes of root1 with the 
   start nodes and subnodes of root2. Assumes root1's end position path and root2's start 
   position path are of equal depth."
  [root1 root2]
  (if (node/code-term? root1)
    (concat-2-terms root1 root2)
    (let [join-child-1 (node/last-child root1)
          join-child-2 (node/first-child root2)
          joined-child (merge-join-2-nodes join-child-1 join-child-2)
          updated-root-1 (node/replace-last-child root1 joined-child)
          updated-root-2 (node/remove-first-child root2)
          joined-root (concat-2-nodes updated-root-1 updated-root-2)]
      joined-root)))

(defn- merge-join-nodes
  "Joins 2 or more nodes together, concatenating the end nodes of each tree with the 
   start nodes the next one. Assumes each tree's end poisiton path has the same depth 
   as the start position path of the following tree."
  [root1 & roots]
  (loop [root1 root1
         roots roots]
    (let [[root2 & roots-rest] roots]
      (if (nil? root2)
        root1
        (let [root1+2 (merge-join-2-nodes root1 root2)]
          (recur root1+2 roots-rest))))))

(defn- replace-pos-path-range
  "Replace a range in node, described with position paths, with the contents of new-slice. 
   Assumes that the edges of new-slice match the depth of the inner edges of node it will 
   be joined with."
  [node start-path end-path start-offset end-offset new-slice]
  (let [start-slice (start-slice-node node start-path start-offset)
        end-slice (end-slice-node node end-path end-offset)
        new-root (merge-join-nodes start-slice new-slice end-slice)]
    new-root))

(defn replace-tree-id-range
  "Replace a range in node, described with ID paths, with the contents of new-slice. 
   Assumes that the edges of new-slice match the depth of the inner edges of node it will 
   be joined with."
  [tree range new-slice-root]
  (let [{{start-id :id
          start-offset :offset} :start
         {end-id :id
          end-offset :offset} :end} range
        start-path (tree-pos-path tree start-id)
        end-path (tree-pos-path tree end-id)
        new-root (replace-pos-path-range (:root tree)
                                         start-path end-path
                                         start-offset end-offset
                                         new-slice-root)
        new-id->parent-id (node->parent-id-map new-root)]
    (assoc tree
           :root new-root
           :id->parent-id new-id->parent-id)))