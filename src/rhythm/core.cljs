(ns rhythm.core
    (:require
     [reagent.core :as r]
     [reagent.dom :as d]
     [rhythm.app-state :as app-state]
     [rhythm.ui.editor :as editor]
     [rhythm.syntax.ast :as ast]))

(def state-atom (r/atom (app-state/->AppState (ast/->empty-ast) nil)))

(defn swap-editor-state! [f & args]
  (swap! state-atom #(apply f % args)))

;; -------------------------
;; Views

(defn home-page []
  (let [{:keys [ast selection]} @state-atom]
    [editor/editor ast selection swap-editor-state!]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
