(ns rhythm.utils)

(defn matching-indices
  "Returns a sequence of indices in a sequence that match a predicate."
  [pred s]
  (keep-indexed #(when (pred %2) %1) s))

(defn find-first-pos
  "Returns the position of the first occurrence of an element in a collection."
  [element coll]
  (first (matching-indices #(= %1 element) coll)))