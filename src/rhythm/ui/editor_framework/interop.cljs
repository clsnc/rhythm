(ns rhythm.ui.editor-framework.interop
  [:require
   ["./components" :rename {EditorRoot jsEditorRoot
                            Editable jsEditable}]
   [reagent.core :as r]
   [rhythm.syntax.blocks :as blocks]])

(def EditorRoot (r/adapt-react-class jsEditorRoot))
(def Editable (r/adapt-react-class jsEditable))

(defn jsEditorPoint->CodeTreePoint
  "Returns a tuple of [path offset] from an EditorPoint."
  [js-point]
  (blocks/->CodeTreePoint (.-id js-point) (.-offset js-point)))

(defn jsEditorRange->CodeTreeRange
  "Returns a CodeTreeRange from an EditorRange."
  [js-range]
  (let [start-point (jsEditorPoint->CodeTreePoint (.-startPoint js-range))
        end-point (jsEditorPoint->CodeTreePoint (.-endPoint js-range))]
    (blocks/->CodeTreeRange start-point end-point)))