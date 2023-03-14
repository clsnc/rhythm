(ns rhythm.project)

(defn vector-insert [v pos e]
  (vec (concat (subvec v 0 pos) [e] (subvec v pos))))

(def default-text-tree [:root [""]])

(def empty-block [""])

(defn path->block [tree path]
  (get-in tree path))

(defn block->children [block]
  (rest block))

(defn block->text [block]
  (get block 0))

(defn offset-path [path offset]
  (let [path-start (drop-last path)
        path-final (last path)
        offset-final (+ path-final offset)
        new-path (conj path-start offset-final)]
    new-path))

(defn insert-block [tree path new-block]
  (let [parent-path (drop-last path)
        insert-pos (last path)
        parent-block (path->block tree parent-path)
        new-parent-block (vector-insert parent-block insert-pos new-block)
        new-tree (if (empty? parent-path)
                   new-parent-block
                   (assoc-in tree parent-path new-parent-block))]
    new-tree))

(defn insert-block-after [tree path new-block]
  (let [insert-path (offset-path path 1)]
    (insert-block tree insert-path new-block)))

(defn replace-block [tree path new-node]
  (assoc-in tree path new-node))

(defn replace-tree-block-text [tree path text]
  (let [block (path->block tree path)
        new-block (assoc block 0 text)
        new-tree (replace-block tree path new-block)]
    new-tree))