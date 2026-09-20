# EasyJPA

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1+-brightgreen?style=flat-square&logo=springboot)
![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk)
![JPA](https://img.shields.io/badge/Jakarta%20Persistence-3.1-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

**Type-safe dynamic queries for Spring Data JPA — every query a lambda, every join a name, not a
single JPQL string.**

EasyJPA puts a fluent, lambda-driven API over the JPA Criteria API. You keep Criteria's type safety
and dynamic composition, and you drop the `CriteriaBuilder` / `Root` / `Predicate[]` ceremony.

```java
// The Criteria API
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<User> cq = cb.createQuery(User.class);
Root<User> root = cq.from(User.class);
List<Predicate> predicates = new ArrayList<>();
predicates.add(cb.equal(root.get("username"), "Jack"));
predicates.add(cb.equal(root.get("password"), "123456"));
cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
User user = em.createQuery(cq).getSingleResult();

// EasyJPA
User user = userDao.query()
        .filter(new FilterList().eq(User::getUsername, "Jack").eq(User::getPassword, "123456"))
        .selectThis().one();
```

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Usage](#usage)
- [How It Works](#how-it-works)
- [Provider and Database Support](#provider-and-database-support)
- [Running the Tests](#running-the-tests)
- [License](#license)

## Features

- **Lambda-driven** — method references everywhere, so a renamed field is a compile error
- **Dynamic queries** — filters compose and nest without string concatenation
- **All join kinds** — inner, left, right and cross, plus derived tables
- **Subqueries** — as a filter, as a column, as one side of a comparison, nested arbitrarily
- **Computed columns** — arithmetic, functions, `CASE WHEN`, date parts rendered per database
- **Grouping and having**, with pagination that counts groups rather than rows
- **Pagination** that builds the listing query and its counting query from one definition
- **Update and delete** with correlated subqueries
- **Fetch joins** to kill N+1
- **Result transformers** — entity, VO, `Map`, `List` or `Tuple`
- **Native SQL** fallback that keeps pagination
- **Hibernate by default**, EclipseLink and the plain Criteria API supported

## Requirements

| | |
| --- | --- |
| Java | 17 or later |
| Spring Boot | 3.1 to 3.5 (`1.0.x`); 4.0 and later takes the `2.0.x` line |
| Jakarta Persistence | 3.1 (`1.0.x`); 3.2 on the `2.0.x` line |
| Build | Maven 3.9 or later, or the bundled `./mvnw` |

**The version line follows the Spring Boot line**, and the two are maintained apart — they are not
one jar built twice:

| Your Spring Boot | The version to use |
| --- | --- |
| 4.0 and later | `2.0.x` |
| 3.1 to 3.5 | `1.0.x`, this one |
| 3.0 and earlier | not supported |

Spring Boot 3.0 falls short of a few things this library builds on, namely a derived table joined
by an on condition and the parts of a date, so 3.1 is where this line starts.

## Installation

The current version is **`1.0.0-SNAPSHOT`**, published to the snapshot repository of the Central
Portal. Maven reads that repository from no project that has not named it, so both blocks are
needed:

```xml
<dependency>
    <groupId>com.github.paganini2008</groupId>
    <artifactId>easyjpa-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

```xml
<repositories>
    <repository>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases><enabled>false</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>
```

On Spring Boot 4, take `2.0.0-SNAPSHOT` from the same repository. Once `1.0.0` is cut it lands on
Maven Central proper and the `<repositories>` block is no longer needed.

## Quick Start

**1. Point Spring Data at EasyJPA's repository implementation.**

```java
@EntityScan(basePackages = {"com.example.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class,
        basePackages = {"com.example.dao"})
@Configuration(proxyBeanMethods = false)
public class JpaConfig {
}
```

The factory bean you name is the provider the whole library talks to:

| Factory bean | Provider |
| --- | --- |
| `HibernateEntityDaoFactoryBean` | Hibernate, the one Spring Boot brings along |
| `EclipseLinkEntityDaoFactoryBean` | EclipseLink |
| `StandardEntityDaoFactoryBean` | the Criteria API alone, whatever runs underneath |

**2. Let every DAO extend `EntityDao`.**

```java
public interface UserDao extends EntityDao<User, Long> {
}
```

`EntityDao` **is** a `JpaRepository` — `save`, `findById`, `deleteAll` and the rest are untouched.

**3. Query.**

```java
@Autowired
private UserDao userDao;

List<User> vips = userDao.query()
        .filter(Restrictions.eq(User::getVip, true))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis()
        .list();
```

Nothing else has to be configured. On EclipseLink, add the dependency and build the entity manager
yourself, since Spring Boot autoconfigures Hibernate alone:

```xml
<dependency>
    <groupId>org.eclipse.persistence</groupId>
    <artifactId>org.eclipse.persistence.jpa</artifactId>
    <version>4.0.4</version>
</dependency>
```

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

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

## Usage

The examples below use a small e-commerce model — the same one the test suite runs against:

```
User ──< Order ──< OrderProduct >── Product
                                        │
                                      Stock
```

### Pick an entry point

| What you need | Entry point | What it returns |
| --- | --- | --- |
| The entities themselves | `dao.query()` | the entity type |
| Some columns mapped to a VO | `dao.query(Vo.class)` | the given type |
| Any columns, whatever they are | `dao.customQuery()` | a `Tuple` |
| The same, with a total count | `dao.page()` / `dao.page(Vo.class)` / `dao.customPage()` | a `JpaPageResultSet` |
| An update or a delete | `dao.update()` / `dao.delete()` | the affected rows |
| Something the Criteria API misses | `dao.queryForMap(sql, args)` and its friends | a `PageableQuery` |

### Filtering

`Restrictions` builds one condition, `FilterList` chains several, and `.not()` negates.

```java
userDao.count(Restrictions.eq(User::getVip, true));
userDao.count(Restrictions.isNull(User::getEmail));
userDao.count(Restrictions.in(User::getUsername, List.of("Jack", "Petter")));
userDao.count(Restrictions.in(User::getUsername, usernames).not());
```

Conditions nest — `vip or (username in ('Scott','Lee') and email is not null)`:

```java
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true)
                .or(new FilterList().in(User::getUsername, List.of("Scott", "Lee"))
                        .and().notNull(User::getEmail)))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

An attribute can be addressed three ways, all equivalent:

```java
Restrictions.eq(Product::getName, "Juicer")   // by a lambda, the alias is resolved for you
Restrictions.eq("name", "Juicer")             // by an attribute name, on the root entity
Restrictions.eq("p", "name", "Juicer")        // by an alias and an attribute name
```

### Joining

Join by a lambda and give every join an alias. The lambda carries the entity it belongs to, so a
join always grows from that entity — however many branches you need:

```java
orderProductDao.customPage()
        .join(OrderProduct::getOrder, "o", null)      // OrderProduct -> Order
        .join(Order::getUser, "u", null)              // Order        -> User
        .join(OrderProduct::getProduct, "p", null)    // OrderProduct -> Product, a second branch
```

```sql
from example_order_product op1_0
join example_order o1_0 on o1_0.id = op1_0.order_id
join example_user u1_0 on u1_0.id = o1_0.user_id
join example_product p1_0 on p1_0.id = op1_0.product_id
```

`leftJoin`, `rightJoin` and `crossJoin` work the same. An `on` condition is the third argument:

```java
orderDao.customQuery().leftJoin(Order::getOrderProducts, "op",
                                Restrictions.gt("op", "amount", 10))
```

Two tables with no association at all are still joined by a cross join and correlated in the where
clause:

```java
productDao.customPage().crossJoin(Stock.class, "s")
          .filter(new FilterList().eq(Stock::getProductId, Product::getId))
```

### Grouping and aggregation

```java
List<UserOrderVo> dataList = userDao.customQuery()
        .leftJoin(User::getOrders, "o", null)
        .groupBy(new FieldList(User::getUsername))
        .having(Restrictions.gt(Fields.count(Order::getId), 0L))
        .sort(JpaSort.asc(User::getUsername))
        .select(new ColumnList().addColumns(User::getUsername)
                .addColumns(Fields.count(Order::getId).as("orderAmount"),
                            Fields.sum(Order::getTotalPrice).as("totalPrice"),
                            Fields.max(Order::getTotalPrice).as("maxPrice")))
        .setTransformer(Transformers.asBean(UserOrderVo.class))
        .list();
```

Give every computed column an alias — that alias is the VO property or the map key it lands in.

### Computed columns

```java
Fields.multiply(Property.forName(Product::getPrice),
                Property.forName(OrderProduct::getAmount)).as("subtotal")

Fields.concat(Fields.upper(User::getUsername), "!")
Fields.countDistinct(Property.forName("this", "order.id")).as("orderAmount")
Fields.month(Order::getOrderDate).as("month")     // rendered per database
```

`IfExpression` is `CASE WHEN`:

```java
IfExpression<String, String> area = new IfExpression<String, String>("location")
        .when("China", "Asia").otherwise("Other");

productDao.customQuery().select(new ColumnList().addColumns(area.as("area"))).list();
```

### Pagination

A pagination is a listing query **plus** a counting query, so build it once and take both:

```java
JpaPageResultSet<Tuple> resultSet = orderDao.customPage()
        .join(Order::getUser, "u", null)
        .filter(Restrictions.gt(Order::getTotalPrice, BigDecimal.valueOf(1000)))
        .select(new ColumnList().addColumns(Order::getId).addColumns(User::getUsername));

long total = resultSet.rowCount();          // one statement, no rows fetched
List<Tuple> rows = resultSet.list(10, 0);   // the first 10 rows
```

When the query groups, `rowCount()` counts the **groups**, `having` included, in one statement:

```java
JpaPageResultSet<Tuple> resultSet = orderProductDao.customPage()
        .join(OrderProduct::getProduct, "p", null)
        .groupBy(new FieldList().addFields(Product::getName))
        .having(Restrictions.gt(Fields.count(OrderProduct::getId), 1L))
        .select(new ColumnList().addColumns(Product::getName)
                .addColumns(Fields.sum(OrderProduct::getAmount).as("soldAmount")));

resultSet.rowCount();   // the number of products, not the number of order lines
```

Page navigation:

```java
PageResponse<Map<String, Object>> page = userDao.customPage()
        .sort(JpaSort.asc(User::getId))
        .select(new ColumnList(User::getId, User::getUsername, User::getVip))
        .setTransformer(Transformers.asCaseInsensitiveMap())
        .paginate(PageRequest.of(2));

page.getTotalRecords();
page.getTotalPages();
page.hasNextPage();
page.nextPage().getContent();
page.lastPage().isLastPage();

resultSet.paginate(PageRequest.of(10))
         .forEachPage(eachPage -> eachPage.getContent().forEach(this::handle));
```

### Subqueries

Build the subquery **from the statement that uses it** — that is what correlates the two.

```java
// exists: the users who have ever ordered
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
        .filter(Restrictions.eq(Order::getUser, User::getId))
        .select(Order::getId);

List<User> customers = query.filter(Restrictions.exists(subQuery)).selectThis().list();

// not exists: negate it
query.filter(Restrictions.exists(subQuery).not()).selectThis().list();
```

A subquery may group, join and distinct just as an ordinary query does:

```java
// in: the repeat customers
JpaSubQuery<Order, Long> repeat = query.subQuery(Order.class, "o", Long.class);
repeat.groupBy(new FieldList().addFields(Order::getUser))
      .having(Restrictions.gt(Fields.count(Order::getId), 1L))
      .select(Property.forName("o", "user.id", Long.class));

query.filter(Restrictions.in(Property.forName(User::getId), repeat)).selectThis().list();
```

It can also be a selected column:

```java
JpaQuery<Product, Tuple> query = productDao.customQuery();
JpaSubQuery<Stock, Long> stock = query.subQuery(Stock.class, "s", Long.class)
        .filter(Restrictions.eq(Stock::getProductId, Product::getId))
        .select(Fields.max(Stock::getAmount));

query.select(new ColumnList().addColumns(Product::getName)
                .addColumns(Column.forSubQuery(stock, "stockAmount")))
     .list();
```

Whatever a subquery selects out of an association, spell the key out — `Property.forName("o",
"user.id", Long.class)` rather than `Order::getUser` — since that is what every provider renders
the same way.

### Joining a derived table

When every row needs an aggregate of another table, join that aggregate once instead of running a
correlated subquery per row:

```java
JpaQuery<Product, Tuple> query = productDao.customQuery();
JpaSubQuery<OrderProduct, Tuple> sales = query.subQuery(OrderProduct.class, "op", Tuple.class);
sales.groupBy(new FieldList().addFields(Property.forName("op", "product.id")))
     .select(new ColumnList()
             .addColumns(Property.forName("op", "product.id").as("productId"))
             .addColumns(Fields.sum("op", "amount", Integer.class).as("soldAmount")));

query.joinSubQuery(sales, "s", Restrictions.eq(Property.forName("s", "productId"),
                                               Property.forName("this", "id")))
     .sort(JpaSort.desc(Property.forName("s", "soldAmount")))
     .select(new ColumnList().addColumns(Product::getName)
             .addColumns(Property.forName("s", "soldAmount").as("soldAmount")))
     .setTransformer(Transformers.asCaseInsensitiveMap())
     .list();
```

### Update and delete

```java
userDao.update().set(User::getVip, true)
       .filter(Restrictions.eq(User::getVip, false)).execute();

userDao.update().set(User::getPassword, "654321", User::getEmail, "nobody@jpatest.com")
       .filter(Restrictions.eq(User::getUsername, "Jack")).execute();

userDao.update().setProperty("email", "username")            // email = username
       .filter(Restrictions.eq(User::getUsername, "Terry")).execute();

stockDao.update().setField(Stock::getAmount, Fields.minusValue(Stock::getAmount, 1L))
        .filter(Restrictions.gt(Stock::getAmount, 0L)).execute();
```

A delete correlates subqueries just as a query does:

```java
JpaDelete<User> delete = userDao.delete();
JpaSubQuery<Order, Order> subQuery = delete.subQuery(Order.class)
        .filter(Restrictions.eq(Order::getUser, User::getId));

int rows = delete.filter(Restrictions.exists(subQuery).not()).execute();
```

### Fetch joins

An association read after the query costs one statement per entity. Fetch it along instead:

```java
orderDao.query().fetch(Order::getUser).selectThis().list();                  // to-one
userDao.query().leftFetch(User::getOrders).distinct().selectThis().list();   // collection
```

The entity owning the association has to be the one selected. A pagination fetches on the query it
lists and never on the one it counts, so keep paginated fetches to the to-one associations — a
fetched collection is paginated in memory.

### Result transformers

```java
.setTransformer(Transformers.asMap())                   // Map<String, Object>
.setTransformer(Transformers.asCaseInsensitiveMap())    // the keys are case insensitive
.setTransformer(Transformers.asList())                  // List<Object>
.setTransformer(Transformers.asBean(SalesVo.class))     // a VO, matched by the column aliases
.setTransformer(Transformers.noop())                    // keep it as it is
```

### Native SQL

Whatever the Criteria API does not reach is one call away, and the result is mapped case
insensitively, so the column labels may be cased however the database likes:

```java
List<Map<String, Object>> dataList = userDao.queryForMap(
        "select u.username as username, count(o.id) as order_amount"
                + " from example_user u left join example_order o on o.user_id = u.id"
                + " group by u.username order by u.username",
        new Object[0]).list();
```

It returns a `PageableQuery`, so `rowCount()` and `paginate(...)` work exactly as above.

## How It Works

Three things are worth knowing about, and none of them needs any configuration.

**Aliases resolve within one statement.** An alias is what tells EasyJPA which table an attribute
belongs to. A lambda names no table — it names the *entity* the attribute belongs to, and EasyJPA
looks the alias up in the statement being built, taking the nearest table of that entity. The
lookup never leaves the statement, which is why an outer query and its subquery tell their tables
apart even when both query the same entity. The root entity is always aliased `this`, and a blank
alias means the same. When one entity is joined twice, as in a self join, name the tables apart and
address them by `Property.forName("o2", ...)`.

**A pagination is two statements built from one definition.** `JpaPageResultSet` holds the listing
query and derives the counting query from it, so the two can never drift apart. When the query
groups, the counting query wraps it as a derived table and counts the groups — one statement no
matter how many groups there are, and a `having` clause counted correctly. Providers that cannot
join a derived table fall back to `count(distinct ...)`.

**Providers are asked, not assumed.** Every feature a provider may or may not reach is a method on
`JpaProvider`, so the answer is available at runtime rather than in a stack trace:

```java
if (JpaProviders.getProvider().supportsDerivedTable()) {
    ...
}
```

## Provider and Database Support

Hibernate is the default and the one every feature is built for. EclipseLink is supported as far as
it goes, and what it does not reach is reported rather than silently worked around.

| | Hibernate | EclipseLink |
| --- | --- | --- |
| Query, join, filter, sort, group, having | yes | yes |
| Pagination and its counting | by a derived table | by a `count(distinct ...)` |
| Counting a grouping query with having | one statement | one row per group |
| Subquery as a filter (`in`, `exists`) | yes | yes |
| Subquery as one side of a comparison | yes | quantify it by `Fields.all/any/some` |
| Subquery as a selected column | yes | no |
| Joining a subquery as a derived table | yes | no |
| Right join | yes | no |
| Selecting an entity column by column | yes | no, select a `Tuple` or a bean |
| Filling a bean by its properties | yes | no, its constructor is used |
| Sorting by a column position | yes | no |
| A function of the database passed through | yes | no |
| `year()`, `month()`, `day()` | yes | no, the Criteria API defines none |

Every `no` above is a limit of the provider, not of EasyJPA.

The suite is run against six databases. H2, PostgreSQL and MySQL take everything; these three have
limits of their own:

| | SQL Server | SQLite | Oracle |
| --- | --- | --- | --- |
| Sorting by a column position | yes on Hibernate, no on EclipseLink | yes | yes |
| Concatenating a numeric column | cast it to a string first | yes | yes |
| A subquery quantified by `all` / `any` | yes | no | yes |
| A function of the database passed through | yes | only SQLite's own | yes |
| A date read back as a date | yes | no, SQLite keeps text | yes |
| Joining a derived table | yes | yes | not on Hibernate against Oracle 23+ |
| Several columns of one `Tuple`, paginated | yes | yes | not on EclipseLink |
| EclipseLink | yes | no platform ships for it | yes, with the above |

Two settings are worth carrying into an application:

- **SQL Server** — put `calcBigDecimalPrecision=true` in the JDBC url. Without it the driver binds
  every `BigDecimal` as `decimal(38,0)`, and a `coalesce` against such a parameter drops the
  decimals: `0.90` comes back as `1`.
- **SQLite** — map entity ids as `IDENTITY`. `GenerationType.AUTO` falls back to a sequence table,
  which Hibernate writes from a transaction of its own, and SQLite locks against the transaction
  already open.

## Running the Tests

The tests run against H2 out of the box, and the same set of them runs on every provider and every
database. Which one is picked is a matter of the active profile alone.

```bash
mvn test                                                # Hibernate, H2
mvn test -Dspring.profiles.active=postgresql            # Hibernate, PostgreSQL
mvn test -Dspring.profiles.active=mysql                 # Hibernate, MySQL
mvn test -Dspring.profiles.active=sqlserver             # Hibernate, SQL Server
mvn test -Dspring.profiles.active=sqlite                # Hibernate, SQLite
mvn test -Dspring.profiles.active=oracle                # Hibernate, Oracle
mvn test -Dspring.profiles.active=standard              # the Criteria API alone
mvn test -Peclipselink                                  # EclipseLink
mvn test -Peclipselink -Dtest.profiles=eclipselink,mysql
```

A skipped test is a feature the provider does not reach, listed in the tables above. Coverage lands
in `target/site/jacoco`.

## License

MIT. See [LICENSE](LICENSE).

## Links

- **GitHub** — [paganini2008/easyjpa](https://github.com/paganini2008/easyjpa)
- **Changelog** — [CHANGELOG.md](CHANGELOG.md)
