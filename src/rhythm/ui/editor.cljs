(ns rhythm.ui.editor 
  (:require [rhythm.code.evaluation :as eval]
            [rhythm.ui.editor-framework.interop :as e]
            [rhythm.ui.motion :as motion]
            [rhythm.ui.ui-utils :as ui-utils]
            [medley.core :as m]
            [rhythm.ui.prespace :as ps]
            [rhythm.code.node :as node]))

(defn- editor-term
  "Displays a term of a code tree."
  [term]
  [e/Editable
   {:class :code-term
    :editableId (:id term)
    :value (:text term)}])

(defn- node-prespace
  "Displays a space between editor nodes."
  [node-id]
  [e/Editable
   {:class :code-prespace
    :editableId (ps/eid->prespace-eid node-id)
    :value " "}])

(defn- editor-node
  "Displays a node of a code tree."
  [node]
  (let [id->child (:id->child node)
        space+subnode-vecs (for [[child-pos child-id] (m/indexed (:pos->id node))]
                             (let [child (id->child child-id)
                                   child-component-class (if (node/code-term? child)
                                                           editor-term
                                                           editor-node)
                                   rendered-node ^{:key child-id} [child-component-class child]]
                               (if (pos? child-pos)
                                 [^{:key (str child-id :prespace)} [node-prespace child-id]
                                  rendered-node]
                                 [rendered-node])))
        spaces+subnodes (apply concat space+subnode-vecs)]
    [:div.code-block
     [:div.code-view spaces+subnodes]
     [:div.code-eval
      {:contentEditable false}
      (str (:clj-val (eval/eval-node node)))]]))

(defn editor-pane
  "Displays an editor pane containing the visual representation of code tree."
  [node]
  [motion/div
   {:class :editor-pane
    :drag true
    :dragMomentum false}
   [:div.code-blocks
    {:onPointerDownCapture ui-utils/stop-propagation!}
    (let [id->child (:id->child node)]
      (for [child-id (:pos->id node)]
        (let [child (id->child child-id)]
          ^{:key child-id} [editor-node child])))]])