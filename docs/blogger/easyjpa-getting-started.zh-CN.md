# EasyJPA：Criteria API 的全部能力，三行写完

## 1. 项目概览

**EasyJPA —— Criteria API 的全部能力，三行写完。**

EasyJPA 是一个 Spring Boot starter，在 JPA Criteria API 之上包了一层流式、Lambda 驱动的 API。
每个条件都是方法引用，每个 join 都有名字，一个 JPQL 字符串都不用写。Join、子查询、分组、分页、
更新、原生 SQL，全部在一条从上往下读得通的链式调用里。

全部卖点一屏说完：

```java
// ── Criteria API 的写法 ────────────────────────────────────────────────────────
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<User> cq = cb.createQuery(User.class);
Root<User> root = cq.from(User.class);
List<Predicate> predicates = new ArrayList<>();
predicates.add(cb.equal(root.get("username"), "Jack"));
predicates.add(cb.equal(root.get("password"), "123456"));
cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
User user = em.createQuery(cq).getSingleResult();

// ── EasyJPA 的写法 ────────────────────────────────────────────────────────────
User user = userDao.query()
        .filter(new FilterList().eq(User::getUsername, "Jack").eq(User::getPassword, "123456"))
        .selectThis().one();
```

同一个查询，同样的类型安全，生成的 SQL 一模一样。

## 2. 它解决什么问题

Criteria API 是 JPA 里唯一类型安全的动态查询方式，但几乎没人愿意用它。三个条件的过滤要写十行
`CriteriaBuilder` 管道代码；多分支 join 要手工维护一堆 `Join<?, ?>` 变量；关联子查询基本没法读。
于是团队退回到 JPQL 字符串或者 Spring Data 方法名——然后丢掉类型安全，或者丢掉可组合性，或者两样
都丢。

第二个问题更隐蔽，代价更大。一次分页其实是**两条**语句：列表查询和计数查询，而它们是分开写的，
于是会漂移。而当查询带 `group by` 时，顺手写的 `count(*)` 数的是行数而不是分组数，总数直接就是错的。
EasyJPA 把两条语句从同一份定义推导出来，并用派生表来数分组。

| | |
| --- | --- |
| **保留** | Criteria 的类型安全和动态组合能力 |
| **丢掉** | `CriteriaBuilder` / `Root` / `Predicate[]` 样板代码 |
| **修掉** | 会漂移的分页计数，以及把行数当分组数的错误总数 |

## 3. 快速开始

**安装** —— 当前版本是 `2.0.0-SNAPSHOT`，在 Central Portal 的快照仓库里；没有声明过这个仓库的项目
Maven 是读不到的：

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

**指定 Provider：**

```java
@EntityScan(basePackages = {"com.example.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class,
        basePackages = {"com.example.dao"})
@Configuration(proxyBeanMethods = false)
public class JpaConfig {
}
```

**继承 `EntityDao`：**

```java
public interface UserDao extends EntityDao<User, Long> {
}
```

`EntityDao` **本身就是** `JpaRepositoryImplementation`，所以 `save`、`findById`、`deleteAll` 一个
都没少。然后查：

```java
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

配置到此结束。没有属性要加，没有参数要调。

## 4. 环境要求

| | 最低版本 | 说明 |
| --- | --- | --- |
| Java | 17 | |
| Spring Boot | 4.0 | Spring Boot 3.1 – 3.5 请走 `1.0.x` 线 |
| Jakarta Persistence | 3.2 | |
| JPA Provider | Hibernate 7.4 | 或 EclipseLink 5.0.1，或任意 Criteria 实现 |
| 构建 | Maven 3.9 | |

版本线跟随 Spring Boot 线，两条线独立维护：

| 你的 Spring Boot | 用哪个版本 |
| --- | --- |
| 4.0 及以上 | **`2.0.x`，本版** |
| 3.1 – 3.5 | `1.0.x` |
| 3.0 及以下 | 不支持 |

## 5. 工作原理

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  你的代码          userDao.query().filter(...).sort(...).selectThis().list()  │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │  方法引用 + 别名
┌──────────────────────────────────▼──────────────────────────────────────────┐
│  EASYJPA          Model ─────────── 把别名解析成具体的表                     │
│                   Filter · Field · Column · JpaSort                         │
│                   JpaPageResultSet ─ 一份定义，两条语句                      │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │  「这里能用派生表吗？」
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
│  数据库       H2 · PostgreSQL · MySQL · SQL Server · SQLite · Oracle        │
└─────────────────────────────────────────────────────────────────────────────┘
```

