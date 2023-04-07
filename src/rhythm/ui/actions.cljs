(ns rhythm.ui.actions
  (:require [rhythm.syntax.blocks :as blocks]))

(defn ->header-change-handler
  "Returns an onChange event handler for the header of an editor block that handles
   updating the editor."
  [path swap-block!]
  (fn [event]
    (let [new-text (-> event .-target .-value)
          text-updater #(blocks/update-header % new-text)]
      (swap-block! path text-updater))))

(defn- enter-down-handler
  "Handles an onKeyDown event for Enter."
  [event path swap-block!]
  (.preventDefault event)
  (let [cursor-pos (-> event .-target .-selectionStart)
        parent-path (drop-last path)
        child-id (last path)
        splitter #(blocks/split-child-in-header % child-id cursor-pos)]
    (swap-block! parent-path splitter)))

(defn ->key-down-handler
  "Returns an onKeyDown event handler for the header of an editor block."
  [path swap-block!]
  (fn [event]
    (let [pressed-key (.-key event)]
      (if (= pressed-key "Enter")
        (enter-down-handler event path swap-block!)
        nil))))