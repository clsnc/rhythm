(ns rhythm.ui.actions
  (:require [rhythm.ui.editor-framework.interop :as interop]
            [rhythm.app-state :as app-state]
            [rhythm.syntax.blocks :as blocks]))

(defn- change-data->new-selection [suggested-selection replace-range new-blocks]
  (let [num-new-blocks (count new-blocks)]
    (if (= num-new-blocks 1)
      suggested-selection
      ;; If multiple blocks are being inserted, the suggested selection is probably wrong.
      (let [replace-end-path (:path (:end-point replace-range))
            new-selection-path (blocks/step-path-end replace-end-path (dec num-new-blocks))
            new-selection-offset (count (:header (last new-blocks)))
            new-selection-point (blocks/->CodeTreePoint new-selection-path new-selection-offset)]
        (blocks/->code-tree-range new-selection-point new-selection-point)))))

(defn handle-editor-content-change!
  "Handles on onChange event from an editor."
  [event swap-editor-state!]
  (.preventDefault event)
  (let [replace-range (interop/jsEditorRange->code-tree-range (.-replaceRange event))
        suggested-selection (interop/jsEditorRange->code-tree-range (.-afterRange event))
        new-text (.-data event)
        new-blocks (blocks/text->blocks new-text)
        new-selection (change-data->new-selection suggested-selection replace-range new-blocks)]
    (swap-editor-state! #(-> %
                             (app-state/replace-state-editor-range replace-range new-blocks)
                             (app-state/replace-selection new-selection)))))

(defn handle-editor-selection-change!
  "Handles an onSelect event from an editor."
  [event swap-editor-state!]
  (let [new-selection (interop/jsEditorRange->code-tree-range (.-selectionRange event))]
    (swap-editor-state! app-state/replace-selection new-selection)))