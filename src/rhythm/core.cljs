(ns rhythm.core
    (:require
     [medley.core :as m]
     [reagent.core :as r]
     [reagent.dom.client :as d]
     [rhythm.app-state :as app-state]
     [rhythm.ui.actions :as actions]
     [rhythm.ui.editor :as editor]
     [rhythm.ui.editor-framework.interop :as e]))

(def state-atom (r/atom (app-state/->start-state)))

(defn swap-state! [f & args]
  (swap! state-atom #(apply f % args)))

;; -------------------------
;; Views

(defn home-page []
  (let [{:keys [ast selection]} @state-atom
        code-tree-root (:root ast)]
    [e/EditorRoot
     ;; Only the contents of the EditorRoot should be shown, not the div itself.
     {:style {:position :absolute
              :max-height 0
              :max-width 0
              :outline :none}
      :onChange #(actions/handle-editor-content-change! % swap-state!)
      :onSelect #(actions/handle-editor-selection-change! % swap-state!)
      :selection selection}
     (for [[code-node-pos code-node] (m/indexed (:children code-tree-root))]
       ^{:key (:id code-node)} [editor/editor-pane code-node [code-node-pos]])]))

;; -------------------------
;; Initialize app

(defonce app-root (d/create-root (.getElementById js/document "app")))

(defn mount-root []
  (d/render app-root [home-page]))

(defn ^:export init! []
  (mount-root))
