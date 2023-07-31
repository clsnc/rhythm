(ns rhythm.ui.editor-framework.interop
  [:require
   ["./components" :rename {EditorRoot jsEditorRoot
                            Editable jsEditable}]
   [reagent.core :as r]
   [rhythm.code.tree :as tree]])

(def AdaptedJsEditorRoot (r/adapt-react-class jsEditorRoot))
(def Editable (r/adapt-react-class jsEditable))

(defn- fix-prespace-path
  "Reagent automatically converts props to JS objects. Keywords get converted to strings.
   This means that when a path is used as an Editable ID and then passed back to Clojurescript, 
   the converted Clojurescript object now contains a string in place of the original keyword. 
   This function corrects this in the specific case of prespace paths."
  [p]
  (let [last-pos (dec (count p))
        last-el (p last-pos)]
    (if (= last-el "prespace")
      (assoc p last-pos :prespace)
      p)))

(defn jsEditorPoint->code-point
  "Returns a tuple of [path offset] from an EditorPoint."
  [js-point]
  (tree/->code-point (fix-prespace-path (js->clj (.-id js-point))) (.-offset js-point)))

(defn jsEditorRange->code-range
  "Returns a CodeTreeRange from an EditorRange."
  [^js js-range]
  (when js-range
    (let [start-point (jsEditorPoint->code-point (.-startPoint js-range))
          end-point (jsEditorPoint->code-point (.-endPoint js-range))
          anchor-point (jsEditorPoint->code-point (.-anchorPoint js-range))
          focus-point (jsEditorPoint->code-point (.-focusPoint js-range))]
      (tree/->code-range start-point end-point anchor-point focus-point))))

(defn- code-range->js-selection-prop [ctr]
  (let [{{start-path :path
          start-offset :offset} :start
         {end-path :path
          end-offset :offset} :end} ctr]
    (clj->js {:startId start-path
              :startOffset start-offset
              :endId end-path
              :endOffset end-offset})))

(defn EditorRoot [props children]
  (let [selection (:selection props)
        jsProps (assoc props
                       :children (r/as-element children)
                       :selection (code-range->js-selection-prop selection))]
   [AdaptedJsEditorRoot jsProps]))