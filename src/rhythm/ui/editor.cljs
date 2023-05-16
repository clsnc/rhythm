(ns rhythm.ui.editor 
  (:require [rhythm.ui.editor-framework.interop :as e]
            [rhythm.ui.motion :as motion]
            [rhythm.ui.ui-utils :as ui-utils]
            [rhythm.syntax.blocks :as blocks]
            [medley.core :as m]))

(declare editor-block-children)

(defn- editor-block
  "Displays a code block in an editor. This includes the header and children of the block."
  [block path]
  [:div {:class :block}
   [e/Editable
    {:class :block-header
     :editableId path
     :value (:header block)}]
   [:div {:class :block-children}
    (editor-block-children block path)]])

(defn- editor-block-children
  "Displays the children blocks of a block in an editor."
  [block path]
  (for [[child-pos child] (m/indexed (blocks/child-blocks block))]
    (let [child-path (conj path child-pos)]
      ^{:key (:id child)} [editor-block child child-path])))

(defn editor-pane
  "Displays an editor pane containing the text representation of code-subtree."
  [code-subtree-root code-path]
  [motion/div
   {:class :editor-pane
    :drag true
    :dragMomentum false}
   [:div {:onPointerDownCapture ui-utils/stop-propagation!}
    [editor-block code-subtree-root code-path]]])