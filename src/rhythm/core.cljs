(ns rhythm.core
    (:require
     [reagent.core :as r]
     [reagent.dom :as d]
     [rhythm.ui.editor :as editor]
     [rhythm.syntax.ast :as ast]))

(def state (r/atom (ast/->empty-ast)))

(defn swap-tree! [f & args]
  (swap! state #(apply ast/update-tree % f args)))

;; -------------------------
;; Views

(defn home-page []
  [editor/editor @state swap-tree!])

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
