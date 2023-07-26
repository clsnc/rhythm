(ns rhythm.app-state
  (:require [rhythm.syntax.code :as code]))

(defrecord AppState [code-root selection])

(defn ->start-state
  "Returns an AppState with a single pane with 1 empty line."
  []
  (->AppState code/empty-tree nil))

(defn replace-state-editor-range [state change-range new-blocks]
  (let [code-tree (:code-root state)
        new-code-tree (code/replace-range code-tree change-range new-blocks)]
    (assoc state :code-root new-code-tree)))

(defn replace-selection
  "Replaces a state's selection."
  [state new-selection]
  (assoc state :selection new-selection))