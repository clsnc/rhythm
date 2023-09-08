(ns rhythm.ui.editor-framework.interop
  [:require
   ["./components" :rename {EditorRoot jsEditorRoot
                            Editable jsEditable}]
   [clojure.edn :as edn]
   [reagent.core :as r]
   [rhythm.code.location :as loc]
   [rhythm.utils :as utils]
   [rhythm.ui.prespace :as ps]])

(def AdaptedJsEditorRoot (r/adapt-react-class jsEditorRoot))
(def AdaptedEditable (r/adapt-react-class jsEditable))

(defn Editable [props & children]
  ;; :editableId is serialized to EDN to allow symbols to survive conversion to and from JS 
  ;; structures. This conversion normally converts symbols to strings, which stay strings on 
  ;; conversion back. If there is a way to pass the editableId prop without it being converted 
  ;; by Reagent, using that would be an improvement on serializing and deserializing.
  (apply vector AdaptedEditable (update props :editableId pr-str) children))

(defn jsEditorPoint->code-path
  "Converts an EditorPoint to a code point."
  [js-point]
  ;; The ID is converted from EDN here because it is serialized in the Editable component.
  (let [raw-inner-path (edn/read-string (js->clj (.-id js-point)))
        raw-offset (.-offset js-point)
        [inner-path offset] (if (ps/prespace-path? raw-inner-path)
                              [(ps/prespace-path->path raw-inner-path) (dec raw-offset)]
                              [raw-inner-path raw-offset])]
    (conj inner-path offset)))

(defn jsEditorRange->code-range
  "Converts an EditorRange to a code range."
  [^js js-range]
  (when js-range
    (let [start-point (jsEditorPoint->code-path (.-startPoint js-range))
          end-point (jsEditorPoint->code-path (.-endPoint js-range))
          anchor-point (jsEditorPoint->code-path (.-anchorPoint js-range))
          focus-point (jsEditorPoint->code-path (.-focusPoint js-range))]
      (loc/->code-range start-point end-point anchor-point focus-point))))

(defn- code-range->js-selection-prop
  "Converts a code range to a selection prop object."
  [ctr]
  (let [{start-path :start
         end-path :end} ctr
        [start-term-path start-offset] (utils/split-off-last start-path)
        [end-term-path end-offset] (utils/split-off-last end-path)]
    (clj->js {:startId (pr-str (vec start-term-path))
              :startOffset start-offset
              :endId (pr-str (vec end-term-path))
              :endOffset end-offset})))

(defn EditorRoot [props children]
  (let [selection (:selection props)
        jsProps (assoc props
                       :children (r/as-element children)
                       :selection (code-range->js-selection-prop selection))]
   [AdaptedJsEditorRoot jsProps]))