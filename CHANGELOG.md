# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-08-17

Everything below lands on top of `1.0.0-RC1`. The breaking changes are marked as such, and each of
them is a rename or a signature change that the compiler points at, so nothing fails silently.

### Breaking

* **`EntityDaoFactoryBean` is abstract now.** Name the one of the provider you run on instead:

  ```java
  @EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class, ...)
  ```

  `HibernateEntityDaoFactoryBean`, `EclipseLinkEntityDaoFactoryBean` and
  `StandardEntityDaoFactoryBean` are the three to pick from, and whichever is named there settles
  the provider the whole library talks to.
* **`JpaUpdate.set(String attributeName, String anotherAttributeName)` is renamed to
  `setProperty`.** It used to collide with `set(String, T)` whenever the value was a string, which
  made `set("username", "Tom")` assign a column named `Tom` rather than that literal.
* **`JpaPage.join` and `JpaPage.rightJoin` taking a lambda return `JpaPage<X, T>`** instead of
  `JpaPage<E, T>`, which is what `leftJoin` already returned and what the chaining actually yields.
* **`Model` gained `getFrom()`, `getFrom(String)` and `aliasOf(String)`.** Only an implementation of
  that interface outside this library is affected, and every implementation shipped here is final
  in practice.
* **`Property.forName(SerializableFunction, Class)` no longer resolves the alias while it is
  built.** It records the entity instead and resolves against the model at evaluation time. The
  written form is unchanged; see *Table aliases* below for what this fixes.

### Added

* **Grouping pagination counted by a derived table.** `rowCount()` on a grouping query used to mean
  fetching every group and counting the rows. It now runs
  `select count(1) from (select 1 ... group by ... having ...)`, one statement whatever the number
  of groups, having clause included.
* **Branched joins.** A lambda carries the entity it belongs to, so a join starts from that entity
  rather than from the one the previous join reached:

  ```java
  orderProductDao.customPage()
          .join(OrderProduct::getOrder, "o", null)      // OrderProduct -> Order
          .join(Order::getUser, "u", null)              // Order        -> User
          .join(OrderProduct::getProduct, "p", null)    // OrderProduct -> Product, a second branch
  ```

  `join(fromAlias, attributeName, alias, on)` names the starting table explicitly, and joining by an
  entity class searches the entity in focus and then the ones before it.
* **Fetch join** — `fetch(...)` and `leftFetch(...)` on a query and on a pagination.
* **Joining a subquery as a derived table** — `joinSubQuery(...)` and `leftJoinSubQuery(...)`, which
  a pagination joins into its counting query as well.
* **Several columns in a subquery** — `JpaSubQuery.select(ColumnList)` and
  `JpaSubQueryGroupBy.select(ColumnList)`, which is what a derived table needs.
* **`Fields.year/month/day`** — the parts of a date, rendered the way every database spells them.
* **`Column.forSubQuery(subQuery, alias)`** — a scalar subquery as one column.
* **`JpaSubQueryGroupBy` extends `SubQueryBuilder`**, so a grouping subquery is handed to a filter
  directly.
* **`JpaDelete.subQuery` and `JpaUpdate.subQuery` take an alias**, and the subquery keeps the model
  of the statement behind its own so that the two get correlated.
* **A provider abstraction** — `JpaProvider` with eleven capabilities behind it, and three
  implementations: Hibernate (the default), the Criteria API alone, and EclipseLink. The common
  package holds no Hibernate import any more.
* **Overloads filled in** — `Restrictions.eq/ne/like/notLike/in` addressing an attribute by name
  alone, the aggregations of `Fields` addressing one by alias, and `FilterList` grown from the
  lambda forms to the full set its `Restrictions` counterpart has.
* **Native sql upon the Criteria API alone**, and a mapping of its own for EclipseLink.
* Test profiles for PostgreSQL, MySQL and EclipseLink, and a JaCoCo report.

### Fixed

