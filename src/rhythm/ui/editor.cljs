(ns rhythm.ui.editor 
  (:require [rhythm.ui.editor-framework.components :as e]
            [rhythm.syntax.blocks :as blocks]
            [rhythm.ui.actions :as actions]
            [medley.core :as m]))

(declare editor-block-children)

(defn editor
  "Displays an editor."
  [tree swap-tree!]
  [:div.editor
   {:class :editor
    :style {:display :flex
            :flex-direction :column}}
   [e/EditorRoot
    {:onChange #(actions/handle-editor-change! % swap-tree!)}
    (editor-block-children (:root tree) [])]])

(defn- editor-block
  "Displays a code block in an editor. This includes the header and children of the block."
  [block path]
  (let [pos-in-parent (last path)]
    [e/EditorNode {:editorId pos-in-parent}
     [:div {:class :block}
      [e/Editable
       {:class :block-header
        :value (:header block)}]
      [:div {:class :block-children}
       (editor-block-children block path)]]]))

(defn- editor-block-children
  "Displays the children blocks of a block in an editor."
  [block path]
  (for [[child-pos child] (m/indexed (blocks/child-blocks block))]
    (let [child-path (conj path child-pos)]
      ^{:key (:id child)} [editor-block child child-path])))