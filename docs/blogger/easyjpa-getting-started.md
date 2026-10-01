# EasyJPA: Criteria API Power in Three Lines, Not Thirty

## 1. Overview

**EasyJPA — Criteria API power. Three lines, not thirty.**

EasyJPA is a Spring Boot starter that puts a fluent, lambda-driven API over the JPA Criteria API.
Every condition is a method reference, every join has a name, and not a single JPQL string is
written. Joins, subqueries, grouping, pagination, updates and native SQL all compose in one chain
you read top to bottom.

Here is the entire pitch:

```java
// ── The Criteria API ──────────────────────────────────────────────────────────
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<User> cq = cb.createQuery(User.class);
Root<User> root = cq.from(User.class);
List<Predicate> predicates = new ArrayList<>();
predicates.add(cb.equal(root.get("username"), "Jack"));
predicates.add(cb.equal(root.get("password"), "123456"));
cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
User user = em.createQuery(cq).getSingleResult();

// ── EasyJPA ───────────────────────────────────────────────────────────────────
User user = userDao.query()
        .filter(new FilterList().eq(User::getUsername, "Jack").eq(User::getPassword, "123456"))
        .selectThis().one();
```

Same query, same type safety, same generated SQL.

## 2. What Problem Does It Solve?

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

| | |
| --- | --- |
| **Keeps** | Criteria's type safety and dynamic composition |
| **Drops** | `CriteriaBuilder` / `Root` / `Predicate[]` boilerplate |
| **Fixes** | pagination counts that drift, and group counts that count rows |

## 3. Quick Start

**Install** — the current version is `2.0.0-SNAPSHOT`, in the Central Portal snapshot repository,
which Maven reads from no project that has not named it:

