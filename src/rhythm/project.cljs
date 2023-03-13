(ns rhythm.project)

(def default-text-tree [:root [""]])

(defn path->block [tree path]
  (get-in tree path))

(defn block->children [block]
  (rest block))

(defn block->text [block]
  (get block 0))

(defn replace-block [tree path new-node]
  (assoc-in tree path new-node))

(defn replace-tree-block-text [tree path text]
  (let [block (path->block tree path)
        new-block (assoc block 0 text)
        new-tree (replace-block tree path new-block)]
    new-tree))