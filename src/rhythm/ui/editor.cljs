(ns rhythm.ui.editor 
  (:require [rhythm.code.evaluation :as eval]
            [rhythm.ui.editor-framework.interop :as e]
            [rhythm.ui.motion :as motion]
            [rhythm.ui.ui-utils :as ui-utils]
            [medley.core :as m]
            [rhythm.ui.prespace :as ps]))

(defn- editor-term
  "Displays a term of a code tree."
  [term path]
  [e/Editable
   {:class :code-term
    :editableId path
    :value term
    }])

(defn- node-prespace
  "Displays a space between editor nodes."
  [node-path]
  [e/Editable
   {:class :code-prespace
    :editableId (ps/path->prespace-path node-path)
    :value " "}])

(defn- editor-node
  "Displays a node of a code tree."
  [subtree path]
  (let [space+subnode-vecs (for [[child-pos child] (m/indexed subtree)]
                             (let [child-path (conj path child-pos)
                                   rendered-node (if (vector? child)
                                                   ^{:key (gensym)} [editor-node child child-path]
                                                   ^{:key (gensym)} [editor-term child child-path])]
                               (if (pos? child-pos)
                                 [^{:key (gensym)} [node-prespace child-path] rendered-node]
                                 [rendered-node])))
        spaces+subnodes (apply concat space+subnode-vecs)]
    [:div.code-block
     [:div.code-view spaces+subnodes]
     [:div.code-eval
      {:contentEditable false}
      (str (:clj-val (eval/eval-expr subtree)))]]))

(defn editor-pane
  "Displays an editor pane containing the visual representation of code tree."
  [code-root]
  [motion/div
   {:class :editor-pane
    :drag true
    :dragMomentum false}
   [:div.code-blocks
    {:onPointerDownCapture ui-utils/stop-propagation!}
    (for [[code-node-pos code-node] (m/indexed code-root)]
      ^{:key (gensym)} [editor-node code-node [code-node-pos]])]])