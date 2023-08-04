(ns rhythm.ui.actions
  (:require [rhythm.app-state :as app-state]
            [rhythm.code.location :as loc]
            [rhythm.ui.editor-framework.interop :as interop]
            [rhythm.ui.prespace :as ps]
            [rhythm.code.node :as node]))

(defn- content-change-data->new-selection
  "Calculates the new selection after the user changes editor content."
  [replace-range inserted-node]
  (let [multiple-new-terms (or (< 1 (node/num-children inserted-node))
                               (< 1 (node/num-children (node/first-child inserted-node))))
        sel-end-id (:id (:end replace-range))
        last-new-term (node/node->last-term inserted-node)
        last-new-term-len (count (:text last-new-term))
        sel-end-offset (if multiple-new-terms
                         last-new-term-len
                         (let [replace-start-offset (:offset (:start replace-range))]
                           (+ replace-start-offset last-new-term-len)))]
    (loc/->code-point-range (loc/->code-point sel-end-id sel-end-offset))))

(defn handle-editor-content-change!
  "Handles on onChange event from an editor."
  [^js event swap-editor-state!]
  (.preventDefault event)
  (let [prespace-replace-range (interop/jsEditorRange->code-range (.-replaceRange event))
        replace-range (ps/maybe-prespace-range->range prespace-replace-range)
        new-text (.-data event)
        inserted-node (node/text->code-node new-text)
        new-selection (content-change-data->new-selection replace-range inserted-node)]
    (swap-editor-state! #(-> %
                             (app-state/replace-state-editor-range replace-range inserted-node)
                             (app-state/replace-selection new-selection)))))

(defn handle-editor-selection-change!
  "Handles an onSelect event from an editor."
  [event swap-editor-state!]
  (let [prespace-new-selection (interop/jsEditorRange->code-range (.-selectionRange event))
        new-selection (ps/maybe-prespace-range->range prespace-new-selection)]
    (swap-editor-state! app-state/replace-selection new-selection)))