(ns rhythm.utils)

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