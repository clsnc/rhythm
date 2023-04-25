(ns rhythm.utils)

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

(defn vec-remove-range
  "Returns a vector with elements with positions start <= position < end removed."
  [v start end]
  (vec (concat (subvec v 0 start) (subvec v end))))