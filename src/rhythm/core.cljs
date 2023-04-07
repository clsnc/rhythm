(ns rhythm.core
    (:require
     [reagent.core :as r]
     [reagent.dom :as d]
     [rhythm.ui.editor :as editor]
     [rhythm.syntax.ast :as ast]))

(def state (r/atom (ast/->empty-ast)))

(defn swap-block! [path op]
  (swap! state #(ast/update-block % path op)))

;; -------------------------
;; Views

(defn home-page []
  [editor/editor @state swap-block!])

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
