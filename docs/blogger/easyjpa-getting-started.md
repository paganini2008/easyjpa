# EasyJPA: Type-Safe Dynamic Queries in Three Lines, Not Thirty

> **Every query is a lambda. Every join has a name. Not a single JPQL string.**

**EasyJPA** is a Spring Boot starter that puts a fluent, lambda-driven API over the JPA Criteria
API. You keep the type safety and the dynamic query building that Criteria gives you, and you drop
the `CriteriaBuilder` / `Root` / `Predicate[]` ceremony that makes it unreadable. Joins, subqueries,
grouping, pagination, updates, deletes and native SQL — all of it in one chain you can read top to
bottom.

Here is the whole pitch in one screenshot's worth of code.

**The Criteria API:**

```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<User> cq = cb.createQuery(User.class);
Root<User> root = cq.from(User.class);
List<Predicate> predicates = new ArrayList<>();
predicates.add(cb.equal(root.get("username"), "Jack"));
predicates.add(cb.equal(root.get("password"), "123456"));
cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
User user = em.createQuery(cq).getSingleResult();
```

**EasyJPA:**

```java
User user = userDao.query()
        .filter(new FilterList().eq(User::getUsername, "Jack").eq(User::getPassword, "123456"))
        .selectThis().one();
```

Same query. Same type safety. Same generated SQL.

---

## Install it in two steps

**Step 1** — add the dependency.

```xml
<dependency>
    <groupId>com.github.paganini2008</groupId>
    <artifactId>easyjpa-spring-boot-starter</artifactId>
    <version>2.0.0-SNAPSHOT</version>  <!-- Spring Boot 4; on Spring Boot 3 take 1.0.0-SNAPSHOT -->
</dependency>
```

The current version is `2.0.0-SNAPSHOT`, which lives in the snapshot repository of the Central
Portal, so name that repository too:

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

**Step 2** — point Spring Data at EasyJPA's repository implementation.

```java
@EntityScan(basePackages = {"com.example.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class,
        basePackages = {"com.example.dao"})
@Configuration(proxyBeanMethods = false)
public class JpaConfig {
}
```

That's it. Now every DAO extends `EntityDao` instead of `JpaRepository`:

```java
public interface UserDao extends EntityDao<User, Long> {
}
```

`EntityDao` **is** a `JpaRepository` — `save`, `findById`, `deleteAll` and the rest are all still
there. EasyJPA just adds the query builders on top.

---

## The model used below

Every example in this post is lifted from EasyJPA's own test suite, which runs against a small
e-commerce schema:

```
User  ──< Order ──< OrderProduct >── Product
                                         │
                                       Stock
```

`User` has `username`, `email`, `vip`. `Order` has `totalPrice`, `orderDate`, `status`.
`Product` has `name`, `price`, `discount`, `location`. Nothing surprising.

---

## Filtering

`Restrictions` builds a single condition. `FilterList` chains several.

```java
userDao.count(Restrictions.eq(User::getVip, true));
userDao.count(Restrictions.isNull(User::getEmail));
userDao.count(Restrictions.in(User::getUsername, List.of("Jack", "Petter", "Nobody")));
userDao.count(Restrictions.like(User::getEmail, "jpatest"));
```

Every one of them takes a method reference, so a renamed field is a compile error rather than a
runtime surprise.

Negation is a method, not a different class:

```java
Restrictions.in(User::getUsername, usernames).not()          // not in
Restrictions.notLike(User::getEmail, "00")
    .or(Restrictions.eq(User::getUsername, "Jack"))          // or
```

And conditions nest the way you'd write them on a whiteboard —
`vip or (username in ('Scott','Lee') and email is not null)`:

```java
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true)
                .or(new FilterList().in(User::getUsername, List.of("Scott", "Lee"))
                        .and().notNull(User::getEmail)))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

---

## Joining

Join by a lambda and EasyJPA works out which table you're growing from. Give every join a short
alias — that's the name you'll use later.

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

Notice the third join branches back off `OrderProduct` rather than continuing from `User`. The
lambda carries its own entity, so the tree comes out the way it reads.

`leftJoin`, `rightJoin` and `crossJoin` are all there too, and an `on` condition is just the third
argument:

```java
orderDao.customQuery().leftJoin(Order::getOrderProducts, "op",
                                Restrictions.gt("op", "amount", 10))
