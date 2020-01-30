(ns datomic-schema.core-test
  (:require [clojure.test :refer :all]
            [datomic-schema.core :refer :all]))

(deftest test-schema-tx
  (are [case input expected] (testing case (= expected (read-string input)))
                             "One attribute"
                             "#datomic/schema[[:entity/attribute :one :long \"A docstring\"]]"
                             [{:db/ident       :entity/attribute
                               :db/valueType   :db.type/long
                               :db/cardinality :db.cardinality/one
                               :db/doc         "A docstring"}]

                             "Two attributes"
                             "#datomic/schema[[:e/a1 :one :long \"\"] [:e/a2 :many :string \"docstring 2\"]]"
                             [{:db/ident       :e/a1
                               :db/valueType   :db.type/long
                               :db/cardinality :db.cardinality/one
                               :db/doc         ""}
                              {:db/ident       :e/a2
                               :db/valueType   :db.type/string
                               :db/cardinality :db.cardinality/many
                               :db/doc         "docstring 2"}]

                             "One toggle"
                             "#datomic/schema[[:e/a :one :long :identity \"Doc\"]]"
                             [{:db/ident       :e/a
                               :db/valueType   :db.type/long
                               :db/cardinality :db.cardinality/one
                               :db/unique      :db.unique/identity
                               :db/doc         "Doc"}]

                             "Several toggles"
                             "#datomic/schema[[:e/a :one :string :identity :index :component :no-history :fulltext \"Doc\"]]"
                             [{:db/ident       :e/a
                               :db/valueType   :db.type/string
                               :db/cardinality :db.cardinality/one
                               :db/index       true
                               :db/isComponent true
                               :db/noHistory   true
                               :db/fulltext    true
                               :db/unique      :db.unique/identity
                               :db/doc         "Doc"}]

                             "Docstring is optional"
                             "#datomic/schema[[:e/a :one :string]]"
                             [{:db/ident       :e/a
                               :db/valueType   :db.type/string
                               :db/cardinality :db.cardinality/one}]

                             "Cardinality is optional"
                             "#datomic/schema[[:e/a :string]]"
                             [{:db/ident       :e/a
                               :db/valueType   :db.type/string
                               :db/cardinality :db.cardinality/one}]

                             "Cardinality is optional and works with toggles"
                             "#datomic/schema[[:e/a :string :unique]]"
                             [{:db/ident       :e/a
                               :db/valueType   :db.type/string
                               :db/cardinality :db.cardinality/one
                               :db/unique      :db.unique/value}]

                             ":id shorthand"
                             "#datomic/schema[[:e/a :string :id]]"
                             [{:db/ident       :e/a
                               :db/valueType   :db.type/string
                               :db/cardinality :db.cardinality/one
                               :db/unique      :db.unique/identity}]

                             "Composite Tuples"
                             "#datomic/schema[[:semester/year+season [:semester/year :semester/season] :id]]"
                             [{:db/ident       :semester/year+season
                               :db/valueType   :db.type/tuple
                               :db/tupleAttrs  [:semester/year :semester/season]
                               :db/cardinality :db.cardinality/one
                               :db/unique      :db.unique/identity}]

                             "Heterogeneous Tuples"
                             "#datomic/schema[[:player/location [:long :long]]]"
                             [{:db/ident       :player/location
                               :db/valueType   :db.type/tuple
                               :db/tupleTypes  [:db.type/long :db.type/long]
                               :db/cardinality :db.cardinality/one}]

                             "Fully qualified named heterogeneous tuple"
                             "#datomic/schema[[:player/location [:db.type/long :db.type/long]]]"
                             [{:db/ident       :player/location
                               :db/valueType   :db.type/tuple
                               :db/tupleTypes  [:db.type/long :db.type/long]
                               :db/cardinality :db.cardinality/one}]

                             "Homogeneous Tuples"
                             "#datomic/schema[[:db/tupleAttrs [:keyword]]]"
                             [{:db/ident       :db/tupleAttrs
                               :db/valueType   :db.type/tuple
                               :db/tupleType   :db.type/keyword
                               :db/cardinality :db.cardinality/one}]

                             "Enum support"
                             "#datomic/schema[[:e/a :enum]]"
                             [{:db/ident :e/a}]

                             "Shorthand works"
                             "#d/schema[[:e/a :enum]]"
                             [{:db/ident :e/a}])
  (are [bad-input] (thrown? Throwable (read-string bad-input))
                   "#datomic/schema[[:e/a :one-is-the-lonliest-number :long \"doc\"]]"
                   "#datomic/schema[[:e :one :long \"doc\"]]"
                   "#datomic/schema[[\"not a keyword\" :one :ref \"doc\"]]"
                   "#datomic/schema[[:e/a :one :categorically-imperative \"\"]]"
                   "#datomic/schema[#{:e/a :one :ref \"doc\"}]"))
