(ns rhythm.ui.prespace
  (:require [clojure.core.rrb-vector :as v]))

(defn prespace-path?
  "Returns whether p is a prespace ID."
  [p]
  (and (vector? p) (= (peek p) :prespace)))

(defn prespace-path->path
  "Converts a prespace ID to a regular ID."
  [pp]
  (v/subvec pp 0 (dec (count pp))))

(defn path->prespace-path
  "Converts a regular ID to a prespace ID."
  [p]
  (conj p :prespace))