```

---

## Grouping, aggregating, and mapping to a VO

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

The alias you give a computed column (`.as("orderAmount")`) is the VO property it lands in. Same
rule for map keys.

Don't want a VO? Pick another shape:

```java
.setTransformer(Transformers.asMap())                   // Map<String, Object>
.setTransformer(Transformers.asCaseInsensitiveMap())    // keys are case insensitive
.setTransformer(Transformers.asList())                  // List<Object>
.setTransformer(Transformers.asBean(SalesVo.class))     // a VO
```

`having` filters the groups:

```java
.having(Restrictions.gt(Fields.count(Order::getId), 0L))
```

---

## Computed columns

`Fields` gives you the arithmetic and the functions:

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
        .when("China", "Asia")
        .otherwise("Other");

productDao.customQuery()
        .select(new ColumnList().addColumns(area.as("area")))
        .list();
```

---

## Pagination that counts correctly

This is where most Criteria code goes wrong. A pagination is a listing query **plus** a counting
query, so EasyJPA builds it once and hands you both:

```java
JpaPageResultSet<Tuple> resultSet = orderDao.customPage()
        .join(Order::getUser, "u", null)
        .filter(Restrictions.gt(Order::getTotalPrice, BigDecimal.valueOf(1000)))
        .select(new ColumnList().addColumns(Order::getId).addColumns(User::getUsername));

long total = resultSet.rowCount();          // one statement, no rows fetched
List<Tuple> rows = resultSet.list(10, 0);   // the first 10 rows
```

And when the query groups, `rowCount()` counts **groups**, not rows — through a derived table, in
one statement, `having` clause included. That's the bug you don't have to find:

```java
JpaPageResultSet<Tuple> resultSet = orderProductDao.customPage()
        .join(OrderProduct::getProduct, "p", null)
        .groupBy(new FieldList().addFields(Product::getName))
        .having(Restrictions.gt(Fields.count(OrderProduct::getId), 1L))
        .select(new ColumnList().addColumns(Product::getName)
                .addColumns(Fields.sum(OrderProduct::getAmount).as("soldAmount")));

resultSet.rowCount();   // the number of products, not the number of order lines
```

Walking pages is a small API of its own:

```java
PageResponse<Map<String, Object>> page = userDao.customPage()
        .sort(JpaSort.asc(User::getId))
        .select(new ColumnList(User::getId, User::getUsername, User::getVip))
        .setTransformer(Transformers.asCaseInsensitiveMap())
        .paginate(PageRequest.of(2));       // 2 rows per page

page.getTotalRecords();
page.getTotalPages();
page.hasNextPage();
page.nextPage().getContent();
page.lastPage().isLastPage();
```

Or stream every page:

```java
resultSet.setTransformer(Transformers.asCaseInsensitiveMap())
        .paginate(PageRequest.of(10))
        .forEachPage(eachPage -> eachPage.getContent().forEach(this::handle));
```

---

## Subqueries

Build the subquery **from the query that uses it** — that's what correlates the two.

**exists** — the users who have ever ordered:

```java
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
        .filter(Restrictions.eq(Order::getUser, User::getId))
        .select(Order::getId);

List<User> customers = query.filter(Restrictions.exists(subQuery)).selectThis().list();
```

**not exists** — the products nobody ever bought:

```java
query.filter(Restrictions.exists(subQuery).not()).selectThis().list();
```

**in, with a grouping subquery** — the repeat customers:

```java
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class);
subQuery.groupBy(new FieldList().addFields(Order::getUser))
        .having(Restrictions.gt(Fields.count(Order::getId), 1L))
        .select(Property.forName("o", "user.id", Long.class));

query.filter(Restrictions.in(Property.forName(User::getId), subQuery)).selectThis().list();
```

**As a selected column** — every product with its stock alongside:

```java
JpaQuery<Product, Tuple> query = productDao.customQuery();
JpaSubQuery<Stock, Long> stock = query.subQuery(Stock.class, "s", Long.class)
        .filter(Restrictions.eq(Stock::getProductId, Product::getId))
        .select(Fields.max(Stock::getAmount));

query.select(new ColumnList().addColumns(Product::getName)
                .addColumns(Column.forSubQuery(stock, "stockAmount")))
     .list();
```

Subqueries nest, join, group and distinct exactly like ordinary queries do.

---

## Joining a derived table

When every row needs an aggregate of another table, join the aggregate once instead of running a
correlated subquery per row:

