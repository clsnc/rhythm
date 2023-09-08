(ns rhythm.code.location)

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

(defn add-paths
  "Returns a new path where each step is the sum of the steps at the same positions in paths."
  [& paths]
  (apply map + paths))

(defn- pad-path
  "Prepends enough 0s to a path for it to be length target-len."
  [path target-len]
  (last (take-while #(<= (count %) target-len) (iterate #(conj % 0) (seq path)))))

(defn pad-and-add-paths
  "Returns a new path where each step is the sum of the steps at the same positions in paths. 
   Paths shorter than the longest are padded with 0s at their beginnings to achieve the same 
   length."
  [& paths]
  (let [max-path-len (apply max (map count paths))
        padded-paths (map #(pad-path % max-path-len) paths)]
    (apply add-paths padded-paths)))