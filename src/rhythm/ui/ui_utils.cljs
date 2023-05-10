(ns rhythm.ui.ui-utils)

(defn stop-propagation! [event]
  (.stopPropagation event))