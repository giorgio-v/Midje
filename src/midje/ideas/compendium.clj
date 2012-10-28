(ns ^{:doc "A compendium is 'a collection of concise but detailed information
            about a particular subject. The Midje compendium contains
            the currently relevant facts about your program."}
  midje.ideas.compendium)

(def ^:dynamic *parse-time-fact-level* 0)

(defmacro given-possible-fact-nesting [& forms]
  `(binding [*parse-time-fact-level* (inc *parse-time-fact-level*)]
     ~@forms))

(defmacro working-on-nested-facts [& forms]
  ;; Make sure we don't treat this as a top-level fact
  `(binding [*parse-time-fact-level* (+ 2 *parse-time-fact-level*)]
     ~@forms))


(def fact-check-history (atom (constantly true)))

(defn dereference-history []
  @(ns-resolve 'midje.ideas.compendium @fact-check-history))
  

(defn wrap-with-check-time-fact-recording [true-name form]
  (if (= *parse-time-fact-level* 1)
    `(do (record-fact-check '~true-name)
         ~form)
    form))

(def by-namespace-compendium (atom {}))

(defn reset-compendium []
  (reset! by-namespace-compendium {}))

(defn compendium-contents []
  (apply concat (vals @by-namespace-compendium)))
  
(defn namespace-facts [namespace]
  (get @by-namespace-compendium
       (if (= (type namespace) clojure.lang.Namespace)
         (ns-name namespace)
         namespace)))

;; TODO: the use of the true-name symbol means accumulation of
;; non-garbage-collected crud as functions are redefined. Worry about
;; that later.

;; I must be brain-dead, because this code has got to be way too complicated.
(defn record-fact-existence [function]
  (let [metadata (meta function)
        fact-namespace (:midje/namespace metadata)]
    (intern 'midje.ideas.compendium (:midje/true-name metadata) function)
    (when (contains? metadata :midje/name)
      (let [same-namespace-functions (namespace-facts fact-namespace)
            without-old (remove (fn [f]
                                  (= (:midje/name metadata) (:midje/name (meta f))))
                                same-namespace-functions)]
        (swap! by-namespace-compendium
               assoc fact-namespace without-old)))
    (swap! by-namespace-compendium
           #(merge-with concat % { fact-namespace [function] }))))

(defn record-fact-check [true-name]
  (reset! fact-check-history true-name))

(defn check-some-facts [fact-functions]
  (every? true? (map #(%) fact-functions)))