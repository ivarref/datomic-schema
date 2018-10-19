# datomic-schema

Simplified writing of [Datomic schemas](https://docs.datomic.com/on-prem/schema.html).

## Installation

[![Clojars Project](http://clojars.org/ivarref/datomic-schema/latest-version.svg)](http://clojars.org/ivarref/datomic-schema)

Add `[ivarref/datomic-schema "0.1.5"]` to your dependency vector.

## Syntax

The syntax is

```
#d/schema[[attribute-name cardinality? type toggles* docstring?]*]

attribute-name = The name of your attribute
cardinality    = :one or :many. Defaults to :one
type           = A type from the list below
toggles        = Zero or more toggles
docstring      = An optional documentation string

type           = :bigdec  |
                 :bigint  |
                 :boolean |
                 :bytes   |
                 :double  |
                 :float   |
                 :instant |
                 :keyword |
                 :long    |
                 :ref     |
                 :string  |
                 :uri     |
                 :uuid

toggles        = :component  |
                 :fulltext   |
                 :id         | ; alias for :identity
                 :identity   |
                 :index      |
                 :no-history |
                 :unique

All arguments except docstring must be Clojure keywords.

Enums are also supported: #d/schema [[attribute-name :enum]]
```

## Example usage from REPL

```clojure
(require 'datomic-schema.core)

#d/schema [[:entity/attr :one :string "Documentation"]]
=>
[{:db/id #db/id[:db.part/db -1000001],
  :db/ident :entity/attr,
  :db/valueType :db.type/string,
  :db/cardinality :db.cardinality/one,
  :db.install/_attribute :db.part/db,
  :db/doc "Documentation"}]

#d/schema [[:entity/attr :many :long :unique]]
=>
[{:db/id #db/id[:db.part/db -1000002],
  :db/ident :entity/attr,
  :db/valueType :db.type/long,
  :db/cardinality :db.cardinality/many,
  :db.install/_attribute :db.part/db,
  :db/unique :db.unique/value}]
  
#d/schema [[:entity/attr :enum]]
=>
[{:db/ident :entity/attr}]
```

and so forth.

## Example usage with conformity

In order to use this library with [conformity](https://github.com/rkneufeld/conformity),
you will need to require the namespace so that the reader literal is loaded before `conformity` runs.

Add the following file to `resources/`:

```
;; resources/something.edn
{:my-project/something-schema
 {:txes [#d/schema[[:something/title :one :string]
                   [:something/author :one :uuid]]]}}
```

Then in your code:

```clojure
(ns my-project.something
  (:require [datomic-schema.core] ; namespace needs to be required so that reader literal is loaded
            [io.rkn.conformity :as c]
            [datomic.api :as d]))

(def uri "datomic:mem://my-project")
(d/create-database uri)
(def conn (d/connect uri))

(def norms-map (c/read-resource "something.edn"))

(println (str "Has attribute? " (c/has-attribute? (d/db conn) :something/title)))
(c/ensure-conforms conn norms-map [:my-project/something-schema])
(println (str "Has attribute? " (c/has-attribute? (d/db conn) :something/title)))
```

## Notes

This project is derived from [cognitect-labs/vase](https://github.com/cognitect-labs/vase).

## License

Copyright © 2015-2017 Cognitect, Inc. All rights reserved.

Copyright © 2018 Ivar Refsdal.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
