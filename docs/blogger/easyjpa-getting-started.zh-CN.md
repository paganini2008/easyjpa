# EasyJPA：三行写完的类型安全动态查询

> **每个查询都是 Lambda，每个 join 都有名字，一个 JPQL 字符串都不用写。**

**EasyJPA** 是一个 Spring Boot starter，它在 JPA Criteria API 之上包了一层流式的、Lambda 驱动的
API。Criteria 给你的类型安全和动态拼装能力一样不少，而 `CriteriaBuilder` / `Root` /
`Predicate[]` 那套让人读不下去的样板代码全部消失。Join、子查询、分组、分页、更新、删除、原生
SQL——全都在一条从上往下读得通的链式调用里。

一屏代码就能说完全部卖点。

**Criteria API 的写法：**

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

**EasyJPA 的写法：**

```java
User user = userDao.query()
        .filter(new FilterList().eq(User::getUsername, "Jack").eq(User::getPassword, "123456"))
        .selectThis().one();
```

同一个查询，同样的类型安全，生成的 SQL 也一模一样。

---

## 两步装好

**第一步** —— 加依赖。

```xml
<dependency>
    <groupId>com.github.paganini2008</groupId>
    <artifactId>easyjpa-spring-boot-starter</artifactId>
    <version>2.0.0-SNAPSHOT</version>  <!-- Spring Boot 4；Spring Boot 3 用 1.0.0-SNAPSHOT -->
</dependency>
```

当前版本是 `2.0.0-SNAPSHOT`，发布在 Central Portal 的快照仓库里，所以还要把这个仓库声明出来：

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

**第二步** —— 让 Spring Data 用 EasyJPA 的仓储实现。

```java
@EntityScan(basePackages = {"com.example.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class,
        basePackages = {"com.example.dao"})
@Configuration(proxyBeanMethods = false)
public class JpaConfig {
}
```

完事。接下来每个 DAO 继承 `EntityDao` 而不是 `JpaRepository`：

```java
public interface UserDao extends EntityDao<User, Long> {
}
```

`EntityDao` **本身就是** `JpaRepository` —— `save`、`findById`、`deleteAll` 这些一个都没少，
EasyJPA 只是在上面加了查询构建器。

---

## 下文用到的模型

本文所有例子都取自 EasyJPA 自己的测试用例，跑在一个小型电商模型上：

```
User  ──< Order ──< OrderProduct >── Product
                                         │
                                       Stock
```

`User` 有 `username`、`email`、`vip`；`Order` 有 `totalPrice`、`orderDate`、`status`；
`Product` 有 `name`、`price`、`discount`、`location`。没什么意外。

---

## 条件过滤

`Restrictions` 构造单个条件，`FilterList` 把多个串起来。

```java
userDao.count(Restrictions.eq(User::getVip, true));
userDao.count(Restrictions.isNull(User::getEmail));
userDao.count(Restrictions.in(User::getUsername, List.of("Jack", "Petter", "Nobody")));
userDao.count(Restrictions.like(User::getEmail, "jpatest"));
```

每一个都接收方法引用，所以字段改名是编译期报错，而不是上线之后才发现。

取反是一个方法，不是另一个类：

```java
Restrictions.in(User::getUsername, usernames).not()          // not in
Restrictions.notLike(User::getEmail, "00")
    .or(Restrictions.eq(User::getUsername, "Jack"))          // or
```

嵌套条件怎么想就怎么写 —— `vip or (username in ('Scott','Lee') and email is not null)`：

```java
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true)
                .or(new FilterList().in(User::getUsername, List.of("Scott", "Lee"))
                        .and().notNull(User::getEmail)))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

---

## 关联查询

用 Lambda 来 join，EasyJPA 自己算出是从哪张表长出来的。给每个 join 起一个短别名，后面就靠它来指认。

```java
orderProductDao.customPage()
        .join(OrderProduct::getOrder, "o", null)      // OrderProduct -> Order
        .join(Order::getUser, "u", null)              // Order        -> User
        .join(OrderProduct::getProduct, "p", null)    // OrderProduct -> Product，另一条分支
```

```sql
from example_order_product op1_0
join example_order o1_0 on o1_0.id = op1_0.order_id
join example_user u1_0 on u1_0.id = o1_0.user_id
join example_product p1_0 on p1_0.id = op1_0.product_id
```

注意第三个 join 是从 `OrderProduct` 重新分叉的，而不是接着 `User` 往下走。Lambda 自带它所属的实体，
所以关联树写出来就是它读起来的样子。

`leftJoin`、`rightJoin`、`crossJoin` 也都在，`on` 条件就是第三个参数：

```java
orderDao.customQuery().leftJoin(Order::getOrderProducts, "op",
                                Restrictions.gt("op", "amount", 10))
