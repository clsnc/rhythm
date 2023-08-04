(ns rhythm.ui.prespace)

(defn prespace-id?
  "Returns whether p is a prespace ID."
  [p]
  (and (vector? p) (= (peek p) :prespace)))

(defn prespace-eid->eid
  "Converts a prespace ID to a regular ID."
  [pp]
  (first pp))

(defn eid->prespace-eid
  "Converts a regular ID to a prespace ID."
  [p]
  [p :prespace])

(defn prespace-point?
  "Returns whether p is a prespace point."
  [p] 
  (prespace-id? (:id p)))

(defn prespace-point->point
  "Converts a prespace point to a regular point."
  [pp]
  (-> pp
      (update :id prespace-eid->eid)
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