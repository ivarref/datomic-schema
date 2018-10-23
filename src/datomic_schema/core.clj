(ns datomic-schema.core)

;; Schema literals
;; ---------------
(def ^:private accepted-schema-toggles #{:unique :id :identity :index :fulltext :component :no-history})
(def ^:private accepted-kinds #{:keyword :string :boolean :long :bigint :float :double :bigdec :ref :instant :uuid :uri :bytes})
(def ^:private accepted-cards #{:one :many})

(def ^:private schema-tx-usage
  "#d/schema[[attribute-name cardinality? type toggles* docstring]* ]")

(defmacro schema-problem
  [flavor actual]
  `(str "#d/schema must look like this:\n\n"
        schema-tx-usage
        "\n\n"
        ~flavor
        "\n\n"
        "I got:\n\n"
        (pr-str ~actual)))

(defmacro schema-assert
  [f flavor emit]
  `(when-not ~f
     (throw (AssertionError. (schema-problem ~flavor ~emit)))))

(defn- parse-schema-vec
  [s-vec]
  (let [doc-string (when (string? (last s-vec)) (last s-vec))
        s-vec (if doc-string (butlast s-vec) s-vec)
        [ident card kind & _] (take 3 s-vec)
        opt-toggles (take-while keyword? (drop 3 s-vec))]
    (cond (and (= 2 (count s-vec))
               (= :enum (last s-vec)))
          {:db/ident (first s-vec)}

          (not (contains? accepted-cards card))
          (parse-schema-vec (into [ident :one] (rest s-vec)))

          :else (do
                  (schema-assert (every? keyword? s-vec)
                                 "All of _attribute-name_, _cardinality_, _type_, and _toggles_ must be Clojure keywords."
                                 s-vec)
                  (schema-assert (some? (namespace ident)) "Ident must have namespace" ident)
                  (schema-assert (every? #(contains? accepted-schema-toggles %) opt-toggles)
                                 (str "Short schema toggles must be taken from " accepted-schema-toggles) opt-toggles)
                  (schema-assert (contains? accepted-kinds kind) (str "The value type must be one of " accepted-kinds) kind)
                  (schema-assert (contains? accepted-cards card) (str "The cardinality must be one of " accepted-cards) card)
                  (merge {:db/ident              ident
                          :db/valueType          (keyword "db.type" (name kind))
                          :db/cardinality        (keyword "db.cardinality" (name card))
                          :db.install/_attribute :db.part/db}
                         (when doc-string {:db/doc doc-string})
                         (reduce (fn [m opt]
                                   (merge m (case opt
                                              :unique {:db/unique :db.unique/value}
                                              :identity {:db/unique :db.unique/identity}
                                              :id {:db/unique :db.unique/identity}
                                              :index {:db/index true}
                                              :fulltext {:db/fulltext true
                                                         :db/index    true}
                                              :component {:db/isComponent true}
                                              :no-history {:db/noHistory true}
                                              nil)))
                                 {}
                                 opt-toggles))))))

(defn schema-tx [form]
  (schema-assert (vector? form) "The top level must be a vector." form)
  (schema-assert (every? vector? form) "The top level vector must only contain other vectors" form)
  (mapv parse-schema-vec form))

(defn schema [form]
  (schema-tx form))

(defn s [form]
  (schema-tx form))