```

---

## 分组、聚合、映射成 VO

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

你给计算列起的别名（`.as("orderAmount")`）就是它落到 VO 的哪个属性上。映射成 Map 时同理，别名即 key。

不想要 VO？换个形状就行：

```java
.setTransformer(Transformers.asMap())                   // Map<String, Object>
.setTransformer(Transformers.asCaseInsensitiveMap())    // key 不区分大小写
.setTransformer(Transformers.asList())                  // List<Object>
.setTransformer(Transformers.asBean(SalesVo.class))     // 映射成 VO
```

`having` 用来过滤分组：

```java
.having(Restrictions.gt(Fields.count(Order::getId), 0L))
```

---

## 计算列

算术和函数都在 `Fields` 里：

```java
Fields.multiply(Property.forName(Product::getPrice),
                Property.forName(OrderProduct::getAmount)).as("subtotal")

Fields.concat(Fields.upper(User::getUsername), "!")
Fields.countDistinct(Property.forName("this", "order.id")).as("orderAmount")
Fields.month(Order::getOrderDate).as("month")     // 按数据库方言各自渲染
```

`IfExpression` 就是 `CASE WHEN`：

```java
IfExpression<String, String> area = new IfExpression<String, String>("location")
        .when("China", "Asia")
        .otherwise("Other");

productDao.customQuery()
        .select(new ColumnList().addColumns(area.as("area")))
        .list();
```

---

## 算得准的分页

这是大多数 Criteria 代码出错的地方。一次分页 = 一个列表查询 **加** 一个计数查询，EasyJPA 构建一次，
两个都给你：

```java
JpaPageResultSet<Tuple> resultSet = orderDao.customPage()
        .join(Order::getUser, "u", null)
        .filter(Restrictions.gt(Order::getTotalPrice, BigDecimal.valueOf(1000)))
        .select(new ColumnList().addColumns(Order::getId).addColumns(User::getUsername));

long total = resultSet.rowCount();          // 一条语句，一行数据都不取
List<Tuple> rows = resultSet.list(10, 0);   // 前 10 行
```

查询带分组时，`rowCount()` 数的是**分组数**而不是行数 —— 通过派生表实现，一条语句搞定，`having`
也算得对。这个 bug 你不用再去排查了：

```java
JpaPageResultSet<Tuple> resultSet = orderProductDao.customPage()
        .join(OrderProduct::getProduct, "p", null)
        .groupBy(new FieldList().addFields(Product::getName))
        .having(Restrictions.gt(Fields.count(OrderProduct::getId), 1L))
        .select(new ColumnList().addColumns(Product::getName)
                .addColumns(Fields.sum(OrderProduct::getAmount).as("soldAmount")));

resultSet.rowCount();   // 商品的个数，不是订单明细的行数
```

翻页本身也是一套小 API：

```java
PageResponse<Map<String, Object>> page = userDao.customPage()
        .sort(JpaSort.asc(User::getId))
        .select(new ColumnList(User::getId, User::getUsername, User::getVip))
        .setTransformer(Transformers.asCaseInsensitiveMap())
        .paginate(PageRequest.of(2));       // 每页 2 条

page.getTotalRecords();
page.getTotalPages();
page.hasNextPage();
page.nextPage().getContent();
page.lastPage().isLastPage();
```

或者把每一页都流式处理掉：

```java
resultSet.setTransformer(Transformers.asCaseInsensitiveMap())
        .paginate(PageRequest.of(10))
        .forEachPage(eachPage -> eachPage.getContent().forEach(this::handle));
```

---

## 子查询

子查询要**从用它的那个查询上创建**，这是两者产生关联的关键。

**exists** —— 下过单的用户：

```java
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
        .filter(Restrictions.eq(Order::getUser, User::getId))
        .select(Order::getId);

List<User> customers = query.filter(Restrictions.exists(subQuery)).selectThis().list();
```

**not exists** —— 从来没卖出去过的商品：

```java
query.filter(Restrictions.exists(subQuery).not()).selectThis().list();
```

**in，配合分组子查询** —— 回头客：

```java
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class);
subQuery.groupBy(new FieldList().addFields(Order::getUser))
        .having(Restrictions.gt(Fields.count(Order::getId), 1L))
        .select(Property.forName("o", "user.id", Long.class));

query.filter(Restrictions.in(Property.forName(User::getId), subQuery)).selectThis().list();
```

**当成查询列** —— 每个商品连带它的库存：

```java
JpaQuery<Product, Tuple> query = productDao.customQuery();
JpaSubQuery<Stock, Long> stock = query.subQuery(Stock.class, "s", Long.class)
        .filter(Restrictions.eq(Stock::getProductId, Product::getId))
        .select(Fields.max(Stock::getAmount));

