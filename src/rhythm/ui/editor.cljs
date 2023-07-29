(ns rhythm.ui.editor 
  (:require [rhythm.code.evaluation :as eval]
            [rhythm.ui.editor-framework.interop :as e]
            [rhythm.ui.motion :as motion]
            [rhythm.ui.ui-utils :as ui-utils]
            [medley.core :as m]))

(defn- editor-term
  "Displays a term of a code tree."
  [term path]
  [e/Editable
   {:class :code-term
    :editableId path
    :value (str " " term)}])

(defn- editor-node
  "Displays a node of a code tree."
  [subtree path]
  (let [subnodes (for [[child-pos child] (m/indexed subtree)]
                   (let [child-path (conj path child-pos)]
                     (if (vector? child)
                       ^{:key (gensym)} [editor-node child child-path]
                       ^{:key (gensym)} [editor-term child child-path])))]
    [:div.code-block
     [:div.code-view subnodes]
     [:div.code-eval
      {:contentEditable false}
      (str (:clj-val (eval/eval-expr subtree)))]]))

(defn editor-pane
  "Displays an editor pane containing the visual representation of code tree."
  [code-subtree-root code-path]
  [motion/div
   {:class :editor-pane
    :drag true
    :dragMomentum false}
   [:div {:onPointerDownCapture ui-utils/stop-propagation!}
    [editor-node code-subtree-root code-path]]])