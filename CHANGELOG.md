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

* **Counting the groups of a group by over a key that is not text failed on SQL Server.** Where
  the count cannot go through a derived table, several group-by keys are concatenated into one and
  counted distinctly, and the text of a non-text key was asked for by `Expression.as(String.class)`.
  That casts in the type system of Java alone: Hibernate writes no SQL `cast` for it, so the column
  reached the concatenation as it stood, which every other database widened to text and SQL Server
  refused. Turning an expression into text is a question for the provider now, `JpaProvider.asText`,
  and the Criteria API answers it by the cast function every provider knows, so a date key is
  written as `cast(produce_date as varchar)` rather than as the bare column. It cost `rowCount()`
  of a paginated group-by query whose keys included a number or a date.
* **Five tests sorted by a column position without asking whether the provider reaches it.**
  `supportsOrdinalSort()` says EclipseLink does not, and four of the five ran on it anyway. They
  passed, because EclipseLink binds the position as a parameter and most databases order by such a
  constant without complaint and without sorting; SQL Server refuses it outright, which is what
  brought it to light. They ask now, as the rest of the suite does, and EclipseLink skips 39 rather
  than 35.
* **Sorting by the position of a column went through a cast.** `JpaSort.asc(int)` and
  `JpaSort.desc(int)` wrapped the position in `toInteger(literal(n))`; it is a plain literal now,
  which is what a database reads as a position.
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

* **SQL Server, SQLite and Oracle join the databases the suite is run against**, by the profiles
  `sqlserver`, `sqlite` and `oracle`. The whole of every run is in the README, with the version of
  each database and driver, and with what each one refuses and why. Nothing in the library had to
  change for any of them.
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

**Spring Boot 3.1 or later.** The version of this library follows the version of Spring Boot, and
the two lines are maintained apart: **1.0.0 is written for Spring Boot 3, 2.0.0 for Spring Boot 4.**
They are not one jar built twice.

Spring Boot 3.0 ships Hibernate 6.1, which neither joins a derived table by an on condition nor
renders the parts of a date, and leaves a few functions of Jakarta Persistence 3.1 unimplemented,
so 3.1 is where this line starts. On EclipseLink, 4.x, the one Jakarta Persistence 3.1 asks for;
the 2.0.0 line takes 5.0 or later.

### Note for SQL Server, SQLite and Oracle

Section 7a of the README is new and says what each database does not take, next to section 7 and
what each provider does not reach. In short: on SQL Server put `calcBigDecimalPrecision=true` in
the url, or the driver binds every `BigDecimal` as `decimal(38,0)` and a `coalesce` against it
loses its decimals, 0.90 coming back as 1; and sort by naming the column rather than its position
if EclipseLink is a target. On SQLite map the ids as `IDENTITY`, since `GenerationType.AUTO` falls
back to a sequence table written from a transaction of its own, which locks against the one
already open — and use Hibernate or the Criteria API alone, since EclipseLink ships no SQLite
platform. On Oracle 23 and later, Hibernate writes a group by no derived table can be joined on —
it names the select item by an alias it numbered internally rather than by the one the query gave
it — so until Hibernate has that, an `OracleDialect` subclass built with `DatabaseVersion.make(21)`
is the way around it.

### Tested

202 tests, on all three providers and against six databases, all of it on Spring Boot 3.5 and
Hibernate 6.6:

| Provider | H2 | PostgreSQL | MySQL | SQL Server | SQLite | Oracle |
| --- | --- | --- | --- | --- | --- | --- |
| Hibernate | 202 | 202 | 202 | 202, 1 failed | 202, 3 failed | 202, 4 failed |
| Criteria API alone | 202, 6 skipped | 202, 6 skipped | 202, 6 skipped | 202, 6 skipped, 1 failed | 202, 6 skipped, 3 failed | 202, 6 skipped |
| EclipseLink | 202, 39 skipped | 202, 39 skipped | 202, 39 skipped | 202, 39 skipped | not supported | 202, 39 skipped, 2 failed |

Against H2 2.3.232, SQLite 3.49.1, MySQL 9.6.0, PostgreSQL 16.13, SQL Server 2022 (16.00.4265) and
Oracle AI Database 26ai Free (23.26.3.0.0), each on the driver Spring Boot 3.5 manages, and
EclipseLink 4.0.4.

What is skipped is what the provider does not reach, what failed is what the database, or the
provider on that database, does not take, and neither is the library answering wrongly.

## [1.0.0-RC1]

The first release.
