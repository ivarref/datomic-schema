(ns datomic-schema.core
  (:require [clojure.string :as str]))

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

(def ^:private tuple-scalars (into (sorted-set) #{:db.type/bigdec :db.type/bigint :db.type/boolean :db.type/double
                                                  :db.type/instant :db.type/keyword :db.type/long :db.type/string
                                                  :db.type/symbol :db.type/ref :db.type/uri :db.type/uuid}))

(defn- tuple-scalar [attr]
  (or (some #{attr} tuple-scalars)
      (some #{(keyword "db.type" (name attr))} tuple-scalars)))

(defn- ns-keyword? [kw]
  (and (keyword? kw)
       (some? (namespace kw))))

(defn- tuple-attrs [attrs]
  (if (= 1 (count attrs))
    (some->> (tuple-scalar (first attrs))
             (assoc {} :db/tupleType)) ; Homogeneous Tuples
    (when (and (>= (count attrs) 2)
               (<= (count attrs) 8))
      (cond (every? tuple-scalar attrs)
            (assoc {} :db/tupleTypes (mapv tuple-scalar attrs)) ; Heterogeneous Tuples

            (some tuple-scalar attrs)
            nil

            (every? ns-keyword? attrs)
            (assoc {} :db/tupleAttrs (mapv identity attrs)) ; Composite Tuples

            :else
            nil))))

(defn- parse-schema-vec
  [s-vec]
  (let [doc-string (when (string? (last s-vec)) (last s-vec))
        s-vec (if doc-string (butlast s-vec) s-vec)
        [ident card kind & _] (take 3 s-vec)
        opt-toggles (take-while keyword? (drop 3 s-vec))]
    (cond (and (= 2 (count s-vec))
               (= :enum (last s-vec)))
          {:db/ident ident}

          (not (contains? accepted-cards card))
          (parse-schema-vec (into [ident :one] (rest s-vec)))

          :else
          (do
            (schema-assert (ns-keyword? ident) "attribute-name must be a namespaced keyword." s-vec)
            #_(schema-assert (every? keyword? s-vec)
                             "All of _attribute-name_, _cardinality_, _type_, and _toggles_ must be Clojure keywords."
                             s-vec)
            (schema-assert (every? #(contains? accepted-schema-toggles %) opt-toggles)
                           (str "Short schema toggles must be taken from " accepted-schema-toggles) opt-toggles)
            (when (keyword? kind)
              (schema-assert (contains? accepted-kinds kind) (str "The value type must be one of " accepted-kinds) kind))
            (when (vector? kind)
              (schema-assert (tuple-attrs kind) (str "The type of a tuple must be:\n"
                                                     "* Composite tuple: The value must be a vector of 2-8 keywords naming other attributes.\n"
                                                     "* Heterogeneous fixed length tuple: The value must be a vector of 2-8 scalar value types: "
                                                     (str/join ", " (mapv #(str ":" (name %)) tuple-scalars)) "\n"
                                                     "* Homogeneous variable length tuple: The value must be a vector containing 1 scalar type.")
                             kind))
            (schema-assert (contains? accepted-cards card) (str "The cardinality must be one of " accepted-cards) card)
            (merge {:db/ident       ident
                    :db/cardinality (keyword "db.cardinality" (name card))}
                   (when (keyword? kind)
                     {:db/valueType (keyword "db.type" (name kind))})
                   (when (vector? kind)
                     (merge {:db/valueType :db.type/tuple}
                            (tuple-attrs kind)))
                   (when doc-string
                     {:db/doc doc-string})
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

(comment
  (parse-schema-vec [:hello/world [:keyword]]))


(defn schema-tx [form]
  (schema-assert (vector? form) "The top level must be a vector." form)
  (schema-assert (every? vector? form) "The top level vector must only contain other vectors" form)
  (mapv parse-schema-vec form))

(defn schema [form]
  (schema-tx form))

(defn s [form]
  (schema-tx form))