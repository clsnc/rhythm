(ns rhythm.ui.actions
  (:require [rhythm.app-state :as app-state]
            [rhythm.syntax.blocks :as blocks]))

(defn handle-editor-change!
  "Handles on onChange event from an editor."
  [event swap-editor-state!]
  (.preventDefault event)
  (let [new-text (.-data event)]
    (swap-editor-state! app-state/replace-state-editor-selection new-text)))

(defn handle-editor-selection-change!
  "Handles an onSelect event from an editor."
  [event swap-editor-state!]
  (let [start-path (js->clj (.-startEditorPath event))
        end-path (js->clj (.-endEditorPath event))
        start-offset (.-startOffset event)
        end-offset (.-endOffset event)
        new-selection (blocks/->CodeTreeRange start-path start-offset end-path end-offset)]
    (swap-editor-state! app-state/replace-selection new-selection)))