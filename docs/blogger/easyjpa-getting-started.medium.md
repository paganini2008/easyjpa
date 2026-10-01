# EasyJPA: Criteria API Power in Three Lines, Not Thirty

> **EasyJPA — Criteria API power. Three lines, not thirty.**

EasyJPA is a Spring Boot starter that puts a fluent, lambda-driven API over the JPA Criteria API.
Every condition is a method reference, every join has a name, and not a single JPQL string is
written. Joins, subqueries, grouping, pagination, updates and native SQL all compose in one chain
you read top to bottom.

Here is the entire pitch.

The Criteria API:

```
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<User> cq = cb.createQuery(User.class);
Root<User> root = cq.from(User.class);
List<Predicate> predicates = new ArrayList<>();
predicates.add(cb.equal(root.get("username"), "Jack"));
predicates.add(cb.equal(root.get("password"), "123456"));
cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
User user = em.createQuery(cq).getSingleResult();
```

EasyJPA:

```
User user = userDao.query()
        .filter(new FilterList().eq(User::getUsername, "Jack").eq(User::getPassword, "123456"))
        .selectThis().one();
```

Same query, same type safety, same generated SQL.

---

## What Problem Does It Solve?

The Criteria API is the only type-safe way to build a dynamic JPA query, and almost nobody enjoys
using it. A three-condition filter costs ten lines of `CriteriaBuilder` plumbing; a multi-branch
join means tracking `Join<?, ?>` variables by hand; a correlated subquery is close to unreadable.
So teams fall back on JPQL strings or Spring Data method names — and lose type safety, or
composability, or both.

The second problem is quieter and costs more. A paginated query is really *two* statements, the
listing one and the counting one, and they are written separately. They drift. And when the query
has a `group by`, the obvious `count(*)` counts rows rather than groups, so the total is simply
wrong. EasyJPA derives both statements from one definition, and counts groups through a derived
table.

**It keeps** Criteria's type safety and dynamic composition.

**It drops** the `CriteriaBuilder` / `Root` / `Predicate[]` boilerplate.

**It fixes** pagination counts that drift, and group counts that count rows.

---

## Quick Start

The current version is `2.0.0-SNAPSHOT`, in the Central Portal snapshot repository, which Maven
reads from no project that has not named it:

```
<dependency>
    <groupId>com.github.paganini2008</groupId>
    <artifactId>easyjpa-spring-boot-starter</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>

<repositories>
    <repository>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases><enabled>false</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>
```

Name the provider:

```
@EntityScan(basePackages = {"com.example.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class,
        basePackages = {"com.example.dao"})
@Configuration(proxyBeanMethods = false)
public class JpaConfig {
}
```

Extend `EntityDao`:

```
public interface UserDao extends EntityDao<User, Long> {
}
```

`EntityDao` **is** a `JpaRepositoryImplementation`, so `save`, `findById` and `deleteAll` are
untouched. Query:

```
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

That is the whole setup. No properties to add, nothing to tune.

---

## Requirements

**Java** — 17 or later.

**Spring Boot** — 4.0 or later. Spring Boot 3.1 to 3.5 takes the `1.0.x` line instead.

**Jakarta Persistence** — 3.2.

**JPA provider** — Hibernate 7.4, or EclipseLink 5.0.1, or any Criteria API implementation.

**Build** — Maven 3.9 or later.

The version line follows the Spring Boot line, and the two are maintained apart. Spring Boot 4.0 and
later takes the `2.0.x` line, which is the one described here; Spring Boot 3.1 to 3.5 takes `1.0.x`;
Spring Boot 3.0 and earlier is not supported.

---

## How It Works

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  YOUR CODE        userDao.query().filter(...).sort(...).selectThis().list()  │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │  method references + aliases
┌──────────────────────────────────▼──────────────────────────────────────────┐
│  EASYJPA          Model ─────────── resolves an alias to a table            │
│                   Filter · Field · Column · JpaSort                         │
│                   JpaPageResultSet ─ one definition, two statements         │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │  "may I use a derived table here?"
┌──────────────────────────────────▼──────────────────────────────────────────┐
│  JpaProvider        Hibernate   │   EclipseLink   │   Standard              │
│                     supportsDerivedTable() · supportsRightJoin() · …        │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼──────────────────────────────────────────┐
│  JAKARTA PERSISTENCE   CriteriaBuilder · CriteriaQuery · Root · Predicate    │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼──────────────────────────────────────────┐
│  DATABASE    H2 · PostgreSQL · MySQL · SQL Server · SQLite · Oracle         │
└─────────────────────────────────────────────────────────────────────────────┘
```