query.select(new ColumnList().addColumns(Product::getName)
                .addColumns(Column.forSubQuery(stock, "stockAmount")))
     .list();
```

子查询照样可以嵌套、join、分组、去重，跟普通查询没有区别。

---

## 把派生表 join 进来

当每一行都需要另一张表的聚合值时，把聚合结果作为一张表 join 一次，而不是每行跑一遍关联子查询：

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

## 更新与删除

```java
userDao.update().set(User::getVip, true)
       .filter(Restrictions.eq(User::getVip, false))
       .execute();

userDao.update().set(User::getPassword, "654321", User::getEmail, "nobody@jpatest.com")
       .filter(Restrictions.eq(User::getUsername, "Jack"))
       .execute();
```

用另一个字段赋值，或者用表达式赋值：

```java
userDao.update().setProperty("email", "username")                        // email = username
       .filter(Restrictions.eq(User::getUsername, "Terry")).execute();

userDao.update().setField(User::getUsername, Fields.concat(Fields.upper(User::getUsername), "!"))
       .filter(Restrictions.eq(User::getUsername, "Lee")).execute();

stockDao.update().setField(Stock::getAmount, Fields.minusValue(Stock::getAmount, 1L))
        .filter(Restrictions.gt(Stock::getAmount, 0L)).execute();
```

删除同样可以关联子查询 —— 比如清掉从没下过单的用户：

```java
JpaDelete<User> delete = userDao.delete();
JpaSubQuery<Order, Order> subQuery = delete.subQuery(Order.class)
        .filter(Restrictions.eq(Order::getUser, User::getId));

int rows = delete.filter(Restrictions.exists(subQuery).not()).execute();
```

---

## Fetch Join

查询之后再去读关联，每个实体要多发一条 SELECT。直接一起取回来：

```java
orderDao.query().fetch(Order::getUser).selectThis().list();                  // to-one
userDao.query().leftFetch(User::getOrders).distinct().selectThis().list();   // 集合
```

分页时只在列表查询上 fetch，计数查询上不会，所以这里多的是一个 join，而不是 N+1：

```java
JpaPageResultSet<Order> resultSet = orderDao.page().fetch(Order::getUser)
        .filter(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED))
        .selectThis();
```

---

## 原生 SQL，分页照旧

Criteria 表达不了的，一个方法调用就能退回原生 SQL，结果按不区分大小写映射：

```java
List<Map<String, Object>> dataList = userDao.queryForMap(
        "select u.username as username, count(o.id) as order_amount"
                + " from example_user u left join example_order o on o.user_id = u.id"
                + " group by u.username order by u.username",
        new Object[0]).list();
```

它返回的是 `PageableQuery`，所以 `rowCount()` 和 `paginate(...)` 用法跟上面完全一致。

---

## 选对入口

整个 API 的入口就这一张表：

| 你要什么 | 入口 | 返回什么 |
| --- | --- | --- |
| 实体本身 | `dao.query()` | 实体类型 |
| 若干列映射成 VO | `dao.query(Vo.class)` | 指定的类型 |
| 任意列 | `dao.customQuery()` | `Tuple` |
| 同上，外加总数 | `dao.page()` / `dao.customPage()` | `JpaPageResultSet` |
| 更新或删除 | `dao.update()` / `dao.delete()` | 影响行数 |
| Criteria 够不着的 | `dao.queryForMap(sql, args)` | `PageableQuery` |

---

## 支持哪些 Provider 和数据库

Hibernate 是默认实现，所有特性都跑得通。EclipseLink 和纯 Criteria API 也支持，能做到哪一步就是哪一步
—— 做不到的地方，EasyJPA 在运行期直接告诉你，而不是让你去某个栈底捞异常：

```java
if (JpaProviders.getProvider().supportsDerivedTable()) {
    ...
}
```

同一套测试会在 **H2、PostgreSQL、MySQL、SQL Server、SQLite、Oracle** 上，以三种 Provider 各跑一遍。
哪些组合会跳过哪些用例、为什么跳过，README 里逐条写明。

---

## 试一下

```java
userDao.query()
       .filter(Restrictions.eq(User::getVip, true))
       .sort(JpaSort.asc(User::getUsername))
       .selectThis()
       .list();
```

如果这段读起来就是你想要的那个查询，那你已经会用这套 API 了。

**GitHub：**[paganini2008/easyjpa](https://github.com/paganini2008/easyjpa)，MIT 协议。
Spring Boot 3 用户走 `1.0.x` 线，Spring Boot 4 用户走 `2.0.x` 线。
`2.0.0` 正式版正在路上，在那之前请用 `2.0.0-SNAPSHOT`。

<!-- 建议标签：java, springboot, jpa, hibernate -->
