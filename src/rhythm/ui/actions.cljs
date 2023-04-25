(ns rhythm.ui.actions
  (:require [rhythm.syntax.blocks :as blocks]))

(defn handle-editor-change! [event swap-tree!]
  (.preventDefault event)
  (let [start-path (js->clj (.-startEditorPath event))
        end-path (js->clj (.-endEditorPath event))
        start-offset (.-startOffset event)
        end-offset (.-endOffset event)
        new-text (.-data event)]
    (swap-tree! blocks/replace-range
                start-path start-offset
                end-path end-offset
                new-text)))