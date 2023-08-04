(ns rhythm.ui.editor-framework.interop
  [:require
   ["./components" :rename {EditorRoot jsEditorRoot
                            Editable jsEditable}]
   [clojure.edn :as edn]
   [reagent.core :as r]
   [rhythm.code.location :as loc]])

(def AdaptedJsEditorRoot (r/adapt-react-class jsEditorRoot))
(def AdaptedEditable (r/adapt-react-class jsEditable))

(defn Editable [props & children]
  ;; :editableId is serialized to EDN to allow symbols to survive conversion to and from JS 
  ;; structures. This conversion normally converts symbols to strings, which stay strings on 
  ;; conversion back. If there is a way to pass the editableId prop without it being converted 
  ;; by Reagent, using that would be an improvement on serializing and deserializing.
  (apply vector AdaptedEditable (update props :editableId pr-str) children))

(defn jsEditorPoint->code-point
  "Converts an EditorPoint to a code point."
  [js-point]
  ;; The ID is converted from EDN here because it is serialized in the Editable component.
  (loc/->code-point (edn/read-string (js->clj (.-id js-point))) (.-offset js-point)))

(defn jsEditorRange->code-range
  "Converts an EditorRange to a code range."
  [^js js-range]
  (when js-range
    (let [start-point (jsEditorPoint->code-point (.-startPoint js-range))
          end-point (jsEditorPoint->code-point (.-endPoint js-range))
          anchor-point (jsEditorPoint->code-point (.-anchorPoint js-range))
          focus-point (jsEditorPoint->code-point (.-focusPoint js-range))]
      (loc/->code-range start-point end-point anchor-point focus-point))))

(defn- code-range->js-selection-prop
  "Converts a code range to a selection prop object."
  [ctr]
  (let [{{start-id :id
          start-offset :offset} :start
         {end-id :id
          end-offset :offset} :end} ctr]
    (clj->js {:startId start-id
              :startOffset start-offset
              :endId end-id
              :endOffset end-offset})))

(defn EditorRoot [props children]
  (let [selection (:selection props)
        jsProps (assoc props
                       :children (r/as-element children)
                       :selection (code-range->js-selection-prop selection))]
   [AdaptedJsEditorRoot jsProps]))