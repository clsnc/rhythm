(ns rhythm.app-state
  (:require [rhythm.syntax.ast :as ast]
            [rhythm.syntax.blocks :as blocks]))

(defrecord AppState [ast selection])

(defn ->start-state
  "Returns an AppState with a single pane with 1 empty line."
  []
  (->AppState (ast/->empty-ast) nil))

(defn replace-state-editor-range [state change-range new-blocks]
  (let [ast (:ast state)
        new-ast (ast/update-tree ast blocks/replace-range change-range new-blocks)]
    (assoc state :ast new-ast)))

(defn replace-selection
  "Replaces a state's selection."
  [state new-selection]
  (assoc state :selection new-selection))