```xml
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

**Name the provider:**

```java
@EntityScan(basePackages = {"com.example.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class,
        basePackages = {"com.example.dao"})
@Configuration(proxyBeanMethods = false)
public class JpaConfig {
}
```

**Extend `EntityDao`:**

```java
public interface UserDao extends EntityDao<User, Long> {
}
```

`EntityDao` **is** a `JpaRepositoryImplementation`, so `save`, `findById` and `deleteAll` are
untouched. Query:

```java
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

That is the whole setup. No properties to add, nothing to tune.

## 4. Requirements

| | Minimum | Notes |
| --- | --- | --- |
| Java | 17 | |
| Spring Boot | 4.0 | Spring Boot 3.1 – 3.5 takes the `1.0.x` line |
| Jakarta Persistence | 3.2 | |
| JPA provider | Hibernate 7.4 | or EclipseLink 5.0.1, or any Criteria implementation |
| Build | Maven 3.9 | |

The version line follows the Spring Boot line, and the two are maintained apart:

| Your Spring Boot | Use |
| --- | --- |
| 4.0 and later | **`2.0.x`, this one** |
| 3.1 – 3.5 | `1.0.x` |
| 3.0 and earlier | not supported |

## 5. How It Works

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

| Mechanic | In one line |
| --- | --- |
| **Alias resolution** | A lambda names an *entity*, not a table; the alias is looked up inside the statement being built and never outside it — so an outer query and its subquery keep their tables apart |
| **Two statements, one definition** | `JpaPageResultSet` holds the listing query and derives the counting query from it, so they cannot drift |
| **Providers are asked, not assumed** | Every optional feature is a `boolean` on `JpaProvider`, readable at runtime |

And the pagination split, which is the part worth seeing:

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

## 6. Code Examples

Every SQL block below is what Hibernate actually emitted, captured from the test suite against H2.
The model:

```
User ──< Order ──< OrderProduct >── Product        User.vip, Order.status, Order.totalPrice
                                        │          Product.name/price/location
                                      Stock        Stock.amount
```

### Example 1 — Nested conditions

**Input** — `vip or (username in ('Scott','Lee') and email is not null)`, by username.

**Execution**

```java
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true)
                .or(new FilterList().in(User::getUsername, List.of("Scott", "Lee"))
                        .and().notNull(User::getEmail)))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

**Output**

```sql
select u1_0.id, u1_0.email, u1_0.password, u1_0.username, u1_0.vip
from example_user u1_0
where u1_0.vip = ? or u1_0.username in (?, ?) and u1_0.email is not null
order by u1_0.username
```

### Example 2 — Four tables, two branches

**Input** — order lines with their order, customer and product; only discounted products on paid or
shipped orders; newest first.

**Execution**

```java
orderProductDao.customPage()
        .join(OrderProduct::getOrder, "o", null)      // OrderProduct → Order
        .join(Order::getUser, "u", null)              // Order        → User
        .join(OrderProduct::getProduct, "p", null)    // OrderProduct → Product, second branch
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

**Output**

```sql
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

**Input** — best sellers: units sold, distinct orders and turnover per product, keeping products
sold more than once. Then the total, for the page navigation.

**Execution**

```java
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

**Output — the listing query**

```sql
select p1_0.name, sum(op1_0.amount), count(distinct op1_0.order_id),
       sum((p1_0.price * op1_0.amount))
from example_order_product op1_0
join example_product p1_0 on p1_0.id = op1_0.product_id
group by p1_0.name having count(op1_0.id) > ?
order by 2 desc
offset ? rows
```

**Output — the counting query, derived from the same definition**

```sql
select count(1) from (
    select 1 from example_order_product op1_0
    join example_product p1_0 on p1_0.id = op1_0.product_id
    group by p1_0.name having count(op1_0.id) > ?
) derived1_0(c)
```

One statement, `having` honoured, no groups fetched. This is the bug you do not have to find.

### Example 4 — A correlated subquery

**Input** — the users who have ever ordered.

**Execution**

```java
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
        .filter(Restrictions.eq(Order::getUser, User::getId))
        .select(Order::getId);

List<User> customers = query.filter(Restrictions.exists(subQuery)).selectThis().list();
```

**Output**

```sql
select u1_0.id, u1_0.email, u1_0.password, u1_0.username, u1_0.vip
from example_user u1_0
where exists (select o1_0.id from example_order o1_0 where o1_0.user_id = u1_0.id)
```

The subquery is created **from** the outer query, which is what correlates the two. Negate it with
`.not()` for `not exists`; swap `exists` for `in` and it becomes an `in` subquery.

### Example 5 — Joining an aggregate as a derived table

**Input** — every product with its sales, without a correlated subquery per row.

**Execution**

```java
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

**Output**

```sql
select p1_0.name, p1_0.price, s1_0.soldAmount, s1_0.orderAmount
from example_product p1_0
join (select op1_0.product_id c0, sum(op1_0.amount) c1, count(op1_0.id) c2
      from example_order_product op1_0
      group by c0) s1_0(productId, soldAmount, orderAmount)
  on s1_0.productId = p1_0.id
order by 3 desc
```

### Example 6 — Update by an expression, delete by a subquery

**Input** — shout a username; decrement a stock; drop the users who never ordered.

**Execution**

```java
userDao.update().setField(User::getUsername, Fields.concat(Fields.upper(User::getUsername), "!"))
       .filter(Restrictions.eq(User::getUsername, "Lee")).execute();

stockDao.update().setField(Stock::getAmount, Fields.minusValue(Stock::getAmount, 1L))
        .filter(Restrictions.gt(Stock::getAmount, 0L)).execute();

JpaDelete<User> delete = userDao.delete();
JpaSubQuery<Order, Order> subQuery = delete.subQuery(Order.class)
        .filter(Restrictions.eq(Order::getUser, User::getId));
delete.filter(Restrictions.exists(subQuery).not()).execute();
```

**Output**

```sql
update example_user  u1_0 set username = (upper(u1_0.username)||?) where u1_0.username = ?
update example_stock s1_0 set amount = (s1_0.amount - cast(? as bigint)) where s1_0.amount > ?

delete from example_user u1_0
where not exists (select o1_0.id from example_order o1_0 where o1_0.user_id = u1_0.id)
```

### The rest of the surface, in one table

| You want | Call |
| --- | --- |
| The entities | `dao.query()` |
| Columns into a VO | `dao.query(Vo.class)` |
| Any columns | `dao.customQuery()` → `Tuple` |
| …with a total count | `dao.page()` · `dao.customPage()` → `JpaPageResultSet` |
| Fetch an association | `fetch(Order::getUser)` · `leftFetch(User::getOrders).distinct()` |
| `CASE WHEN` | `new IfExpression<>(attr).when(a, b).otherwise(c)` |
| Date parts | `Fields.year/month/day(...)` |
| Any database function | `Function.build("LOWER", String.class, Product::getName)` |
| A result shape | `Transformers.asBean/asMap/asCaseInsensitiveMap/asList/noop` |
| Native SQL | `dao.queryForMap(sql, args)` — keeps `rowCount()` and `paginate()` |

## 7. Configuration

EasyJPA has **no properties of its own** — no `spring.easyjpa.*` namespace. What you configure is
the provider and, for three databases, the driver.

| What | Where | Value |
| --- | --- | --- |
| Provider | `@EnableJpaRepositories(repositoryFactoryBeanClass = …)` | `HibernateEntityDaoFactoryBean` (default) · `EclipseLinkEntityDaoFactoryBean` · `StandardEntityDaoFactoryBean` |
| EclipseLink | `spring.autoconfigure.exclude` | `…orm.jpa.HibernateJpaAutoConfiguration` |
| SQL Server | JDBC url | `calcBigDecimalPrecision=true` — otherwise every `BigDecimal` binds as `decimal(38,0)` and `0.90` reads back as `1` |
| SQLite | entity id | `GenerationType.IDENTITY` — `AUTO` needs a sequence table SQLite locks against |
| Oracle 23+ on Hibernate | `OracleDialect` subclass | `DatabaseVersion.make(21)` |

## 8. Comparison

Feature-level. No timings are claimed — nothing here is a benchmark.

| | EasyJPA | Criteria API | Spring Data `Specification` | QueryDSL |
| --- | --- | --- | --- | --- |
| Type-safe attributes | method references | metamodel or strings | metamodel or strings | generated `Q` classes |
| Code generation step | none | none | none | annotation processor |
| Dynamic composition | ✅ | manual `Predicate[]` | ✅ | ✅ |
| Multi-branch joins | one call each | track `Join<?,?>` by hand | manual | ✅ |
| Correlated subqueries | built from the outer query | manual | awkward | ✅ |
| Pagination count over `group by` | derived table, one statement | write it yourself | write it yourself | write it yourself |
| Derived-table join | `joinSubQuery(...)` | manual | ❌ | limited |
| Native SQL, pagination kept | ✅ | ❌ | ❌ | separate API |
| Provider gaps discoverable at runtime | `JpaProvider` flags | ❌ | ❌ | ❌ |

### Provider and database coverage

The same 202 tests run on three providers against six databases.

| | H2 2.3 | PostgreSQL 16 | MySQL 9.6 | SQL Server 16 | SQLite 3.49 | Oracle 23 |
| --- | --- | --- | --- | --- | --- | --- |
| Hibernate | ✅ | ✅ | ✅ | ⚠️ 1 | ⚠️ 3 | ⚠️ 4 |
| Criteria API alone | ✅ | ✅ | ✅ | ⚠️ 1 | ⚠️ 3 | ✅ |
| EclipseLink | ✅ | ✅ | ✅ | ✅ | ❌ no platform | ⚠️ 2 |

Each ⚠️ belongs to the database or to the provider on that database, never to a query EasyJPA built.
The README names every one of them.

## 9. Limitations & Trade-offs

- **Aliases are strings.** A typo in `"op"` is a runtime error, not a compile error.
- **Not auto-configured.** You name the factory bean yourself; there is no `@AutoConfiguration`.
- **The Criteria API is the ceiling.** Window functions, CTEs and `UNION` need native SQL.
- **EclipseLink reaches less than Hibernate.** No derived table, no right join, no subquery as a
  selected column — all of it reported through `JpaProvider` rather than failed over silently.
- **A fetched collection paginates in memory.** Keep paginated fetches to to-one associations.
- **No reactive support.** Blocking JDBC only.
- **Not a good fit** for a project that writes mostly static queries — Spring Data method names are
  shorter for those.

## 10. Summary

1. **One chain instead of ten lines.** `query().filter().sort().select().list()` replaces the whole
   `CriteriaBuilder` / `Root` / `Predicate[]` ritual.
2. **Method references throughout**, so a renamed field is a compile error rather than a runtime
   surprise. No code generation, no annotation processor.
3. **Joins branch by themselves.** A lambda carries its own entity, so a four-table, two-branch join
   is four calls that read in order.
4. **A pagination is one definition, two statements.** The counting query is derived from the
   listing query and cannot drift from it.
5. **A grouping pagination counts groups**, through a derived table, in one statement, `having`
   included — the count most hand-written pagination gets wrong.
6. **Subqueries correlate themselves** because they are created from the query that uses them.
   Filter, column, comparison or nested — all the same way.
7. **Aggregates join once**, as a derived table, instead of running a correlated subquery per row.
8. **Provider gaps are a `boolean`, not a stack trace.** `JpaProviders.getProvider().supportsXxx()`
   answers before you build.
9. **Tested where it matters** — 202 tests, three providers, six databases, and every documented
   failure attributed to the database or the provider rather than hidden.
10. **Zero configuration.** No properties; name one factory bean and extend `EntityDao`.

**GitHub** — [paganini2008/easyjpa](https://github.com/paganini2008/easyjpa), MIT licensed.
Spring Boot 4 takes the `2.0.x` line, Spring Boot 3 the `1.0.x` line.

<!-- Suggested tags: java, springboot, jpa, hibernate, database -->
