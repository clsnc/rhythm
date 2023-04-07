(ns rhythm.ui.editor 
  (:require [rhythm.syntax.blocks :as blocks]
            [rhythm.ui.actions :as actions]))

(declare editor-block-children)

(defn editor
  "Displays an editor."
  [tree swap-block!]
  [:div.editor
   {:class :editor
    :style {:display :flex
            :flex-direction :column}}
   (editor-block-children (:root tree) [:root] swap-block!)])

(defn- editor-block
  "Displays a code block in an editor. This includes the header and children of the block."
  [block path swap-block!]
  [:div {:class :block}
   [:input
    {:type :text
     :value (:header block)
     :onChange (actions/->header-change-handler path swap-block!)
     :onKeyDown (actions/->key-down-handler path swap-block!)}]
   [:div {:class :block-children}
    (editor-block-children block path swap-block!)]])

(defn- editor-block-children
  "Displays the children blocks of a block in an editor."
  [block path swap-block!]
  (for [child (blocks/child-blocks block)]
    (let [child-id (:id child)
          child-path (conj path child-id)]
      ^{:key child-id} [editor-block child child-path swap-block!])))