Three mechanics are worth knowing, and none of them needs configuring.

**Alias resolution.** A lambda names an *entity*, not a table. The alias is looked up inside the
statement being built and never outside it — so an outer query and its subquery keep their tables
apart even when both query the same entity.

**Two statements, one definition.** `JpaPageResultSet` holds the listing query and derives the
counting query from it, so the two cannot drift.

**Providers are asked, not assumed.** Every optional feature is a `boolean` on `JpaProvider`,
readable at runtime.

The pagination split is the part worth seeing:

```
      customPage().join(...).filter(...).groupBy(...).select(...)
                              one definition
                                    │
                 ┌──────────────────┴──────────────────┐
                 ▼                                     ▼
            list(10, 0)                           rowCount()
                 │                                     │
   select … offset ? rows                   no group by → select count(1) from (…) d
          fetch first ? rows only              group by → select count(1) from
                                                          ( <the groups> ) d
```

---

## Code Examples

Every SQL block below is what Hibernate actually emitted, captured from the test suite against H2.
The model:

```
User ──< Order ──< OrderProduct >── Product        User.vip, Order.status, Order.totalPrice
                                        │          Product.name/price/location
                                      Stock        Stock.amount
```

### Example 1 — Nested conditions

**Input:** `vip or (username in ('Scott','Lee') and email is not null)`, by username.

**Execution:**

```
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true)
                .or(new FilterList().in(User::getUsername, List.of("Scott", "Lee"))
                        .and().notNull(User::getEmail)))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

**Output:**

```
select u1_0.id, u1_0.email, u1_0.password, u1_0.username, u1_0.vip
from example_user u1_0
where u1_0.vip = ? or u1_0.username in (?, ?) and u1_0.email is not null
order by u1_0.username
```

### Example 2 — Four tables, two branches

**Input:** order lines with their order, customer and product; only discounted products on paid or
shipped orders; newest first.

**Execution:**

```
orderProductDao.customPage()
        .join(OrderProduct::getOrder, "o", null)      // OrderProduct -> Order
        .join(Order::getUser, "u", null)              // Order        -> User
        .join(OrderProduct::getProduct, "p", null)    // OrderProduct -> Product, second branch
        .filter(new FilterList().notNull(Product::getDiscount).and()
                .in(Property.forName("o", "status"), List.of(OrderStatus.PAID, OrderStatus.SHIPPED)))
        .sort(JpaSort.desc(Order::getOrderDate), JpaSort.asc(Product::getName))
        .select(new ColumnList().addColumns(User::getUsername)
                .addColumns(Order::getId, Order::getOrderDate, Order::getStatus)
                .addColumns(Product::getName, Product::getPrice)
                .addColumns(OrderProduct::getAmount)
                .addColumns(Fields.multiply(Property.forName(Product::getPrice),
                        Property.forName(OrderProduct::getAmount)).as("subtotal")));
```

**Output:**

```
select u1_0.username, o1_0.id, o1_0.order_date, o1_0.status,
       p1_0.name, p1_0.price, op1_0.amount, (p1_0.price * op1_0.amount)
