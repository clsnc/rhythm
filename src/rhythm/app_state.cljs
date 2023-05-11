(ns rhythm.app-state
  (:require [rhythm.panes :as panes]
            [rhythm.syntax.ast :as ast]
            [rhythm.syntax.blocks :as blocks]))

(defrecord AppState [ast pane-id->pane selection])

(defn ->single-empty-pane-state
  "Returns an AppState with a single pane with 1 empty line."
  []
  (let [pane (panes/->new-pane [0])]
    (->AppState (ast/->empty-ast) {(:id pane) pane} nil)))

(defn replace-state-editor-range [state change-range new-blocks]
  (let [ast (:ast state)
        new-ast (ast/update-tree ast blocks/replace-range change-range new-blocks)]
    (assoc state :ast new-ast)))

(defn replace-selection
  "Replaces a state's selection."
  [state new-selection]
  (assoc state :selection new-selection))