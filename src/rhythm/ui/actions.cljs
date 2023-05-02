(ns rhythm.ui.actions
  (:require [rhythm.ui.editor-framework.interop :as interop]
            [rhythm.app-state :as app-state]))

(defn handle-editor-content-change!
  "Handles on onChange event from an editor."
  [event swap-editor-state!]
  (.preventDefault event)
  (let [replace-range (interop/jsEditorRange->CodeTreeRange (.-replaceRange event))
        new-selection (interop/jsEditorRange->CodeTreeRange (.-afterRange event))
        new-text (.-data event)]
    (swap-editor-state! #(-> %
                             (app-state/replace-state-editor-range replace-range new-text)
                             (app-state/replace-selection new-selection)))))

(defn handle-editor-selection-change!
  "Handles an onSelect event from an editor."
  [event swap-editor-state!]
  (let [new-selection (interop/jsEditorRange->CodeTreeRange (.-selectionRange event))]
    (swap-editor-state! app-state/replace-selection new-selection)))