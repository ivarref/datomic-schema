# datomic-schema

Simplified writing of Datomic schemas.

## Installation

[![Clojars Project](http://clojars.org/dato-schema/latest-version.svg)](http://clojars.org/dato-schema)

Add `[dato-schema "0.1.3"]` to your dependency vector.


## Usage

```clojure
(require 'dato-schema.core)

#datomic/schema [[:entity/attr :one :string "Documentation"]]
=>
[{:db/id #db/id[:db.part/db -1000001],
  :db/ident :entity/attr,
  :db/valueType :db.type/string,
  :db/cardinality :db.cardinality/one,
  :db.install/_attribute :db.part/db,
  :db/doc "Documentation"}]

#datomic/schema [[:entity/attr :many :long :unique]]
=>
[{:db/id #db/id[:db.part/db -1000002],
  :db/ident :entity/attr,
  :db/valueType :db.type/long,
  :db/cardinality :db.cardinality/many,
  :db.install/_attribute :db.part/db,
  :db/unique :db.unique/value}]
  
#datomic/schema [[:entity/attr :enum]]
=>
[{:db/ident :entity/attr}]
```

and so forth.

Supported types is `:keyword :string :boolean :long :bigint :float :double :bigdec :ref :instant :uuid :uri :bytes`.

Supported list of toggles is `:unique :identity :index :fulltext :component :no-history`.

Last parameter is an optional documentation string.

## Notes

This project is derived from [cognitect-labs/vase](https://github.com/cognitect-labs/vase).

## License

Copyright © 2015-2017 Cognitect, Inc. All rights reserved.

Copyright © 2018 Ivar Refsdal.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