```java
JpaQuery<Product, Tuple> query = productDao.customQuery();
JpaSubQuery<OrderProduct, Tuple> sales = query.subQuery(OrderProduct.class, "op", Tuple.class);
sales.groupBy(new FieldList().addFields(Property.forName("op", "product.id")))
     .select(new ColumnList()
             .addColumns(Property.forName("op", "product.id").as("productId"))
             .addColumns(Fields.sum("op", "amount", Integer.class).as("soldAmount")));

List<Map<String, Object>> dataList = query
        .joinSubQuery(sales, "s", Restrictions.eq(Property.forName("s", "productId"),
                                                  Property.forName("this", "id")))
        .sort(JpaSort.desc(Property.forName("s", "soldAmount")))
        .select(new ColumnList().addColumns(Product::getName)
                .addColumns(Property.forName("s", "soldAmount").as("soldAmount")))
        .setTransformer(Transformers.asCaseInsensitiveMap())
        .list();
```

---

## Updates and deletes

```java
userDao.update().set(User::getVip, true)
       .filter(Restrictions.eq(User::getVip, false))
       .execute();

userDao.update().set(User::getPassword, "654321", User::getEmail, "nobody@jpatest.com")
       .filter(Restrictions.eq(User::getUsername, "Jack"))
       .execute();
```

Set one column from another, or from an expression:

```java
userDao.update().setProperty("email", "username")                        // email = username
       .filter(Restrictions.eq(User::getUsername, "Terry")).execute();

userDao.update().setField(User::getUsername, Fields.concat(Fields.upper(User::getUsername), "!"))
       .filter(Restrictions.eq(User::getUsername, "Lee")).execute();

stockDao.update().setField(Stock::getAmount, Fields.minusValue(Stock::getAmount, 1L))
        .filter(Restrictions.gt(Stock::getAmount, 0L)).execute();
```

Deletes correlate subqueries just the same — here, every user who never ordered:

```java
JpaDelete<User> delete = userDao.delete();
JpaSubQuery<Order, Order> subQuery = delete.subQuery(Order.class)
        .filter(Restrictions.eq(Order::getUser, User::getId));

int rows = delete.filter(Restrictions.exists(subQuery).not()).execute();
```

---

## Fetch joins

An association read after the query costs one SELECT per entity. Fetch it along instead:

```java
orderDao.query().fetch(Order::getUser).selectThis().list();                  // to-one
userDao.query().leftFetch(User::getOrders).distinct().selectThis().list();   // collection
```

Pagination fetches on the listing query and never on the counting one, so this stays one extra
join rather than N+1:

```java
JpaPageResultSet<Order> resultSet = orderDao.page().fetch(Order::getUser)
        .filter(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED))
        .selectThis();
```

---

## Native SQL, with pagination intact

Whatever Criteria can't express is one call away, and the result is mapped case-insensitively:

```java
List<Map<String, Object>> dataList = userDao.queryForMap(
        "select u.username as username, count(o.id) as order_amount"
                + " from example_user u left join example_order o on o.user_id = u.id"
                + " group by u.username order by u.username",
        new Object[0]).list();
```

It returns a `PageableQuery`, so `rowCount()` and `paginate(...)` work exactly as above.

---

## Pick your entry point

The whole API surface fits in one table:

| What you need | Entry point | What you get |
| --- | --- | --- |
| The entities themselves | `dao.query()` | the entity type |
| Some columns mapped to a VO | `dao.query(Vo.class)` | the given type |
| Any columns at all | `dao.customQuery()` | a `Tuple` |
| The same, with a total count | `dao.page()` / `dao.customPage()` | a `JpaPageResultSet` |
| An update or a delete | `dao.update()` / `dao.delete()` | the affected rows |
| Something Criteria can't reach | `dao.queryForMap(sql, args)` | a `PageableQuery` |

---

## Which provider, which database

Hibernate is the default and reaches every feature. EclipseLink and the plain Criteria API are
supported as far as they go — and where they fall short, EasyJPA tells you at runtime instead of
failing somewhere down in a stack trace:

```java
if (JpaProviders.getProvider().supportsDerivedTable()) {
    ...
}
```

The same test suite runs against **H2, PostgreSQL, MySQL, SQL Server, SQLite and Oracle**, on all
three providers. The README documents exactly which combinations skip what, and why.

---

## Try it

```java
userDao.query()
       .filter(Restrictions.eq(User::getVip, true))
       .sort(JpaSort.asc(User::getUsername))
       .selectThis()
       .list();
```

If that reads like the query you meant, you already know the API.

**GitHub:** [paganini2008/easyjpa](https://github.com/paganini2008/easyjpa) — MIT licensed.
Spring Boot 3 users take the `1.0.x` line, Spring Boot 4 users the `2.0.x` line.
`2.0.0` is on its way to Maven Central; until then `2.0.0-SNAPSHOT` is the one to use.

<!-- Suggested tags: java, springboot, jpa, hibernate -->
