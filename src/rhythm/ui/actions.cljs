(ns rhythm.ui.actions
  (:require [rhythm.app-state :as app-state]
            [rhythm.code.tree :as tree]
            [rhythm.ui.editor-framework.interop :as interop]
            [rhythm.ui.range-offsets :as ro]))

(defn- change-data->new-selection
  "Calculates the new selection after the user changes editor content."
  [suggested-selection replace-range new-nodes]
  (let [num-new-nodes (count new-nodes)]
    ;; The suggested selection is based on the assumption that text is being edited within 
    ;; a single node. If this is not true, then then calculate the correct new selection.
    (if (= num-new-nodes 1)
      suggested-selection
      (let [replace-end-path (:path (:end replace-range))
            new-selection-path (tree/step-path-end replace-end-path (dec num-new-nodes))
            new-selection-offset (count (peek new-nodes))
            new-selection-point (tree/->code-point new-selection-path new-selection-offset)]
        (tree/->code-point-range new-selection-point)))))

(defn handle-editor-content-change!
  "Handles on onChange event from an editor."
  [^js event swap-editor-state!]
  (.preventDefault event)
  (let [space-offset-replace-range (interop/jsEditorRange->code-range (.-replaceRange event))
        replace-range (ro/remove-range-space-offset space-offset-replace-range)
        suggested-space-offset-selection (interop/jsEditorRange->code-range (.-afterRange event))
        suggested-selection (ro/remove-range-space-offset suggested-space-offset-selection)
        new-text (.-data event)
        new-nodes (tree/text->code new-text)
        ;;new-selection (change-data->new-selection suggested-selection replace-range new-blocks)
        ;; new-selection (ro/remove-range-space-offset suggested-space-offset-selection)
        new-selection (change-data->new-selection suggested-selection replace-range new-nodes)]
    (swap-editor-state! #(-> %
                             (app-state/replace-state-editor-range replace-range new-nodes)
                             (app-state/replace-selection new-selection)))))

(defn handle-editor-selection-change!
  "Handles an onSelect event from an editor."
  [event swap-editor-state!]
  (let [new-space-offset-selection (interop/jsEditorRange->code-range (.-selectionRange event))
        new-selection (ro/remove-range-space-offset new-space-offset-selection)]
    (swap-editor-state! app-state/replace-selection new-selection)))