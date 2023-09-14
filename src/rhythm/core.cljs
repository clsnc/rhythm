(ns rhythm.core
    (:require
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
  (let [{:keys [code-tree selection]} @state-atom]
    [e/EditorRoot
     ;; Only the contents of the EditorRoot should be shown, not the div itself.
     {:style {:position :absolute
              :max-height 0
              :max-width 0
              :outline :none}
      :onChange #(actions/handle-editor-content-change! % swap-state!)
      :onKeyDown #(actions/handle-editor-key-down! % selection swap-state!)
      :onSelect #(actions/handle-editor-selection-change! % swap-state!)
      :selection selection}
     ^{:key (gensym)} [editor/editor-pane code-tree]]))

;; -------------------------
;; Initialize app

(defonce app-root (d/create-root (.getElementById js/document "app")))

(defn mount-root []
  (d/render app-root [home-page]))

(defn ^:export init! []
  (mount-root))