* **`Restrictions.is(false)` matched everything.** It rendered `FALSE is false`.
* **`Restrictions.notLike` was not the complement of `like`.** `like` wrapped the pattern in `%`
  while `notLike` did not.
* **`Function.build(String, Class, String...)` threw a `NullPointerException`** for more than one
  argument, since the index it filled never moved.
* **`FilterList.lte(function, subQuery)` compared by `<`**, calling `Restrictions.lt`.
* **A collection association resolved to `java.util.List`.** `JoinModel.getType()` returned the
  collection instead of the entity behind it, which broke the alias it registered and made
  `join(Class)` report `Not an entity: java.util.List`.
* **`Column.forSubQuery` selected the inside of the subquery** rather than the subquery itself, so
  the sql it built referred to a table the outer query never joined.
* **`PageResponse.toPage()` reported the wrong page.** The page number of this library is one
  based, and it went into the zero based `PageImpl` of Spring Data untouched, so `hasNext()` and
  `isFirst()` answered wrongly.
* **Native sql results went missing by the case of the column labels.** H2 and Oracle hand them
  back upper case, PostgreSQL lower case, and the bean and map mappings matched them exactly.
  Matching ignores the case and the underscores now.
* **Table aliases leaked across statements, and across requests with them.** They lived in a thread
  local, so a statement that was built but never executed left its aliases on that thread, and the
  next request on a pooled thread saw them. Aliases are resolved against the model of the statement
  being built now, which makes them last exactly as long as that statement, and lets an outer query
  and its subquery tell their tables apart even when both query the same entity.
* **A subquery of a delete or an update could not reach the entity being changed.** Its model was
  built alone rather than behind the model of the statement, so a condition such as
  `eq(Stock::getProductId, Product::getId)` compared the subquery table against itself. A
  `not exists` written that way was always true, which emptied the table.
* **A single selected column was instantiated rather than returned.** Selecting one column into a
  scalar type went through `multiselect`, which made Hibernate look for a constructor.
* **`rowCount()` of a native pagination truncated to `int`.**
* `PropertyUtils` threw an exception type borrowed from Hibernate.

### Changed

* An on condition is evaluated after its join is established, so it may refer to the table being
  joined: `leftJoin(Order::getOrderProducts, "op", Restrictions.gt("op", "amount", 10))`.
* `TableAlias` is no longer used by the library itself. The class stays for whoever passes an alias
  around on their own.
* The Maven Wrapper is the official one again (3.3.4, script only, Maven 3.9.12) and points at
  Maven Central. The one shipped before was a Takari 0.5.6 wrapper pointing at a private repository
  on `127.0.0.1:8081`, which no one outside could use, and `mvnw` was not even executable.
* Documentation: 22 worked examples with the sql each of them produces, a Best Practice of 13
  sections, a table of what every provider reaches, and how to run the tests.

### Removed

* `junit-platform-console-standalone:1.10.0`, pinned in the pom and clashing with the JUnit
  platform Spring Boot manages, which had kept `mvn test` from running at all.
* `GroupCountUtils` and `CountUtils`, whose work the provider abstraction took over.

### Requires

Spring Boot 3.1 or later. Spring Boot 3.0 ships Hibernate 6.1, which neither joins a derived table
by an on condition nor renders the parts of a date, and leaves a few functions of Jakarta
Persistence 3.1 unimplemented.

### Tested

202 tests, run against Spring Boot 3.1, 3.2, 3.3, 3.4 and 3.5, and against every combination
below:

| Provider | H2 | PostgreSQL | MySQL |
| --- | --- | --- | --- |
| Hibernate | 202 | 202 | 202 |
| Criteria API alone | 202, 6 skipped | 202, 6 skipped | 202, 6 skipped |
| EclipseLink | 202, 35 skipped | 202, 35 skipped | 202, 35 skipped |

A skipped test is one the provider does not reach, and each is skipped by the capability it needs
rather than by its name.

## [1.0.0-RC1]

The first release.
