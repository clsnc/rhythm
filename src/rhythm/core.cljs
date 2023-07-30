(ns rhythm.core
    (:require
     [reagent.core :as r]
     [reagent.dom.client :as d]
     [rhythm.app-state :as app-state]
     [rhythm.ui.actions :as actions]
     [rhythm.ui.editor :as editor]
     [rhythm.ui.editor-framework.interop :as e]
     [rhythm.ui.range-offsets :as ro]))

(def state-atom (r/atom (app-state/->start-state)))

(defn swap-state! [f & args]
  (swap! state-atom #(apply f % args)))

;; -------------------------
;; Views

(defn home-page []
  (let [{:keys [code-root selection]} @state-atom]
    [e/EditorRoot
     ;; Only the contents of the EditorRoot should be shown, not the div itself.
     {:style {:position :absolute
              :max-height 0
              :max-width 0
              :outline :none}
      :onChange #(actions/handle-editor-content-change! % swap-state!)
      :onSelect #(actions/handle-editor-selection-change! % swap-state!)
      :selection (ro/add-range-space-offset selection)}
     ^{:key (gensym)} [editor/editor-pane code-root]]))

;; -------------------------
;; Initialize app

(defonce app-root (d/create-root (.getElementById js/document "app")))

(defn mount-root []
  (d/render app-root [home-page]))

(defn ^:export init! []
  (mount-root))
