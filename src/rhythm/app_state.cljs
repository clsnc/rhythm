(ns rhythm.app-state
  (:require [rhythm.syntax.ast :as ast]
            [rhythm.syntax.blocks :as blocks]))

(defrecord AppState [ast selection])

(defn replace-state-editor-selection
  "Replaces the portion of the state AST described by the state selection with new-text."
  [state new-text]
  (let [{:keys [ast selection]} state
        new-ast (ast/update-tree ast blocks/replace-range selection new-text)
        {:keys [start-path start-offset]} selection
        new-caret-offset (+ start-offset (count new-text))
        new-selection (assoc selection
                             :start-offset new-caret-offset
                             :end-path start-path
                             :end-offset new-caret-offset)]
    (assoc state
           :ast new-ast
           :selection new-selection)))

(defn replace-selection
  "Replaces a state's selection."
  [state new-selection]
  (assoc state :selection new-selection))