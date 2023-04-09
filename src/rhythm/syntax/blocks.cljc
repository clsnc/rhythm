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

(defn block-path->key-path
  "Returns a sequence of keys for following a given block ID path through a code tree."
  [path]
  (m/interleave-all 
   (repeat (count path) :child-id->child)
   path))

(defn child-blocks
  "Returns the children of a given code block."
  [block]
  (map (:child-id->child block) (:pos->child-id block)))

(defn child-by-id
  "Returns the child of a block with the given ID. If no such child is present, returns
   nil."
  [parent child-id]
  ((:child-id->child parent) child-id))

(defn child-id-by-pos
  "Returns the ID of the child at a given position. If no such child is present, returns 
   nil."
  [parent child-pos]
  ((:pos->child-id parent) child-pos))

(defn count-children
  "Returns the number of children in a block."
  [block]
  (count (:pos->child-id block)))

(defn find-child-pos-by-id
  "Returns the position of the child with a given ID inside its parent block."
  [parent child-id]
  (utils/find-first-pos child-id (:pos->child-id parent)))

(defn find-child-pos
  "Returns the position of a child block inside of its parent block."
  [parent child]
  (find-child-pos-by-id parent (:id child)))

(defn get-sibling-pos-by-child-id
  "Returns the position of a sibling the given offset from a child."
  [parent child-id offset]
  (let [child-pos (find-child-pos-by-id parent child-id)
        sibling-pos (+ child-pos offset)]
    (if (and (>= sibling-pos 0) (<= sibling-pos (count-children parent)))
      sibling-pos
      nil)))

(defn get-sibling-id-by-child-id
  "Returns the ID of a sibling the given offset from a child. Returns nil if there 
   is no valid sibling at the given offset."
  [parent child-id offset]
  (when-let [sibling-pos (get-sibling-pos-by-child-id parent child-id offset)]
    (child-id-by-pos parent sibling-pos)))

(defn insert-child
  "Return a new parent block with the child block inserted at a 
   given position."
  [parent child pos]
  (let [partial-new-parent (set-child parent child)
        new-pos->child-id (vec (m/insert-nth pos (:id child) (:pos->child-id parent)))
        new-parent (assoc partial-new-parent :pos->child-id new-pos->child-id)]
    new-parent))

(defn remove-child-by-id
  "Removes a child with a given ID from a block if the child exists."
  [parent child-id]
  (let [child-pos (find-child-pos-by-id parent child-id)
        new-pos->child-id (vec (m/remove-nth child-pos (:pos->child-id parent)))
        new-child-id->child (dissoc (:child-id->child parent) child-id)
        new-parent (assoc parent
                          :pos->child-id new-pos->child-id
                          :child-id->child new-child-id->child)]
    new-parent))

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
  (let [[desc-parent-path desc-id] (utils/split-off-last desc-path)]
    (update-descendant ancestor desc-parent-path remove-child-by-id desc-id)))

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
  [parent moving-child-id]
  (let [stable-child-id (get-sibling-id-by-child-id parent moving-child-id -1)]
    (if (nil? stable-child-id)
      parent
      (let [stable-child (child-by-id parent stable-child-id)
            new-moving-child-pos (count-children stable-child)]
        (move-descendant parent [moving-child-id] [stable-child-id] new-moving-child-pos)))))

(defn split-child-in-header
  "Split a child into two children at a given header position. The 
   second new child will keep the children of the original child."
  [parent child-id header-pos]
  (let [child (child-by-id parent child-id)
        [new-header0 new-header1] (utils/split-str-at-pos (:header child) header-pos)
        new-child0 (->text-code-block new-header0)
        new-child1 (assoc child :header new-header1)]
    (-> parent
        (insert-child new-child0 (find-child-pos parent child))
        (set-child new-child1))))

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