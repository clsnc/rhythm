(ns rhythm.ui.editor-framework.interop
  [:require
   ["./js_components" :rename {EditorRoot jsEditorRoot
                               Editable jsEditable}]
   [reagent.core :as r]
   [rhythm.syntax.blocks :as blocks]])

(def EditorRoot (r/adapt-react-class jsEditorRoot))
(def Editable (r/adapt-react-class jsEditable))

(defn jsEditorPoint->editableId+offset
  "Returns a tuple of [path offset] from an EditorPoint."
  [js-point]
  [(.-id js-point) (.-offset js-point)])

(defn jsEditorRange->CodeTreeRange
  "Returns a CodeTreeRange from an EditorRange."
  [js-range]
  (let [[start-path start-offset] (jsEditorPoint->editableId+offset (.-startPoint js-range))
        [end-path end-offset] (jsEditorPoint->editableId+offset (.-endPoint js-range))]
    (blocks/->CodeTreeRange start-path start-offset end-path end-offset)))