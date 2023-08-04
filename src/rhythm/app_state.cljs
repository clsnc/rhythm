(ns rhythm.app-state
  (:require [rhythm.code.tree :as tree]))

(defrecord AppState [code-tree selection])

(defn ->start-state
  "Returns an AppState with a single pane with 1 empty line."
  []
  (->AppState tree/empty-code-tree nil))

(defn replace-state-editor-range [state change-range insert-root]
  (let [code-tree (:code-tree state)
        new-code-tree (tree/replace-tree-id-range code-tree change-range insert-root)]
    (assoc state :code-tree new-code-tree)))

(defn replace-selection
  "Replaces a state's selection."
  [state new-selection]
  (assoc state :selection new-selection))