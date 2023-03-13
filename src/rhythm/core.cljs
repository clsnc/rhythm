(ns rhythm.core
    (:require
     [reagent.core :as r]
     [reagent.dom :as d]
     [rhythm.editor :as editor]
     [rhythm.project :as proj]))

(def state (r/atom proj/default-text-tree))

(defn replace-root! [new-tree]
  (reset! state new-tree))

;; -------------------------
;; Views

(defn home-page []
  [editor/editor @state replace-root!])

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
