(ns rhythm.ui.actions
  (:require [rhythm.app-state :as app-state]
            [rhythm.code.tree :as tree]
            [rhythm.ui.editor-framework.interop :as interop]
            [rhythm.ui.prespace :as ps]))

(defn- change-data->new-selection
  "Calculates the new selection after the user changes editor content."
  [suggested-selection replace-range inserted-tree]
  ;; TODO: Rewrite this to generalize to variable depth trees, possibly after pathing 
  ;; better than simple index sequences is available.
  (let [num-new-lines (count inserted-tree)
        num-new-terms (count (peek inserted-tree))]
    ;; The suggested selection is based on the assumption that text is being edited within 
    ;; a single node. If this is not true, then then calculate the correct new selection.
    (if (and (= num-new-lines 1) (= num-new-terms 1))
      suggested-selection
      (let [replace-start-path (:path (:start replace-range))
            new-selection-path (tree/step-path-end replace-start-path (dec num-new-terms))
            last-term (peek (peek inserted-tree))
            new-selection-offset (count last-term)
            new-selection-point (tree/->code-point new-selection-path new-selection-offset)]
        (tree/->code-point-range new-selection-point)))))

(defn handle-editor-content-change!
  "Handles on onChange event from an editor."
  [^js event swap-editor-state!]
  (.preventDefault event)
  (let [prespace-replace-range (interop/jsEditorRange->code-range (.-replaceRange event))
        replace-range (ps/maybe-prespace-range->range prespace-replace-range)
        prespace-suggested-selection (interop/jsEditorRange->code-range (.-afterRange event))
        suggested-selection (ps/maybe-prespace-range->range prespace-suggested-selection)
        new-text (.-data event)
        inserted-tree (tree/text->code new-text)
        new-selection (change-data->new-selection suggested-selection replace-range inserted-tree)]
    (swap-editor-state! #(-> %
                             (app-state/replace-state-editor-range replace-range inserted-tree)
                             (app-state/replace-selection new-selection)))))

(defn handle-editor-selection-change!
  "Handles an onSelect event from an editor."
  [event swap-editor-state!]
  (let [prespace-new-selection (interop/jsEditorRange->code-range (.-selectionRange event))
        new-selection (ps/maybe-prespace-range->range prespace-new-selection)]
    (swap-editor-state! app-state/replace-selection new-selection)))