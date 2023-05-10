(ns rhythm.ui.motion
  (:require ["framer-motion" :rename {motion jsMotion}]
            [reagent.core :as r]))

(def div (r/adapt-react-class (.-div jsMotion)))