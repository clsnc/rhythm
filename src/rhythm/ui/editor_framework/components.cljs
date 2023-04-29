(ns rhythm.ui.editor-framework.components
  [:require
   ["./js_components" :rename {EditorRoot jsEditorRoot
                               Editable jsEditable}]
   [reagent.core :as r]])

(def EditorRoot (r/adapt-react-class jsEditorRoot))
(def Editable (r/adapt-react-class jsEditable))