from example_order_product op1_0
join example_order   o1_0 on o1_0.id = op1_0.order_id
join example_user    u1_0 on u1_0.id = o1_0.user_id
join example_product p1_0 on p1_0.id = op1_0.product_id
where p1_0.discount is not null and o1_0.status in (?, ?)
order by o1_0.order_date desc, p1_0.name
offset ? rows fetch first ? rows only
```

The third join branches back off `OrderProduct` rather than continuing from `User`: the lambda
carries its own entity, so the tree comes out the way it reads.

### Example 3 — A grouping pagination that counts groups

**Input:** best sellers — units sold, distinct orders and turnover per product, keeping products
sold more than once. Then the total, for the page navigation.

**Execution:**

```
JpaPageResultSet<Tuple> resultSet = orderProductDao.customPage()
        .join(OrderProduct::getProduct, "p", null)
        .groupBy(new FieldList().addFields(Product::getName))
        .having(Restrictions.gt(Fields.count(OrderProduct::getId), 1L))
        .sort(JpaSort.desc(2))
        .select(new ColumnList().addColumns(Product::getName)
                .addColumns(Fields.sum(OrderProduct::getAmount).as("soldAmount"),
                        Fields.countDistinct(Property.forName("this", "order.id")).as("orderAmount"),
                        Fields.sum(Fields.multiply(Property.forName(Product::getPrice),
                                Property.forName(OrderProduct::getAmount))).as("turnover")));

long groups = resultSet.rowCount();
List<SalesVo> rows = resultSet.setTransformer(Transformers.asBean(SalesVo.class)).list();
```

**Output — the listing query:**

```
select p1_0.name, sum(op1_0.amount), count(distinct op1_0.order_id),
       sum((p1_0.price * op1_0.amount))
from example_order_product op1_0
join example_product p1_0 on p1_0.id = op1_0.product_id
group by p1_0.name having count(op1_0.id) > ?
order by 2 desc
offset ? rows
```

**Output — the counting query, derived from the same definition:**

```
select count(1) from (
    select 1 from example_order_product op1_0
    join example_product p1_0 on p1_0.id = op1_0.product_id
    group by p1_0.name having count(op1_0.id) > ?
) derived1_0(c)
```

One statement, `having` honoured, no groups fetched. This is the bug you do not have to find.

### Example 4 — A correlated subquery

**Input:** the users who have ever ordered.

**Execution:**

```
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
        .filter(Restrictions.eq(Order::getUser, User::getId))
        .select(Order::getId);

List<User> customers = query.filter(Restrictions.exists(subQuery)).selectThis().list();
```

**Output:**

```
select u1_0.id, u1_0.email, u1_0.password, u1_0.username, u1_0.vip
from example_user u1_0
where exists (select o1_0.id from example_order o1_0 where o1_0.user_id = u1_0.id)
```

The subquery is created **from** the outer query, which is what correlates the two. Negate it with
`.not()` for `not exists`; swap `exists` for `in` and it becomes an `in` subquery.

### Example 5 — Joining an aggregate as a derived table

**Input:** every product with its sales, without a correlated subquery per row.

**Execution:**

```
JpaQuery<Product, Tuple> query = productDao.customQuery();
JpaSubQuery<OrderProduct, Tuple> sales = query.subQuery(OrderProduct.class, "op", Tuple.class);
sales.groupBy(new FieldList().addFields(Property.forName("op", "product.id")))
     .select(new ColumnList()
             .addColumns(Property.forName("op", "product.id").as("productId"))
             .addColumns(Fields.sum("op", "amount", Integer.class).as("soldAmount"),
                         Fields.count("op", "id").as("orderAmount")));

query.joinSubQuery(sales, "s", Restrictions.eq(Property.forName("s", "productId"),
                                               Property.forName("this", "id")))
     .sort(JpaSort.desc(Property.forName("s", "soldAmount")))
     .select(new ColumnList().addColumns(Product::getName, Product::getPrice)
             .addColumns(Property.forName("s", "soldAmount").as("soldAmount")))
     .setTransformer(Transformers.asCaseInsensitiveMap()).list();
```

**Output:**

```
select p1_0.name, p1_0.price, s1_0.soldAmount, s1_0.orderAmount
from example_product p1_0
join (select op1_0.product_id c0, sum(op1_0.amount) c1, count(op1_0.id) c2
      from example_order_product op1_0
      group by c0) s1_0(productId, soldAmount, orderAmount)
  on s1_0.productId = p1_0.id
order by 3 desc
```

### Example 6 — Update by an expression, delete by a subquery

**Input:** shout a username; decrement a stock; drop the users who never ordered.

**Execution:**

```
userDao.update().setField(User::getUsername, Fields.concat(Fields.upper(User::getUsername), "!"))
       .filter(Restrictions.eq(User::getUsername, "Lee")).execute();

