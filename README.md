<div align="center">

# EasyJPA

**Criteria API power. Three lines, not thirty.**

Type-safe dynamic queries for Spring Data JPA — every condition a method reference, every join a
name, not a single JPQL string. Joins, subqueries, grouping, pagination, updates and native SQL all
compose in one chain you read top to bottom.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1+-brightgreen?style=flat-square&logo=springboot)
![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk)
![JPA](https://img.shields.io/badge/Jakarta%20Persistence-3.1-blue?style=flat-square)
![Hibernate](https://img.shields.io/badge/Hibernate%20%7C%20EclipseLink-supported-yellow?style=flat-square&logo=hibernate)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

</div>

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

**Contents** —
[Features](#features) ·
[How It Works](#how-it-works) ·
[Requirements](#requirements) ·
[Quick Start](#quick-start) ·
[Examples](#examples) ·
[Configuration](#configuration) ·
[Compatibility](#compatibility--comparison) ·
[Docs](#documentation) ·
[License](#contributing--license)

## Features

| The problem | What EasyJPA does about it |
| --- | --- |
| Criteria queries take 10 lines before they say anything | One fluent chain — `query().filter().sort().select().list()` |
| String attribute names break silently on rename | Method references throughout; a rename is a compile error |
| Dynamic `Predicate` lists are assembled by hand | `Restrictions` / `FilterList` compose and nest, `.not()` negates |
| Multi-branch joins force you to track `Join<?,?>` variables | `join(Entity::getAttr, "alias", on)` — the lambda knows its own table |
| A paginated count query drifts out of sync with the listing one | `JpaPageResultSet` derives both from one definition |
| `count(*)` over a `group by` counts rows, not groups | `rowCount()` wraps the grouping in a derived table — one statement, `having` included |
| Correlated subqueries are the hardest thing to write in Criteria | `query.subQuery(...)` is built *from* its outer query, so it correlates itself |
| Aggregates per row cost a subquery per row | `joinSubQuery(...)` joins the aggregate once as a derived table |
| Reading an association after the query costs N+1 selects | `fetch()` / `leftFetch()` on the same chain |
| Results need to be entities here, VOs there, maps elsewhere | `Transformers.asBean/asMap/asList/asCaseInsensitiveMap` |
| Whatever Criteria cannot express means dropping the API | `queryForMap(sql, args)` — native SQL that keeps `rowCount()` and `paginate()` |
| Provider gaps surface as stack traces | `JpaProviders.getProvider().supportsXxx()` answers before you build |

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

Three mechanics are worth knowing. Nothing here needs configuring.

| Mechanic | In one line |
| --- | --- |
| **Alias resolution** | A lambda names an *entity*, not a table; the alias is looked up in the statement being built, never outside it — so an outer query and its subquery keep their tables apart |
| **Two statements, one definition** | `JpaPageResultSet` holds the listing query and derives the counting query from it, so they cannot drift |
| **Providers are asked, not assumed** | Every optional feature is a `boolean` on `JpaProvider`, readable at runtime |

**Why a pagination is two statements:**

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

## Requirements

| | Minimum | Notes |
| --- | --- | --- |
| Java | 17 | |
| Spring Boot | 3.1 | 3.0 lacks a derived table joined by an `on` condition, and date parts |
| Jakarta Persistence | 3.1 | 3.2 on the `2.0.x` line |
| Build | Maven 3.9 | or the bundled `./mvnw` |
| JPA provider | Hibernate 6.6 | or EclipseLink 4.0.4, or any Criteria API implementation |

**The version line follows the Spring Boot line.** The two are maintained apart — not one jar built
twice:

| Your Spring Boot | Use | Jakarta Persistence | EclipseLink |
| --- | --- | --- | --- |
| 4.0 and later | `2.0.x` | 3.2 | 5.0+ |
| 3.1 – 3.5 | **`1.0.x`, this one** | 3.1 | 4.x |
| 3.0 and earlier | not supported | | |

## Quick Start

### Installation

The current version is **`1.0.0-SNAPSHOT`**, in the Central Portal snapshot repository. Maven reads
that repository from no project that has not named it, so both blocks are needed:

```xml
<dependency>
    <groupId>com.github.paganini2008</groupId>
    <artifactId>easyjpa-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
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

### Three steps

**1 — name the provider** on `@EnableJpaRepositories`:

```java
@EntityScan(basePackages = {"com.example.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class,
        basePackages = {"com.example.dao"})
@Configuration(proxyBeanMethods = false)
public class JpaConfig {
}
```

**2 — extend `EntityDao`** instead of `JpaRepository`:

```java
public interface UserDao extends EntityDao<User, Long> {
}
```

`EntityDao` **is** a `JpaRepositoryImplementation` — `save`, `findById`, `deleteAll` are all still
there, plus `count(Filter)`, `exists(Filter)`, `max/min/avg/sum(...)`.

**3 — query.**

```java
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true)
                .or(new FilterList().in(User::getUsername, List.of("Scott", "Lee"))
                        .and().notNull(User::getEmail)))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

**Output** — one statement, no `Predicate[]` in sight:

```sql
select u1_0.id, u1_0.email, u1_0.password, u1_0.username, u1_0.vip
from example_user u1_0
where u1_0.vip = ? or u1_0.username in (?, ?) and u1_0.email is not null
order by u1_0.username
```

## Examples

Every SQL block below is what Hibernate actually emitted for the Java above it, captured from the
test suite against H2. The model:

```
User ──< Order ──< OrderProduct >── Product        User.vip, Order.status, Order.totalPrice
                                        │          Product.name/price/location
                                      Stock        Stock.amount
```

### Entry points

| You want | Call | You get |
| --- | --- | --- |
| The entities | `dao.query()` | the entity type |
| Columns mapped to a VO | `dao.query(Vo.class)` | the given type |
| Any columns | `dao.customQuery()` | `Tuple` |
| …with a total count | `dao.page()` · `dao.page(Vo.class)` · `dao.customPage()` | `JpaPageResultSet` |
| An update / delete | `dao.update()` · `dao.delete()` | affected rows |
| Native SQL | `dao.query(sql, args)` · `dao.queryForMap(sql, args)` | `PageableQuery` |

### 1 · Filtering

**Goal** — count by a flag, by nullability, by a set; negate any of them.

```java
userDao.count(Restrictions.eq(User::getVip, true));
userDao.count(Restrictions.ne(User::getVip, true));
userDao.count(Restrictions.isNull(User::getEmail));
userDao.count(Restrictions.in(User::getUsername, List.of("Jack", "Petter")).not());
```

```sql
select count(u1_0.id) from example_user u1_0 where u1_0.vip = ?
select count(u1_0.id) from example_user u1_0 where u1_0.vip <> ?
```

An attribute is addressed three ways, all equivalent:

```java
Restrictions.eq(Product::getName, "Juicer")   // by lambda — the alias is resolved for you
Restrictions.eq("name", "Juicer")             // by attribute name, on the root entity
Restrictions.eq("p", "name", "Juicer")        // by alias + attribute name
```

### 2 · Joining

**Goal** — order lines with their order, customer and product: four tables, two branches.

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

Note the third join branches back off `OrderProduct` rather than continuing from `User` — the lambda
carries its own entity, so the tree comes out the way it reads.

| Join | Call |
| --- | --- |
| Inner | `join(OrderProduct::getProduct, "p", null)` |
| Left / Right | `leftJoin(...)` · `rightJoin(...)` |
| Cross, no association | `crossJoin(Stock.class, "s")` then correlate in `filter(...)` |
| With an `on` condition | `leftJoin(Order::getOrderProducts, "op", Restrictions.gt("op", "amount", 10))` |
| By attribute name | `join("o", "user", "u", null)` |

### 3 · Grouping and aggregation

**Goal** — per user: how many orders, how much in total, the biggest one. Keep users with no order.

```java
List<UserOrderVo> dataList = userDao.customQuery()
        .leftJoin(User::getOrders, "o", null)
        .groupBy(new FieldList(User::getUsername))
        .sort(JpaSort.asc(User::getUsername))
        .select(new ColumnList().addColumns(User::getUsername)
                .addColumns(Fields.count(Order::getId).as("orderAmount"),
                            Fields.sum(Order::getTotalPrice).as("totalPrice"),
                            Fields.max(Order::getTotalPrice).as("maxPrice")))
        .setTransformer(Transformers.asBean(UserOrderVo.class))
        .list();
```

```sql
select u1_0.username, count(o1_0.id), sum(o1_0.total_price), max(o1_0.total_price)
from example_user u1_0
left join example_order o1_0 on u1_0.id = o1_0.user_id
group by u1_0.username
order by u1_0.username
```

The alias you give a computed column (`.as("orderAmount")`) is the VO property — or the map key —
it lands in. `having` filters the groups: `.having(Restrictions.gt(Fields.count(Order::getId), 0L))`.

### 4 · Computed columns

```java
Fields.multiply(Property.forName(Product::getPrice), Property.forName(OrderProduct::getAmount))
Fields.concat(Fields.upper(User::getUsername), "!")
Fields.countDistinct(Property.forName("this", "order.id"))
Fields.month(Order::getOrderDate)                      // rendered per database
Function.build("LOWER", String.class, Product::getName) // any function the database has
```

**Goal** — map a column to a category: `CASE WHEN`.

```java
IfExpression<String, String> area = new IfExpression<String, String>(Product::getLocation)
        .when("China", "Asia").when("Japan", "Asia")
        .when("Australia", "Oceania")
        .otherwise("Other");

productDao.customQuery()
        .select(new ColumnList().addColumns(area.as("area")).addColumns(Product::getLocation))
        .list();
```

```sql
select case p1_0.location when ? then cast(? as varchar)
                          when ? then cast(? as varchar)
                          when ? then cast(? as varchar)
                          else cast(? as varchar) end,
       p1_0.location
from example_product p1_0
```

### 5 · Pagination, counted correctly

**Goal** — best sellers: per product, units sold, distinct orders, turnover. Keep products sold more
than once, heaviest first, and page through them.

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

long groups = resultSet.rowCount();                  // ← counts products, not order lines
List<SalesVo> rows = resultSet.setTransformer(Transformers.asBean(SalesVo.class)).list();
```

**Output — the listing query:**

```sql
select p1_0.name, sum(op1_0.amount), count(distinct op1_0.order_id),
       sum((p1_0.price * op1_0.amount))
from example_order_product op1_0
join example_product p1_0 on p1_0.id = op1_0.product_id
group by p1_0.name having count(op1_0.id) > ?
order by 2 desc
offset ? rows
```

**Output — the counting query, derived from the same definition:**

```sql
select count(1) from (
    select 1 from example_order_product op1_0
    join example_product p1_0 on p1_0.id = op1_0.product_id
    group by p1_0.name having count(op1_0.id) > ?
) derived1_0(c)
```

One statement, `having` honoured, no groups fetched. Page navigation:

```java
PageResponse<Map<String, Object>> page = userDao.customPage()
        .sort(JpaSort.asc(User::getId))
        .select(new ColumnList(User::getId, User::getUsername, User::getVip))
        .setTransformer(Transformers.asCaseInsensitiveMap())
        .paginate(PageRequest.of(2));

page.getTotalRecords();   page.getTotalPages();   page.hasNextPage();
page.nextPage().getContent();   page.lastPage().isLastPage();

resultSet.paginate(PageRequest.of(10))
         .forEachPage(eachPage -> eachPage.getContent().forEach(this::handle));
```

```sql
select u1_0.id, u1_0.username, u1_0.vip from example_user u1_0
order by u1_0.id offset ? rows fetch first ? rows only
```

### 6 · Subqueries

Build the subquery **from the statement that uses it** — that is what correlates the two.

**Goal** — the users who have ever ordered.

```java
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
        .filter(Restrictions.eq(Order::getUser, User::getId))
        .select(Order::getId);

List<User> customers = query.filter(Restrictions.exists(subQuery)).selectThis().list();
```

```sql
select u1_0.id, u1_0.email, u1_0.password, u1_0.username, u1_0.vip
from example_user u1_0
where exists (select o1_0.id from example_order o1_0 where o1_0.user_id = u1_0.id)
```

| Shape | How |
| --- | --- |
| `not exists` | `Restrictions.exists(subQuery).not()` |
| `in` | `Restrictions.in(Property.forName(User::getId), subQuery)` |
| A grouping subquery | `subQuery.groupBy(...).having(...).select(Property.forName("o", "user.id", Long.class))` |
| As a selected column | `Column.forSubQuery(stock, "stockAmount")` |
| Compared against | `Restrictions.gt(Property.forName(Product::getPrice), subQuery)` |
| Nested in another subquery | create both from the same outer query |

Whatever a subquery selects out of an association, spell the key out —
`Property.forName("o", "user.id", Long.class)` rather than `Order::getUser` — since that is what
every provider renders the same way.

### 7 · Joining a derived table

**Goal** — every product with its sales, without a correlated subquery per row.

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
             .addColumns(Property.forName("s", "soldAmount").as("soldAmount"),
                         Property.forName("s", "orderAmount").as("orderAmount")))
     .setTransformer(Transformers.asCaseInsensitiveMap()).list();
```

```sql
select p1_0.name, p1_0.price, s1_0.soldAmount, s1_0.orderAmount
from example_product p1_0
join (select op1_0.product_id c0, sum(op1_0.amount) c1, count(op1_0.id) c2
      from example_order_product op1_0
      group by c0) s1_0(productId, soldAmount, orderAmount)
  on s1_0.productId = p1_0.id
order by 3 desc
```

### 8 · Update and delete

```java
userDao.update().set(User::getVip, true)
       .filter(Restrictions.eq(User::getVip, false)).execute();

userDao.update().setField(User::getUsername, Fields.concat(Fields.upper(User::getUsername), "!"))
       .filter(Restrictions.eq(User::getUsername, "Lee")).execute();

stockDao.update().setField(Stock::getAmount, Fields.minusValue(Stock::getAmount, 1L))
        .filter(Restrictions.gt(Stock::getAmount, 0L)).execute();
```

```sql
update example_user  u1_0 set vip = ?                              where u1_0.vip = ?
update example_user  u1_0 set username = (upper(u1_0.username)||?) where u1_0.username = ?
update example_stock s1_0 set amount = (s1_0.amount - cast(? as bigint)) where s1_0.amount > ?
```

| Setter | Meaning |
| --- | --- |
| `set(attr, value)` | up to three attribute/value pairs in one call |
| `set("name", value)` | by attribute name |
| `setProperty("email", "username")` | copy one column into another |
| `setField(attr, Fields…)` | assign an expression |

A delete correlates subqueries the same way:

```java
JpaDelete<User> delete = userDao.delete();
JpaSubQuery<Order, Order> subQuery = delete.subQuery(Order.class)
        .filter(Restrictions.eq(Order::getUser, User::getId));

int rows = delete.filter(Restrictions.exists(subQuery).not()).execute();
```

```sql
delete from example_user u1_0
where not exists (select o1_0.id from example_order o1_0 where o1_0.user_id = u1_0.id)
```

### 9 · Fetch joins

**Goal** — read `order.getUser()` afterwards without one extra select per order.

```java
orderDao.query().fetch(Order::getUser)
        .filter(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED))
        .selectThis().list();
```

```sql
select o1_0.id, o1_0.order_date, o1_0.status, o1_0.total_price, o1_0.user_id,
       u1_0.id, u1_0.email, u1_0.password, u1_0.username, u1_0.vip
from example_order o1_0
join example_user u1_0 on u1_0.id = o1_0.user_id
where o1_0.status <> ?
```

| Case | Call | Caveat |
| --- | --- | --- |
| To-one | `fetch(Order::getUser)` | — |
| Collection | `leftFetch(User::getOrders).distinct()` | an inner join would drop the empty ones |
| Upon a join | `join(OrderProduct::getOrder, "o", null).fetch("o", "user")` | the entity owning it must be the one selected |
| With pagination | `page().fetch(Order::getUser)` | to-one only; a fetched collection paginates in memory |

### 10 · Transformers and native SQL

```java
.setTransformer(Transformers.asMap())                   // Map<String, Object>
.setTransformer(Transformers.asCaseInsensitiveMap())    // keys case-insensitive
.setTransformer(Transformers.asList())                  // List<Object>
.setTransformer(Transformers.asBean(SalesVo.class))     // VO, matched by column alias
.setTransformer(Transformers.noop())                    // as it is
```

```java
List<Map<String, Object>> dataList = userDao.queryForMap(
        "select u.username as username, count(o.id) as order_amount"
                + " from example_user u left join example_order o on o.user_id = u.id"
                + " group by u.username order by u.username",
        new Object[0]).list();
```

| Native call | Returns |
| --- | --- |
| `query(sql, args)` | `PageableQuery<E>` |
| `query(sql, args, Vo.class)` | `PageableQuery<Vo>` |
| `query(sql, args, rowMapper)` | `PageableQuery<T>` |
| `queryForMap(sql, args)` | `PageableQuery<Map<String,Object>>`, keys case-insensitive |
| `getSingleResult(sql, args, type)` | one scalar |
| `executeUpdate(sql, args)` | affected rows |

All of them keep `rowCount()` and `paginate(...)`.

## Configuration

EasyJPA itself has **no properties to set** — no `spring.easyjpa.*` namespace, nothing to tune. What
you do configure is the provider and, for three databases, the driver.

| What | Where | Value | Why |
| --- | --- | --- | --- |
| Provider | `@EnableJpaRepositories(repositoryFactoryBeanClass = …)` | `HibernateEntityDaoFactoryBean` | the default; every feature built for it |
| | | `EclipseLinkEntityDaoFactoryBean` | EclipseLink |
| | | `StandardEntityDaoFactoryBean` | the Criteria API alone |
| EclipseLink | `spring.autoconfigure.exclude` | `…orm.jpa.HibernateJpaAutoConfiguration` | Spring Boot autoconfigures Hibernate alone |
| SQL Server | JDBC url | `calcBigDecimalPrecision=true` | otherwise every `BigDecimal` binds as `decimal(38,0)` and `coalesce` drops the decimals — `0.90` reads back as `1` |
| SQLite | entity id mapping | `GenerationType.IDENTITY` | `AUTO` falls back to a sequence table that Hibernate writes from its own transaction, which SQLite locks against |
| Oracle 23+ on Hibernate | `OracleDialect` subclass | `DatabaseVersion.make(21)` | works around a wrong `group by` alias — see [Compatibility](#compatibility--comparison) |

On EclipseLink you also build the entity manager yourself:

```java
@Bean
public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
    factoryBean.setDataSource(dataSource);
    factoryBean.setPackagesToScan("com.example.entity");
    factoryBean.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
    return factoryBean;
}
```

```xml
<dependency>
    <groupId>org.eclipse.persistence</groupId>
    <artifactId>org.eclipse.persistence.jpa</artifactId>
    <version>4.0.4</version>
</dependency>
```

## Compatibility & Comparison

### Providers

Hibernate is the default and reaches every feature. A `no` below is a limit of the provider, not of
EasyJPA — and each one is a `boolean` on `JpaProvider`, so you can ask before you build.

| | Hibernate | EclipseLink | Ask it by |
| --- | --- | --- | --- |
| Query, join, filter, sort, group, having | ✅ | ✅ | |
| Pagination counting | derived table | `count(distinct …)` | `supportsDerivedTable()` |
| Counting a grouping query with `having` | one statement | one row per group | `supportsDerivedTable()` |
| Subquery as a filter (`in`, `exists`) | ✅ | ✅ | |
| Subquery as one side of a comparison | ✅ | quantify by `Fields.all/any/some` | `supportsSubQueryAsExpression()` |
| Subquery as a selected column | ✅ | ❌ | `supportsSubQueryAsSelection()` |
| Join a subquery as a derived table | ✅ | ❌ | `supportsDerivedTable()` |
| Right join | ✅ | ❌ | `supportsRightJoin()` |
| Select an entity column by column | ✅ | ❌ — take a `Tuple` or a bean | `supportsPartialEntity()` |
| Fill a bean by its properties | ✅ | ❌ — its constructor is used | `supportsBeanProjection()` |
| Sort by column position | ✅ | ❌ | `supportsOrdinalSort()` |
| Pass a database function through | ✅ | ❌ | `supportsPassThroughFunction()` |
| `year()` · `month()` · `day()` | ✅ | ❌ — Criteria defines none | `supportsDatePart()` |

### Databases

The same 202 tests run on every provider against every database below.

| | H2 | PostgreSQL | MySQL | SQL Server | SQLite | Oracle |
| --- | --- | --- | --- | --- | --- | --- |
| Version tested | 2.3.232 | 16.13 | 9.6.0 | 16.00.4265 | 3.49.1 | 23.26.3 |
| Hibernate | ✅ | ✅ | ✅ | ⚠️ 1 | ⚠️ 3 | ⚠️ 4 |
| Criteria API alone | ✅ | ✅ | ✅ | ⚠️ 1 | ⚠️ 3 | ✅ |
| EclipseLink | ✅ | ✅ | ✅ | ✅ | ❌ no platform | ⚠️ 2 |

Each ⚠️ belongs to the database or the provider on it, never to a query EasyJPA built:

| Where | What | Why |
| --- | --- | --- |
| SQL Server | 1 concatenation of a numeric column | `+` is read by its first operand, so a `max(price)` asked for as text is taken for arithmetic — cast it first |
| SQLite | 3 tests | no quantified subquery, no `repeat`, no date type |
| Oracle 23+ / Hibernate | 4 derived-table tests | Hibernate writes `group by c0` — its internal alias, not the select item's. Same tests pass when the dialect is told it is Oracle 21 |
| Oracle / EclipseLink | 2 paginated `Tuple` tests | EclipseLink writes no aliases, then wraps as `select a.* from (…) a`; Oracle rejects two columns of the same name |

### Against the alternatives

Feature-level, not a benchmark — no timings are claimed here.

| | EasyJPA | Criteria API | Spring Data `Specification` | QueryDSL |
| --- | --- | --- | --- | --- |
| Type-safe attributes | method references | metamodel or strings | metamodel or strings | generated `Q` classes |
| Code generation step | none | none | none | annotation processor |
| Dynamic composition | ✅ | manual `Predicate[]` | ✅ | ✅ |
| Multi-branch joins | one call each | track `Join<?,?>` by hand | manual | ✅ |
| Correlated subqueries | built from the outer query | manual | awkward | ✅ |
| Grouping + `having` | ✅ | ✅ | not the design target | ✅ |
| Pagination count over a `group by` | derived table, one statement | write it yourself | write it yourself | write it yourself |
| Derived-table join | `joinSubQuery(...)` | manual | ❌ | limited |
| Native SQL with pagination kept | ✅ | ❌ | ❌ | separate API |
| Provider gaps discoverable at runtime | `JpaProvider` flags | ❌ | ❌ | ❌ |

## Documentation

| | |
| --- | --- |
| Worked examples | `src/test/java/com/github/easyjpa/test/` — 202 tests, all SQL above came from them |
| Complex statements | `ComplexQueryTests` — best sellers, repeat customers, nested subqueries, self joins |
| Pagination | `PaginationTests`, `UserDaoTests#testPageNavigation` |
| Provider capabilities | `JpaProvider`, `UtilsTests` |
| Release notes | [CHANGELOG.md](CHANGELOG.md) |

### Running the tests

H2 out of the box; the database and the provider are a matter of the active profile alone.

```bash
mvn test                                                 # Hibernate, H2
mvn test -Dspring.profiles.active=postgresql             # also: mysql, sqlserver, sqlite, oracle
mvn test -Dspring.profiles.active=standard               # the Criteria API alone
mvn test -Peclipselink                                   # EclipseLink
mvn test -Peclipselink -Dtest.profiles=eclipselink,mysql # EclipseLink + a database
```

A skipped test is a feature the provider does not reach. Coverage lands in `target/site/jacoco`.

### Known limitations

- **Aliases are strings.** A typo in `"op"` is a runtime error, not a compile error.
- **Not auto-configured.** You name the factory bean yourself; there is no `@AutoConfiguration`.
- **The Criteria API is the ceiling.** Window functions, CTEs and `UNION` need native SQL.
- **A fetched collection paginates in memory.** Keep paginated fetches to to-one associations.
- **No reactive support.** Blocking JDBC only.

## Contributing & License

Issues and pull requests are welcome at
[paganini2008/easyjpa](https://github.com/paganini2008/easyjpa). Run `mvn test` before opening one —
and if the change touches a provider-specific path, run `-Peclipselink` and `-Dspring.profiles.active=standard` too.

MIT — see [LICENSE](LICENSE).
