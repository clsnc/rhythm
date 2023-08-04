(ns rhythm.code.node 
  (:require [clojure.string :as string]
            [medley.core :as m]))

(declare nodes->new-parent-node nth-child remove-nth-child)

(defn ->code-node [pos->id id->child id]
  {:pos->id pos->id
   :id->child id->child
   :id id})

(defn ->code-term [text id]
  {:text text
   :id id})

(defn ->new-code-node
  "Returns a code node with a new ID."
  [pos->id id->child]
  (->code-node pos->id id->child (gensym)))

(defn ->new-code-term
  "Returns a code term with a new ID."
  [text]
  (->code-term text (gensym)))

(defn append-child
  "Returns node with child appended as a new subnode."
  [node child]
  (let [child-id (:id child)]
    (-> node
        (update :pos->id conj child-id)
        (update :id->child assoc child-id child))))

(defn child-by-id
  "Returns the child of node with ID child-id."
  [node child-id]
  (get-in node [:id->child child-id]))

(defn child-pos-from-id
  "Returns the position of the child of node with ID child-id."
  [node child-id]
  (first (m/find-first #(= (second %) child-id) (m/indexed (:pos->id node)))))

(defn code-term?
  "Returns whether node is a term."
  [node]
  (some? (:text node)))

(defn first-child
  "Returns the first child of node."
  [node]
  (nth-child node 0))

(defn last-child
  "Returns the last child of node."
  [node]
  ((:id->child node) (peek (:pos->id node))))

(defn- line->code-node
  "Converts a line of text to a code node."
  [line]
  (let [term-strs (string/split line " " -1)
        terms (map ->new-code-term term-strs)]
    (nodes->new-parent-node terms)))

(defn- node->edge-term
  "Helper function that returns the first or last descendant term of node depending 
   on whether first or last/peek is passed as edge-f."
  [edge-f node]
  (loop [node node]
    (if (code-term? node)
      node
      (recur ((:id->child node) (edge-f (:pos->id node)))))))

(defn node->last-term
  "Returns the last descendant term of node."
  [node]
  (node->edge-term peek node))

(defn node-children
  "Returns a sequence of child nodes of node."
  [node]
  (map (:id->child node) (:pos->id node)))

(defn- nodes->new-parent-node
  "Returns a new parent node containing nodes as children."
  [nodes]
  (let [pos->id (vec (map :id nodes))
        id->child (into {} (map #(vector (:id %) %) nodes))]
    (->new-code-node pos->id id->child)))

(defn nth-child
  "Returns the child of node that is at pos."
  [node pos]
  (child-by-id node (get-in node [:pos->id pos])))

(defn num-children
  "Returns the number of children of node."
  [node]
  (count (:pos->id node)))

(defn remove-first-child
  "Returns node with its first child removed."
  [node]
  (remove-nth-child node 0))

(defn remove-nth-child
  "Returns node with its nth child removed."
  [node n]
  (-> node
      (update :id->child dissoc ((:pos->id node) n))
      (update :pos->id #(vec (m/remove-nth n %)))))

(defn replace-child-at-pos
  "Returns node with the child at pos replace with new-child."
  [node pos new-child]
  (let [old-pos->id (:pos->id node)
        new-child-id (:id new-child)
        new-pos->id (assoc old-pos->id pos new-child-id)
        old-child-id (old-pos->id pos)
        old-id->child (:id->child node)
        new-id->child (-> old-id->child
                          (dissoc old-child-id)
                          (assoc new-child-id new-child))]
    (assoc node
           :pos->id new-pos->id
           :id->child new-id->child)))

(defn replace-last-child
  "Returns node with the last child replace with new-child."
  [node new-child]
  (replace-child-at-pos node (dec (count (:pos->id node))) new-child))

(defn text->code-node
  "Converts a string to a code node."
  [text]
  (let [lines (string/split text "\n" -1)
        subnodes (map line->code-node lines)]
    (nodes->new-parent-node subnodes)))