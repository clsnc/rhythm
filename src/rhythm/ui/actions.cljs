(ns rhythm.ui.actions
  (:require [rhythm.syntax.blocks :as blocks]
            [rhythm.utils :as utils]))

(defn ->header-change-handler
  "Returns an onChange event handler for the header of an editor block that handles
   updating the editor."
  [path swap-block!]
  (fn [event]
    (let [new-text (-> event .-target .-value)
          text-updater #(blocks/update-header % new-text)]
      (swap-block! path text-updater))))

(defn- enter-down-handler!
  "Handles an onKeyDown event for Enter."
  [event path swap-block!]
  (.preventDefault event)
  (let [cursor-pos (-> event .-target .-selectionStart)
        [parent-path child-id] (utils/split-off-last path)
        splitter #(blocks/split-child-in-header % child-id cursor-pos)]
    (swap-block! parent-path splitter)))

(defn- tab-down-handler!
  "Handles an onKeyDown event for Tab."
  [event path swap-block!]
  (.preventDefault event)
  (let [[parent-path child-id] (utils/split-off-last path)
        mover #(blocks/move-child-inside-preceding-sibling % child-id)]
    (swap-block! parent-path mover)))

(defn ->key-down-handler
  "Returns an onKeyDown event handler for the header of an editor block."
  [path swap-block!]
  (fn [event]
    (let [pressed-key (.-key event)]
      (cond 
        (= pressed-key "Enter") (enter-down-handler! event path swap-block!)
        (= pressed-key "Tab") (tab-down-handler! event path swap-block!)))))