(ns rhythm.ui.editor 
  (:require [rhythm.code.evaluation :as eval]
            [rhythm.ui.actions :as actions]
            [rhythm.ui.editor-framework.interop :as e]
            [rhythm.ui.motion :as motion]
            [rhythm.ui.ui-utils :as ui-utils]
            [medley.core :as m]
            [rhythm.ui.prespace :as ps]
            [rhythm.code.node :as node]))

(defn- editor-term
  "Displays a term of a code tree."
  [term term-path]
  [e/Editable
   {:class :code-term
    :editableId term-path
    :value term}])

(defn- node-prespace
  "Displays a space between editor nodes."
  [node-path]
  [e/Editable
   {:class :code-prespace
    :editableId (ps/path->prespace-path node-path)
    :value " "}])

(defn- editor-node
  "Displays a node of a code tree."
  [node node-path]
  (let [space+subnode-vecs (for [[child-pos child] (m/indexed node)]
                             (let [is-term (node/code-term? child)
                                   child-component-class (if is-term
                                                           editor-term
                                                           editor-node)
                                   child-path (conj node-path child-pos)
                                   rendered-node ^{:key (gensym)} [child-component-class child child-path]
                                   should-use-prespace (and is-term
                                                            ;; is the previous child a term?
                                                            (let [previous-child-pos (dec child-pos)]
                                                              (when (>= previous-child-pos 0)
                                                                (node/code-term? (node previous-child-pos)))))]
                               (if should-use-prespace
                                 [^{:key (gensym)} [node-prespace child-path]
                                  rendered-node]
                                 [rendered-node])))
        spaces+subnodes (apply concat space+subnode-vecs)]
    [:div.code-block
     [:div.code-view spaces+subnodes]
     [:div.code-eval
      {:contentEditable false}
      (str (:clj-val (eval/eval-node node)))]]))

(defn- editor-pane
  "Displays an editor pane containing the visual representation of code tree."
  [node]
  [motion/div
   {:class :editor-pane
    :drag true
    :dragMomentum false}
   [:div.code-blocks
    {:onPointerDownCapture ui-utils/stop-propagation!}
    (for [[child-pos child] (m/indexed node)]
      ^{:key (gensym)} [editor-node child [child-pos]])]])

(defn editor-root
  [code-tree selection swap-state!]
  [e/EditorRoot
   ;; Only the contents of the EditorRoot should be shown, not the div itself.
   {:style {:position :absolute
            :max-height 0
            :max-width 0
            :outline :none}
    :onChange #(actions/handle-editor-content-change! % swap-state!)
    :onKeyDown #(actions/handle-editor-key-down! % selection swap-state!)
    :onSelect #(actions/handle-editor-selection-change! % swap-state!)
    :selection selection}
   ^{:key (gensym)} [editor-pane code-tree]])