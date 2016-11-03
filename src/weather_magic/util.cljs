(ns weather-magic.util)

(defn transparent-println
  "Print something and return that something."
  [item]
  (println item)
  item)

(defn toggle
  "Add item to set if it's not in set, remove item from set if it's already in it.
   In other words, toggle the item in the set."
  [set item]
  (transparent-println
   ((if (contains? set item) disj conj) set item)))

