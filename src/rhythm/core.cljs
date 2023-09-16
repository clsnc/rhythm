(ns rhythm.core
    (:require
     [reagent.core :as r]
     [reagent.dom.client :as d]
     [rhythm.app-state :as app-state]
     [rhythm.ui.editor :as editor]))

(def state-atom (r/atom (app-state/->start-state)))

(defn swap-state! [f & args]
  (swap! state-atom #(apply f % args)))

;; -------------------------
;; Views

(defn home-page []
  (let [{:keys [code-tree selection]} @state-atom]
    [editor/editor-root code-tree selection swap-state!]))

;; -------------------------
;; Initialize app

(defonce app-root (d/create-root (.getElementById js/document "app")))

(defn mount-root []
  (d/render app-root [home-page]))

(defn ^:export init! []
  (mount-root))
