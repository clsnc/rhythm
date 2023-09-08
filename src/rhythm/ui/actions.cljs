(ns rhythm.ui.actions
  (:require [rhythm.app-state :as app-state]
            [rhythm.code.location :as loc]
            [rhythm.ui.editor-framework.interop :as interop]
            [rhythm.code.node :as node]
            [rhythm.utils :as utils]))

(defn- content-change-data->new-selection
  "Calculates the new selection after the user changes editor content."
  [replace-range inserted-node]
  (let [num-insert-lines (count inserted-node)
        last-insert-line-node (peek inserted-node)
        num-last-insert-line-terms (count last-insert-line-node)
        last-insert-term (peek last-insert-line-node)
        last-insert-term-len (count last-insert-term)
        
        ;; Offset line and term term counts are 1 less than insert line/count because they get 
        ;; merged with their new predecessors.
        num-offset-lines (dec num-insert-lines)
        num-last-insert-line-offset-terms (dec num-last-insert-line-terms)
        offset-path [num-offset-lines num-last-insert-line-offset-terms last-insert-term-len]
        new-sel-path (vec (loc/pad-and-add-paths (:start replace-range) offset-path))
        
        ;; The second to last step in the path, the term position, depends on whether multiple 
        ;; lines are being inserted.
        are-multiple-insert-lines (> num-insert-lines 1)
        new-sel-path (if are-multiple-insert-lines
                       (utils/assoc-from-end new-sel-path 1 num-last-insert-line-offset-terms)
                       new-sel-path)
        
        ;; The last step of the path, the offset in the term, depends on whether that term will 
        ;; be merged into a pre-existing term.
        are-multiple-insert-terms (or are-multiple-insert-lines (> num-last-insert-line-terms 1))
        new-sel-path (if are-multiple-insert-terms
                       (utils/assoc-from-end new-sel-path 0 last-insert-term-len)
                       new-sel-path)]
    (loc/->code-point-range new-sel-path)))

(defn handle-editor-content-change!
  "Handles on onChange event from an editor."
  [^js event swap-editor-state!]
  (.preventDefault event)
  (let [replace-range (interop/jsEditorRange->code-range (.-replaceRange event))
        new-text (.-data event)
        inserted-node (node/text->code-node new-text)
        new-selection (content-change-data->new-selection replace-range inserted-node)]
    (swap-editor-state! #(-> %
                             (app-state/replace-state-editor-range replace-range inserted-node)
                             (app-state/replace-selection new-selection)))))

(defn handle-editor-selection-change!
  "Handles an onSelect event from an editor."
  [event swap-editor-state!]
  (let [new-selection (interop/jsEditorRange->code-range (.-selectionRange event))]
    (swap-editor-state! app-state/replace-selection new-selection)))