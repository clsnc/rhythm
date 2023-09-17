(ns rhythm.ui.actions
  (:require [clojure.edn :as edn]
            [rhythm.app-state :as app-state]
            [rhythm.code.location :as loc]
            [rhythm.code.node :as node]
            [rhythm.utils :as utils]
            [rhythm.ui.prespace :as ps]))

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

(defn- jsEditorId+offset->code-path
  "Converts an editor ID and offset to a code path."
  [js-id offset]
  ;; The ID is converted from EDN here because it is serialized in the Editable component.
  (let [raw-inner-path (edn/read-string (js->clj js-id))
        [inner-path offset] (if (ps/prespace-path? raw-inner-path)
                              [(ps/prespace-path->path raw-inner-path) (dec offset)]
                              [raw-inner-path offset])]
    (conj inner-path offset)))

(defn- jsEditorRange->code-range
  "Converts an EditorRange to a code range."
  [^js js-range]
  (when js-range
    (let [start-path (jsEditorId+offset->code-path (.-startId js-range) (.-startOffset js-range))
          end-path (jsEditorId+offset->code-path (.-endId js-range) (.-endOffset js-range))]
      (loc/->code-range start-path end-path))))

(defn handle-editor-content-change!
  "Handles on onChange event from an editor."
  [^js event swap-editor-state!]
  (.preventDefault event)
  (let [replace-range (jsEditorRange->code-range (.-replaceRange event))
        new-text (.-data event)
        inserted-node (node/text->code-node new-text)
        new-selection (content-change-data->new-selection replace-range inserted-node)]
    (swap-editor-state! #(-> %
                             (app-state/replace-state-editor-range replace-range inserted-node)
                             (app-state/replace-selection new-selection)))))

(defn handle-editor-selection-change!
  "Handles an onSelect event from an editor."
  [event swap-editor-state!]
  (let [new-selection (jsEditorRange->code-range (.-selectionRange event))]
    (swap-editor-state! app-state/replace-selection new-selection)))

(defn handle-editor-key-down!
  "Handles an onKeyDown event from an editor."
  [event selection swap-editor-state!]
  (when (= (.-key event) "Tab")
    (.preventDefault event)
    (let [{pre-wrap-start-path :start
           pre-wrap-end-path :end} selection
          [pre-wrap-first-node-path pre-wrap-start-offset] (utils/vec-split-off-last pre-wrap-start-path)
          pre-wrap-first-node-pos (peek pre-wrap-first-node-path)
          pre-wrap-end-offset (peek pre-wrap-end-path)
          pre-wrap-last-node-pos (nth pre-wrap-end-path (- (count pre-wrap-end-path) 2))
          pre-wrap-end-node-pos (inc pre-wrap-last-node-pos) ;; This is the first node after the wrapped section.
          wrap-len (- pre-wrap-end-node-pos pre-wrap-first-node-pos)
          post-wrap-last-node-pos (dec wrap-len)
          post-wrap-start-path (conj pre-wrap-first-node-path 0 pre-wrap-start-offset)
          post-wrap-end-path (conj pre-wrap-first-node-path post-wrap-last-node-pos pre-wrap-end-offset)
          post-wrap-sel (loc/->code-range post-wrap-start-path post-wrap-end-path)]
      (swap-editor-state! #(-> %
                               (app-state/wrap-state-editor-node-range pre-wrap-first-node-path pre-wrap-end-node-pos)
                               (app-state/replace-selection post-wrap-sel))))))