stockDao.update().setField(Stock::getAmount, Fields.minusValue(Stock::getAmount, 1L))
        .filter(Restrictions.gt(Stock::getAmount, 0L)).execute();

JpaDelete<User> delete = userDao.delete();
JpaSubQuery<Order, Order> subQuery = delete.subQuery(Order.class)
        .filter(Restrictions.eq(Order::getUser, User::getId));
delete.filter(Restrictions.exists(subQuery).not()).execute();
```

**Output:**

```
update example_user  u1_0 set username = (upper(u1_0.username)||?) where u1_0.username = ?
update example_stock s1_0 set amount = (s1_0.amount - cast(? as bigint)) where s1_0.amount > ?

delete from example_user u1_0
where not exists (select o1_0.id from example_order o1_0 where o1_0.user_id = u1_0.id)
```

### The rest of the surface

**The entities** — `dao.query()`

**Columns into a VO** — `dao.query(Vo.class)`

**Any columns** — `dao.customQuery()`, returning a `Tuple`

**The same, with a total count** — `dao.page()` or `dao.customPage()`, returning a
`JpaPageResultSet`

**Fetch an association** — `fetch(Order::getUser)`, or `leftFetch(User::getOrders).distinct()` for a
collection

**CASE WHEN** — `new IfExpression<>(attr).when(a, b).otherwise(c)`

**Date parts** — `Fields.year(...)`, `Fields.month(...)`, `Fields.day(...)`

**Any database function** — `Function.build("LOWER", String.class, Product::getName)`

**A result shape** — `Transformers.asBean(...)`, `asMap()`, `asCaseInsensitiveMap()`, `asList()`,
`noop()`

**Native SQL** — `dao.queryForMap(sql, args)`, which keeps `rowCount()` and `paginate()`

---

## Configuration

EasyJPA has **no properties of its own** — there is no `spring.easyjpa.*` namespace. What you
configure is the provider and, for three databases, the driver.

**The provider** goes on `@EnableJpaRepositories(repositoryFactoryBeanClass = …)`:
`HibernateEntityDaoFactoryBean` is the default, `EclipseLinkEntityDaoFactoryBean` and
`StandardEntityDaoFactoryBean` are the alternatives.

**On EclipseLink**, add
`spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration`,
since Spring Boot autoconfigures Hibernate alone, and build the entity manager yourself.

**On SQL Server**, put `calcBigDecimalPrecision=true` in the JDBC url. Without it the driver binds
every `BigDecimal` as `decimal(38,0)`, a `coalesce` against such a parameter drops the decimals, and
`0.90` reads back as `1`.

**On SQLite**, map entity ids as `GenerationType.IDENTITY`. `AUTO` falls back to a sequence table
that Hibernate writes from a transaction of its own, which SQLite locks against.

**On Oracle 23 and later with Hibernate**, subclass `OracleDialect` with
`DatabaseVersion.make(21)`.

---

## Comparison

Feature-level. No timings are claimed — nothing here is a benchmark.

**Type-safe attributes.** EasyJPA uses method references. The raw Criteria API and Spring Data
`Specification` use the metamodel or plain strings. QueryDSL uses generated `Q` classes.

**Code generation.** Only QueryDSL needs an annotation processor and a generated-sources step.
EasyJPA, Criteria and `Specification` need none.

**Dynamic composition.** EasyJPA, `Specification` and QueryDSL all compose. The raw Criteria API
means assembling a `Predicate[]` by hand.

**Multi-branch joins.** One call each in EasyJPA and QueryDSL. By hand in Criteria and
`Specification`, tracking `Join<?, ?>` variables yourself.

**Correlated subqueries.** EasyJPA builds them from the outer query, so they correlate themselves.
QueryDSL handles them well. Criteria is manual; `Specification` is awkward.

**Pagination count over a `group by`.** EasyJPA wraps the grouping in a derived table and counts it
in one statement. Everywhere else you write that query yourself.

**Derived-table join.** `joinSubQuery(...)` in EasyJPA. Manual in Criteria, unavailable in
`Specification`, limited in QueryDSL.

**Native SQL with pagination kept.** EasyJPA only; QueryDSL has a separate API for it.

**Provider gaps discoverable at runtime.** EasyJPA only, through `JpaProvider` flags.

### What EclipseLink does not reach

Hibernate is the default and reaches every feature. On EclipseLink these are reported through
`JpaProvider` rather than failed over silently:

A subquery as a selected column — `supportsSubQueryAsSelection()`.
Joining a subquery as a derived table — `supportsDerivedTable()`.
A right join — `supportsRightJoin()`.
Selecting an entity column by column — `supportsPartialEntity()`; take a `Tuple` or a bean instead.
Filling a bean by its properties — `supportsBeanProjection()`; its constructor is used instead.
Sorting by a column position — `supportsOrdinalSort()`.
Passing a database function through — `supportsPassThroughFunction()`.
`year()`, `month()`, `day()` — `supportsDatePart()`; the Criteria API defines none.

A subquery as one side of a comparison works, but has to be quantified by `Fields.all/any/some`.
Pagination counting falls back from a derived table to `count(distinct …)`.

### Database coverage

The same 202 tests run on three providers against six databases.

**H2 2.3.232, PostgreSQL 16.13 and MySQL 9.6.0** — everything passes, on all three providers.

**SQL Server 16.00.4265** — one failure on Hibernate and on the plain Criteria API: `+` is read by
its first operand, so a `max(price)` asked for as text is taken for arithmetic. Cast it first.
EclipseLink passes.

**SQLite 3.49.1** — three failures: no quantified subquery, no `repeat`, and no date type.
EclipseLink ships no SQLite platform at all.

**Oracle 23.26.3** — four derived-table failures on Hibernate 7.4, which writes `group by c0`, its
internal alias rather than the select item's; the same tests pass once the dialect is told it is
Oracle 21. Two paginated-`Tuple` failures on EclipseLink, which writes no aliases and then wraps as
`select a.* from (…) a`, and Oracle rejects two columns of the same name. The plain Criteria API
passes.

Every one of those belongs to the database or to the provider on that database, never to a query
EasyJPA built.

---

## Limitations & Trade-offs

Aliases are strings — a typo in `"op"` is a runtime error, not a compile error.

It is not auto-configured. You name the factory bean yourself; there is no `@AutoConfiguration`.

The Criteria API is the ceiling. Window functions, CTEs and `UNION` need native SQL.

EclipseLink reaches less than Hibernate, as listed above.

A fetched collection paginates in memory, so keep paginated fetches to to-one associations.

No reactive support — blocking JDBC only.

It is not a good fit for a project that writes mostly static queries. Spring Data method names are
shorter for those.

---

## Summary

**One chain instead of ten lines.** `query().filter().sort().select().list()` replaces the whole
`CriteriaBuilder` / `Root` / `Predicate[]` ritual.

**Method references throughout**, so a renamed field is a compile error rather than a runtime
surprise. No code generation, no annotation processor.

**Joins branch by themselves.** A lambda carries its own entity, so a four-table, two-branch join is
four calls that read in order.

**A pagination is one definition, two statements.** The counting query is derived from the listing
query and cannot drift from it.

**A grouping pagination counts groups**, through a derived table, in one statement, `having`
included — the count most hand-written pagination gets wrong.

**Subqueries correlate themselves** because they are created from the query that uses them. Filter,
column, comparison or nested — all the same way.

**Aggregates join once**, as a derived table, instead of running a correlated subquery per row.

**Provider gaps are a `boolean`, not a stack trace.** `JpaProviders.getProvider().supportsXxx()`
answers before you build.

**Tested where it matters** — 202 tests, three providers, six databases, and every documented
failure attributed to the database or the provider rather than hidden.

**Zero configuration.** No properties; name one factory bean and extend `EntityDao`.

---

GitHub: [paganini2008/easyjpa](https://github.com/paganini2008/easyjpa), MIT licensed. Spring Boot 4
takes the `2.0.x` line, Spring Boot 3 the `1.0.x` line.

<!-- Medium version: no tables, no nested lists. Suggested tags: Java, Spring Boot, JPA, Hibernate, Database -->
