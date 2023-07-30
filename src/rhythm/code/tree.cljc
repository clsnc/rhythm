(ns rhythm.code.tree
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

(defn- line->code
  "Converts a line of text to a code block."
  [line]
  (let [terms (string/split line " " -1)]
    (vec terms)))

(defn text->code
  "Converts a string to a code tree."
  [text]
  (let [lines (string/split text "\n" -1)]
    (vec (map line->code lines))))

(defn term? [node]
  (string? node))

(defn step-path-end
  "Adds step to the last entry in a path."
  [path step]
  (let [[path-start path-last] (utils/split-off-last path)
        new-path-last (+ path-last step)
        new-path (conj (vec path-start) new-path-last)]
    new-path))

(defn- vec-start-slice
  "Returns the portion of a vector before pos."
  [v pos]
  (subvec v 0 pos))

(defn- vec-end-slice
  "Returns the portion of a vector starting with pos."
  [v pos]
  (subvec v pos))

(defn- str-start-slice
  "Returns the substring of a string before pos."
  [s pos]
  (subs s 0 pos))

(defn- str-end-slice
  "Returns the substring of a string starting with pos."
  [s pos]
  (subs s pos))

(defn- start-join-slice-child
  "Joins a sliced child to a sliced node when the start portion is being kept."
  [node child]
  (conj node child))

(defn- end-join-slice-child
  "Joins a sliced child to a sliced node when the end portion is being kept."
  [node child]
  (assoc node 0 child))

(defn- slice-tree
  "Slices a tree along a given path and offset. Whether the start or end slice is 
   returned is determined by the functions passed as vec-slice, str-slice, and 
   join-slice-child."
  [vec-slice str-slice join-slice-child root path offset]
  (let [[slice-pos & path-rest] path
        slice-child (root slice-pos)
        new-slice-child (if (empty? path-rest)
                          (str-slice slice-child offset)
                          (slice-tree vec-slice str-slice join-slice-child
                                      slice-child path-rest offset))
        node-kept-slice (join-slice-child (vec-slice root slice-pos)
                                          new-slice-child)]
    node-kept-slice))

(defn- start-slice-tree
  "Slices a tree along a given path and offset and returns the start slice."
  [root path offset]
  (slice-tree vec-start-slice str-start-slice start-join-slice-child
              root path offset))

(defn- end-slice-tree
  "Slices a tree along a given path and offset and returns the end slice."
  [root path offset]
  (slice-tree vec-end-slice str-end-slice end-join-slice-child
              root path offset))

(defn- join-2-trees
  "Joins 2 trees together, concatenating the end nodes and subnodes of root1 with the 
   start nodes and subnodes of root2. Assumes root1's end path and root2's start path are 
   of equal depth."
  [root1 root2]
  (if (term? root1)
    (str root1 root2)
    (let [join-child-1 (peek root1)
          join-child-2 (first root2)
          joined-child (join-2-trees join-child-1 join-child-2)
          joined-root (utils/vec-concat (drop-last root1)
                                        [joined-child]
                                        (rest root2))]
      joined-root)))

(defn- join-trees
  "Joins 2 or more trees together, concatenating the end nodes of each tree with the 
   start nodes the next one. Assumes each tree's end path has the same depth as the start 
   path of the following tree."
  [root1 & roots]
  (loop [root1 root1
         roots roots]
    (let [[root2 & roots-rest] roots]
      (if (nil? root2)
        root1
        (let [root1+2 (join-2-trees root1 root2)]
          (recur root1+2 roots-rest))))))

(defn replace-range
  "Replace a range in a tree with another tree. Assumes that the edges of new-slice 
   match the depth of the inner edges of root it will be joined with."
  [root range new-slice]
  (let [{{start-path :path
          start-offset :offset} :start
         {end-path :path
          end-offset :offset} :end} range
        start-slice (start-slice-tree root start-path start-offset)
        end-slice (end-slice-tree root end-path end-offset)
        new-root (join-trees start-slice new-slice end-slice)]
    new-root))