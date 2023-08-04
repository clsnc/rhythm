(ns rhythm.code.location)

(defn ->code-point [node-id offset]
  {:id node-id
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