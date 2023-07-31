(ns rhythm.ui.prespace)

(defn prespace-path?
  "Returns whether p is a prespace path."
  [p]
  (= (peek p) :prespace))

(defn prespace-path->path
  "Converts a prespace path to a regular path."
  [pp]
  (vec (drop-last pp)))

(defn path->prespace-path
  "Converts a regular path to a prespace path."
  [p]
  (conj p :prespace))

(defn prespace-point?
  "Returns whether p is a prespace point."
  [p] 
  (prespace-path? (:path p)))

(defn prespace-point->point
  "Converts a prespace point to a regular point."
  [pp]
  (-> pp
      (update :path prespace-path->path)
      (update :offset dec)))

(defn maybe-prespace-point->point
  "If p is a prespace point, returns p converted to a regular point. If not, returns p."
  [p]
  (if (prespace-point? p)
    (prespace-point->point p)
    p))

(defn maybe-prespace-range->range
  "If pr is a prespace range, returns pr converted to a regular range. If not, returns pr."
  [pr]
  (update-vals pr maybe-prespace-point->point))