(ns rhythm.syntax.code 
  (:require [rhythm.utils :as utils]
            [clojure.string :as string]))

(def empty-tree [["THIS" "IS" "A" "TEST"]])

(defn ->code-point [path offset]
  {:path path
   :offset offset})

(defn ->code-range
  ([start end]
  {:start start
   :end end})
  
  ([start end anchor focus]
   (assoc (->code-range start end)
          :anchor anchor
          :focus focus)))

(defn ->code-point-range
  "Returns a code range of length 0."
  [point]
  (->code-range point point))

(defn text->code [text]
  (let [terms (string/split text " " -1)]
    (vec terms)))

(defn step-path-end
  "Adds step to the last entry in a path."
  [path step]
  (let [[path-start path-last] (utils/split-off-last path)
        new-path-last (+ path-last step)
        new-path (conj (vec path-start) new-path-last)]
    new-path))

(defn- merge-inner-tree
  "Slices the start-root tree at start-path, the end-root tree at end-path, then merges 
   the resulting trees into a single tree."
  [start-root end-root start-path end-path]
  (let [[start-slice-pos & start-path-rest] start-path
        [end-slice-pos & end-path-rest] end-path
        start-slice (subvec start-root 0 start-slice-pos)
        end-slice (subvec end-root end-slice-pos)
        merged-root (vec (concat start-slice end-slice))]
    (if (or (empty? start-path-rest) (empty? end-path-rest))
      merged-root
      (let [slice-start-child (get start-root start-slice-pos)
            end-slice-child (or (first end-slice) [])
            combined-subtree (merge-inner-tree slice-start-child end-slice-child
                                               start-path-rest end-path-rest)]
        (assoc merged-root start-slice-pos combined-subtree)))))

(defn- merge-outer-nodes
  "Merges start-leaf into the first node of new-nodes and end-leaf into the last."
  [start-leaf end-leaf start-offset end-offset inner-nodes]
  (let [inner-start-leaf (get inner-nodes 0)
        combined-start-leaf (str (subs start-leaf 0 start-offset)
                                 inner-start-leaf)
        new-nodes-with-combined-start (assoc inner-nodes 0 combined-start-leaf)
        inner-end-leaf-pos (dec (count new-nodes-with-combined-start))
        inner-end-leaf (get new-nodes-with-combined-start inner-end-leaf-pos)
        combined-end-leaf (str inner-end-leaf
                               (subs end-leaf end-offset))
        combined-nodes (assoc new-nodes-with-combined-start inner-end-leaf-pos combined-end-leaf)]
    combined-nodes))

(defn replace-range
  "Removes the portion of the tree between start-path and end-path and merges 
   the remainder of each node on start-path with the remainder of the node of 
   the same level originally on end-path."
  [root range new-nodes]
  (let [new-nodes (vec new-nodes)
        {{start-path :path
          start-offset :offset} :start
         {end-path :path
          end-offset :offset} :end} range
        start-leaf (get-in root start-path)
        end-leaf (get-in root end-path)
        new-combined-nodes (merge-outer-nodes start-leaf end-leaf start-offset end-offset new-nodes)]
    (-> root
        (merge-inner-tree root start-path end-path)
        (utils/vec-remove-nth-in start-path)
        (utils/vec-insert-multiple-in start-path new-combined-nodes))))