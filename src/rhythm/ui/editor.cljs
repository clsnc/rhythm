(ns rhythm.ui.editor 
  (:require [rhythm.ui.motion :as motion]
            [rhythm.ui.editor-framework.interop :as e]
            [rhythm.syntax.blocks :as blocks]
            [rhythm.ui.actions :as actions]
            [rhythm.ui.ui-utils :as ui-utils]
            [medley.core :as m]))

(declare editor-block-children)

(defn editor
  "Displays an editor."
  [tree selection swap-editor-state!]
  [motion/div
   {:class :editor-pane
    :drag true
    :dragMomentum false}
   [e/EditorRoot
    {:class :editor
     :style {:display :flex
             :flex-direction :column}
     :onChange #(actions/handle-editor-content-change! % swap-editor-state!)
     :onSelect #(actions/handle-editor-selection-change! % swap-editor-state!)
     :onPointerDownCapture ui-utils/stop-propagation!
     :selection selection}
    (editor-block-children (:root tree) [])]])

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