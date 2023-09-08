(ns rhythm.utils
  (:require [clojure.core.rrb-vector :as v]))

(declare update-parent-in)

(defn assoc-from-end
  "Like assoc, but pos is relative to the end of the vector. 0 is the last element, 1 is the second 
   to last, etc."
  [v pos val]
  (assoc v (- (count v) 1 pos) val))

(defn split-off-last
  "Returns the a tuple of the form [seq-of-all-but-last-element last-element]."
  [s]
  [(drop-last s) (last s)])

(defn update-root-or-in
  "Like update-in, but applies f and supplied arguments directly to m if ks is 
   empty."
  [m ks f & args]
  (if (empty? ks)
    (apply f m args)
    (apply update-in m ks f args)))

(defn vec-concat
  "Returns a vector that is the concatenation of the given arguments."
  [& vs]
  (vec (apply concat vs)))

(defn vec-cons
  "Returns a new vector of e prepended to v."
  [v e]
  (v/catvec [e] v))

(defn vec-insert-multiple
  "Merge the contents of insert-seq into a vector v at pos."
  [v pos insert-seq]
  (vec-concat (subvec v 0 pos)
              insert-seq
              (subvec v pos)))

(defn update-parent-in
  "Returns a nested associative structure m with the parent of the value at ks replaced with 
   the result of (apply f parent child-pos args)."
  [m ks f & args]
  (let [[ancestor-ks k] (split-off-last ks)]
    (apply update-in m ancestor-ks f k args)))

(defn vec-insert-multiple-in
  "Returns a vector in a nested associative sturcture with the contents of insert-seq merged 
   in beginning at position ks."
  [root ks insert-seq]
  (update-parent-in root ks vec-insert-multiple insert-seq))

(defn vec-remove-range
  "Returns a vector with elements with positions start <= position < end removed."
  [v start end]
  (vec (concat (subvec v 0 start) (subvec v end))))

(defn vec-remove-nth
  "Returns a vector without the value at pos."
  [v pos]
  (vec-remove-range v pos (inc pos)))

(defn vec-remove-nth-in
  "Returns a vector in a nested associative structure without the value at ks."
  [m ks]
  (update-parent-in m ks vec-remove-nth))

(defn vec-split-at [n coll]
  (map vec (split-at n coll)))