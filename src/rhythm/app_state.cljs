(ns rhythm.app-state
  (:require [rhythm.code.node :as node]))

(defrecord AppState [code-tree selection])

(def default-code-tree [["1" "2" "3" "sum"]
                        ["1" ["3" "5" "sum"] "7" "11" "sum"]])

(defn ->start-state
  "Returns an AppState with a single pane with 1 empty line."
  []
  (->AppState default-code-tree nil))

(defn replace-state-editor-range [state change-range insert-root]
  (let [code-tree (:code-tree state)
        {start-path :start
         end-path :end} change-range
        new-code-tree (node/replace-path-range-with-wrapped code-tree start-path end-path insert-root)]
    (assoc state :code-tree new-code-tree)))

(defn replace-selection
  "Replaces a state's selection."
  [state new-selection]
  (assoc state :selection new-selection))