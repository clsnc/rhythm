(ns rhythm.ui.range-offsets)

(defn add-point-space-offset
  "Adds an editor space offset to a code point."
  [p]
  (update p :offset inc))

(defn remove-point-space-offset
  "Removes an editor space offset from a code point."
  [p]
  (update p :offset dec))

(defn add-range-space-offset
  "Adds an editor space offset to a code range."
  [r]
  (update-vals r add-point-space-offset))

(defn remove-range-space-offset
  "Removes an editor space offset from a code range."
  [r]
  (update-vals r remove-point-space-offset))