| 机制 | 一句话 |
| --- | --- |
| **别名解析** | Lambda 指的是*实体*而不是表；别名只在正在构建的那条语句内查找，绝不越界——所以外层查询和它的子查询各认各的表 |
| **一份定义，两条语句** | `JpaPageResultSet` 持有列表查询，并由它推导出计数查询，两者不可能漂移 |
| **Provider 靠问不靠猜** | 每个可选特性都是 `JpaProvider` 上的一个 `boolean`，运行期可读 |

分页的拆分是最值得看的一部分：

```
      customPage().join(...).filter(...).groupBy(...).select(...)
                              一份定义
                                    │
                 ┌──────────────────┴──────────────────┐
                 ▼                                     ▼
            list(10, 0)                           rowCount()
                 │                                     │
   select … offset ? rows                   无 group by → select count(1) from (…) d
          fetch first ? rows only            有 group by → select count(1) from
                                                          ( <分组结果> ) d
```

## 6. 代码示例

下面每一段 SQL 都是 Hibernate 实际生成的，从测试用例跑 H2 时抓下来的。数据模型：

```
User ──< Order ──< OrderProduct >── Product        User.vip, Order.status, Order.totalPrice
                                        │          Product.name/price/location
                                      Stock        Stock.amount
```

### 示例 1 —— 嵌套条件

**输入** —— `vip or (username in ('Scott','Lee') and email is not null)`，按用户名排序。

**执行**

```java
List<User> users = userDao.query()
        .filter(Restrictions.eq(User::getVip, true)
                .or(new FilterList().in(User::getUsername, List.of("Scott", "Lee"))
                        .and().notNull(User::getEmail)))
        .sort(JpaSort.asc(User::getUsername))
        .selectThis().list();
```

**输出**

```sql
select u1_0.id, u1_0.email, u1_0.password, u1_0.username, u1_0.vip
from example_user u1_0
where u1_0.vip = ? or u1_0.username in (?, ?) and u1_0.email is not null
order by u1_0.username
```

### 示例 2 —— 四张表，两条分支

**输入** —— 订单明细连带订单、客户、商品；只要有折扣的商品、已付款或已发货的订单；最新的在前。

**执行**

```java
orderProductDao.customPage()
        .join(OrderProduct::getOrder, "o", null)      // OrderProduct → Order
        .join(Order::getUser, "u", null)              // Order        → User
        .join(OrderProduct::getProduct, "p", null)    // OrderProduct → Product，第二条分支
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

**输出**

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

注意第三个 join 是从 `OrderProduct` 重新分叉的，而不是接着 `User` 往下走：Lambda 自带它所属的实体，
所以关联树写出来就是它读起来的样子。

### 示例 3 —— 数分组而不是数行的分组分页

**输入** —— 畅销榜：每个商品卖了多少件、出现在多少个订单里、带来多少营收，只保留卖出超过一次的；
再要一个总数用于翻页。

**执行**

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

**输出 —— 列表查询**

```sql
select p1_0.name, sum(op1_0.amount), count(distinct op1_0.order_id),
       sum((p1_0.price * op1_0.amount))
from example_order_product op1_0
join example_product p1_0 on p1_0.id = op1_0.product_id
group by p1_0.name having count(op1_0.id) > ?
order by 2 desc
offset ? rows
```

**输出 —— 由同一份定义推导出的计数查询**

```sql
select count(1) from (
    select 1 from example_order_product op1_0
    join example_product p1_0 on p1_0.id = op1_0.product_id
    group by p1_0.name having count(op1_0.id) > ?
) derived1_0(c)
```

一条语句，`having` 也算进去了，一个分组都不用取。这个 bug 你不用再去排查了。

### 示例 4 —— 关联子查询

**输入** —— 下过单的用户。

**执行**

```java
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
        .filter(Restrictions.eq(Order::getUser, User::getId))
        .select(Order::getId);

