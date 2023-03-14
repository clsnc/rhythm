(ns rhythm.editor 
  (:require [rhythm.project :as proj]))

(defn editor-block [tree path replace-tree!]
  (let [block (proj/path->block tree path)
        text (proj/block->text block)]
    [:input
     {:type :text
      :value text
      :onChange (fn [event]
                  (.preventDefault event)
                  (let [new-text (-> event (.-target) (.-value))
                        new-tree (proj/replace-tree-block-text tree path new-text)]
                    (replace-tree! new-tree)))
      :onKeyPress (fn [event]
                    (let [key-str (.-key event)]
                      (if (= key-str "Enter")
                        (do (.preventDefault event)
                            (replace-tree! (proj/insert-block-after tree path proj/empty-block)))
                        nil)))}]))

(defn editor-block-children [tree path replace-tree!]
  (let [block (proj/path->block tree path)
        children (proj/block->children block)]
    (for [child-pos (range 1 (inc (count children)))]
      (let [child-path (conj path child-pos)]
        ^{:key child-pos} [editor-block tree child-path replace-tree!]))))

(defn editor [tree replace-tree!]
  [:div.editor
   {:style {:display :flex
            :flex-direction :column}}
   (editor-block-children tree [] replace-tree!)])