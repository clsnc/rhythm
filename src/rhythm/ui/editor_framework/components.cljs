(ns rhythm.ui.editor-framework.components
  [:require
   ["./js_components" :rename {EditorRoot jsEditorRoot
                               EditorNode jsEditorNode
                               Editable jsEditable}]
   [reagent.core :as r]])

(def EditorRoot (r/adapt-react-class jsEditorRoot))
(def EditorNode (r/adapt-react-class jsEditorNode))
(def Editable (r/adapt-react-class jsEditable))