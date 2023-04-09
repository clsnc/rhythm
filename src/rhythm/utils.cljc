(ns rhythm.utils)

(defn matching-indices
  "Returns a sequence of indices in a sequence that match a predicate."
  [pred s]
  (keep-indexed #(when (pred %2) %1) s))

(defn find-first-pos
  "Returns the position of the first occurrence of an element in a collection."
  [element coll]
  (first (matching-indices #(= %1 element) coll)))

(defn split-off-last
  "Returns the a tuple of the form [seq-of-all-but-last-element last-element]."
  [s]
  [(drop-last s) (last s)])

(defn split-str-at-pos
  "Splits a string s at position pos and returns a tuple of the form 
   [s-part-1 s-part-2]."
  [s pos]
  [(subs s 0 pos) (subs s pos)])

(defn update-root-or-in
  "Like update-in, but applies f and supplied arguments directly to m if ks is 
   empty."
  [m ks f & args]
  (if (empty? ks)
    (apply f m args)
    (apply update-in m ks f args)))