List<User> customers = query.filter(Restrictions.exists(subQuery)).selectThis().list();
```

**输出**

```sql
select u1_0.id, u1_0.email, u1_0.password, u1_0.username, u1_0.vip
from example_user u1_0
where exists (select o1_0.id from example_order o1_0 where o1_0.user_id = u1_0.id)
```

子查询是**从**外层查询上创建的，这就是两者产生关联的原因。`.not()` 变成 `not exists`；把 `exists`
换成 `in` 就是 `in` 子查询。

### 示例 5 —— 把聚合当派生表 join 进来

**输入** —— 每个商品连带它的销量，但不要每行跑一遍关联子查询。

**执行**

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

**输出**

```sql
select p1_0.name, p1_0.price, s1_0.soldAmount, s1_0.orderAmount
from example_product p1_0
join (select op1_0.product_id c0, sum(op1_0.amount) c1, count(op1_0.id) c2
      from example_order_product op1_0
      group by c0) s1_0(productId, soldAmount, orderAmount)
  on s1_0.productId = p1_0.id
order by 3 desc
```

### 示例 6 —— 用表达式更新，用子查询删除

**输入** —— 把用户名改成大写加感叹号；库存减一；删掉从没下过单的用户。

**执行**

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

**输出**

```sql
update example_user  u1_0 set username = (upper(u1_0.username)||?) where u1_0.username = ?
update example_stock s1_0 set amount = (s1_0.amount - cast(? as bigint)) where s1_0.amount > ?

