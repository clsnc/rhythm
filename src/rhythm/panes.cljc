(ns rhythm.panes)

(defrecord pane [id code-path])

(defn ->new-pane
  "Returns a new pane with a unique ID."
  [code-path]
  (->pane (gensym) code-path))