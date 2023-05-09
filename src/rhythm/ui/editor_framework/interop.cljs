(ns rhythm.ui.editor-framework.interop
  [:require
   ["./components" :rename {EditorRoot jsEditorRoot
                            Editable jsEditable}]
   [reagent.core :as r]
   [rhythm.syntax.blocks :as blocks]])

(def AdaptedJsEditorRoot (r/adapt-react-class jsEditorRoot))
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

(defn- CodeTreeRange->js-selection-prop [ctr]
  (let [{{start-path :path
          start-offset :offset} :start-point
         {end-path :path
          end-offset :offset} :end-point} ctr]
    (clj->js {:startId start-path
              :startOffset start-offset
              :endId end-path
              :endOffset end-offset})))

(defn EditorRoot [props children]
  (let [selection (:selection props)
        jsProps (assoc props
                       :children (r/as-element children)
                       :selection (CodeTreeRange->js-selection-prop selection))]
   [AdaptedJsEditorRoot jsProps]))