delete from example_user u1_0
where not exists (select o1_0.id from example_order o1_0 where o1_0.user_id = u1_0.id)
```

### 剩下的 API，一张表看完

| 你要什么 | 怎么调 |
| --- | --- |
| 实体本身 | `dao.query()` |
| 若干列映射成 VO | `dao.query(Vo.class)` |
| 任意列 | `dao.customQuery()` → `Tuple` |
| 同上，外加总数 | `dao.page()` · `dao.customPage()` → `JpaPageResultSet` |
| Fetch 关联 | `fetch(Order::getUser)` · `leftFetch(User::getOrders).distinct()` |
| `CASE WHEN` | `new IfExpression<>(attr).when(a, b).otherwise(c)` |
| 日期部件 | `Fields.year/month/day(...)` |
| 任意数据库函数 | `Function.build("LOWER", String.class, Product::getName)` |
| 结果形状 | `Transformers.asBean/asMap/asCaseInsensitiveMap/asList/noop` |
| 原生 SQL | `dao.queryForMap(sql, args)`，`rowCount()` 和 `paginate()` 照用 |

## 7. 配置项

EasyJPA **自己没有任何配置属性** —— 没有 `spring.easyjpa.*` 命名空间。要配的是 Provider，以及三个
数据库的驱动参数。

| 配什么 | 配在哪 | 值 |
| --- | --- | --- |
| Provider | `@EnableJpaRepositories(repositoryFactoryBeanClass = …)` | `HibernateEntityDaoFactoryBean`（默认）· `EclipseLinkEntityDaoFactoryBean` · `StandardEntityDaoFactoryBean` |
| EclipseLink | `spring.autoconfigure.exclude` | `…orm.jpa.HibernateJpaAutoConfiguration` |
| SQL Server | JDBC url | `calcBigDecimalPrecision=true` —— 否则每个 `BigDecimal` 都按 `decimal(38,0)` 绑定，`0.90` 读回来变成 `1` |
| SQLite | 实体主键 | `GenerationType.IDENTITY` —— `AUTO` 要用序列表，而 SQLite 会锁死它 |
| Oracle 23+ 配 Hibernate | `OracleDialect` 子类 | `DatabaseVersion.make(21)` |

## 8. 横向对比

只比特性，不含性能数字——这里没有任何 benchmark。

| | EasyJPA | Criteria API | Spring Data `Specification` | QueryDSL |
| --- | --- | --- | --- | --- |
| 属性类型安全 | 方法引用 | 元模型或字符串 | 元模型或字符串 | 生成的 `Q` 类 |
| 需要代码生成 | 不需要 | 不需要 | 不需要 | 需要注解处理器 |
| 动态组合 | ✅ | 手工拼 `Predicate[]` | ✅ | ✅ |
| 多分支 join | 一个 join 一行 | 手工维护 `Join<?,?>` | 手工 | ✅ |
| 关联子查询 | 从外层查询上创建 | 手工 | 很别扭 | ✅ |
| `group by` 的分页计数 | 派生表，一条语句 | 自己写 | 自己写 | 自己写 |
| 派生表 join | `joinSubQuery(...)` | 手工 | ❌ | 有限 |
| 原生 SQL 保留分页 | ✅ | ❌ | ❌ | 另一套 API |
| 运行期可查 Provider 能力 | `JpaProvider` 开关 | ❌ | ❌ | ❌ |

### Provider 与数据库覆盖

同一套 202 个测试，三种 Provider × 六种数据库各跑一遍。

| | H2 2.3 | PostgreSQL 16 | MySQL 9.6 | SQL Server 16 | SQLite 3.49 | Oracle 23 |
| --- | --- | --- | --- | --- | --- | --- |
| Hibernate | ✅ | ✅ | ✅ | ⚠️ 1 | ⚠️ 3 | ⚠️ 4 |
| 纯 Criteria API | ✅ | ✅ | ✅ | ⚠️ 1 | ⚠️ 3 | ✅ |
| EclipseLink | ✅ | ✅ | ✅ | ✅ | ❌ 无平台支持 | ⚠️ 2 |

每一个 ⚠️ 都归属于数据库本身或该数据库上的 Provider，没有一个是 EasyJPA 生成的查询出了问题。
README 里逐条写明了是哪些。

## 9. 已知限制与权衡

- **别名是字符串。** `"op"` 打错是运行期报错，不是编译期。
- **不是自动装配的。** 要自己指定 factory bean，没有 `@AutoConfiguration`。
- **Criteria API 就是天花板。** 窗口函数、CTE、`UNION` 还得走原生 SQL。
- **EclipseLink 能做的比 Hibernate 少。** 没有派生表、没有 right join、子查询不能当查询列——
  这些都通过 `JpaProvider` 报出来，而不是静默兜底。
- **Fetch 集合 + 分页会在内存里分页。** 分页时只 fetch to-one 关联。
- **不支持响应式。** 只有阻塞式 JDBC。
- **不适合**以静态查询为主的项目——那种场景 Spring Data 方法名更短。

## 10. 核心结论

1. **一条链替掉十行代码。** `query().filter().sort().select().list()` 取代整套
   `CriteriaBuilder` / `Root` / `Predicate[]` 仪式。
2. **全程方法引用**，字段改名是编译期报错而不是上线后才发现。不需要代码生成，不需要注解处理器。
3. **Join 自己会分叉。** Lambda 自带所属实体，四张表两条分支的 join 就是顺着读下来的四行调用。
4. **一次分页 = 一份定义，两条语句。** 计数查询由列表查询推导而来，不可能和它漂移。
5. **分组分页数的是分组**，走派生表，一条语句，`having` 也算进去——这正是手写分页最容易算错的地方。
6. **子查询自己完成关联**，因为它是从用它的那个查询上创建的。当过滤条件、当查询列、当比较的一侧、
   或者嵌套，写法都一样。
7. **聚合只 join 一次**，作为派生表，而不是每行跑一遍关联子查询。
8. **Provider 的能力缺口是一个 `boolean`，不是一个栈底异常。**
   `JpaProviders.getProvider().supportsXxx()` 在你动手构建之前就给出答案。
9. **测试覆盖到位** —— 202 个用例、三种 Provider、六种数据库，每一个有记录的失败都归因到数据库或
   Provider，没有藏起来。
10. **零配置。** 没有属性要配；指定一个 factory bean、继承 `EntityDao`，就这两件事。

**GitHub** —— [paganini2008/easyjpa](https://github.com/paganini2008/easyjpa)，MIT 协议。
Spring Boot 4 走 `2.0.x` 线，Spring Boot 3 走 `1.0.x` 线。

<!-- 建议标签：java, springboot, jpa, hibernate, database -->
