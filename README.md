

# EasyJPA – Your Best Partner for JPA Development!

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-brightgreen?style=for-the-badge&logo=springboot)
![Hibernate](https://img.shields.io/badge/Hibernate-ORM-yellow?style=for-the-badge&logo=hibernate)
![JPA](https://img.shields.io/badge/JPA-Jakarta%20Persistence%20API-blue?style=for-the-badge)
![EasyJPA](https://img.shields.io/badge/EasyJPA-Lightweight-blueviolet?style=for-the-badge)


### It's time to say goodbye to JPA Criteria API complexity! EasyJPA makes your code sleek, simple, and powerful!

**EasyJPA** elegantly streamlines JPA's Criteria API with a fully **Lambda-expression-based** and developer-friendly API, making dynamic queries both intuitive and efficient. It significantly reduces SQL/JPQL complexity, accelerates development, and improves code readability, ensuring a clean and concise query experience.

With comprehensive support for **complex SQL queries**, **EasyJPA** enables seamless execution of **multi-table joins (INNER, LEFT, RIGHT, CROSS JOINs), subqueries, aggregations, computed columns, and filtering operations**. Its **fluent API with full Lambda expression support** allows developers to construct queries programmatically, eliminating the need for raw SQL while retaining maximum flexibility.

## Features
---------------------------
* Dynamic Queries
* Computed and Function-Based Columns
* Date Parts Rendered per Database
* Grouping and Filtering
* Sorting
* List Queries and Pagination
* Complex Subqueries
* Inner Join, Left Join, Right Join, and Cross Join
* Update and Delete Operations
* Grouping Pagination Counted by a Derived Table
* Fetch Join
* Joining a Subquery as a Derived Table
* Native SQL Operations
* Lambda Expression Support
* Hibernate by Default, EclipseLink Supported

## Examples
**1. Query All Users**

```java
@Autowired
private UserDao userDao;

@BeforeAll
public void saveRandomUsers() {
    List.of(new User("Jack", "123456", "Jack001@jpatest.com", true),
            new User("Petter", "123456", "Petter002@jpatest.com", true),
            new User("Scott", "123456", "scott003@jpatest.com", false),
            new User("Lee", "123456", "lee004@jpatest.com", false),
            new User("Terry", "123456", null, false)
           ).forEach(user -> {
                userDao.save(user);
           });
    log.info("Total users: {}", userDao.count());
}

/**
Hibernate: 
    select
        u1_0.id,
        u1_0.email,
        u1_0.password,
        u1_0.username 
    from
        example_user u1_0
**/
@Test
public void testSelectAll() {
    userDao.query()
           .selectThis()
           .list()
           .forEach(u -> {
                log.info(u.toString());
           });
}
```

**2. Basic Search Conditions**

``` java
/**
Hibernate: 
    select
        u1_0.id,
        u1_0.email,
        u1_0.password,
        u1_0.username 
    from
        example_user u1_0 
    where
        u1_0.username=? 
        and u1_0.password=?
**/
@Test
public void testGetUserByUsernameAndPassword() {
    User user = userDao.query().filter(new FilterList()
                                          .eq(User::getUsername, "Jack")
                                          .eq(User::getPassword, "123456")
                                       ).selectThis()
                                       .one();
    log.info("Load user: {}", user);
    assertTrue(user != null);
}

/**
Hibernate: 
    select
        u1_0.username,
        u1_0.password 
    from
        example_user u1_0 
    where
        u1_0.email like ? escape '' 
    offset
        ? rows 
    fetch
        first ? rows only
**/
@ParameterizedTest
@ValueSource(strings = {"scott003", "lee004"})
public void testGetUserByEmail(String email) {
    User user = userDao.query()
                       .filter(new FilterList()
                                  .like(User::getEmail, email)
                       ).select(new ColumnList(
                                   User::getUsername, 
                                   User::getPassword)
                       ).first();
    log.info("Load user: {}", user);
    assertTrue(user != null);
    
}

/**
Hibernate: 
    select
        u1_0.username,
        u1_0.password,
        u1_0.email 
    from
        example_user u1_0 
    where
        u1_0.email like ? escape '' 
    offset
        ? rows 
    fetch
        first ? rows only
**/
@ParameterizedTest
@ValueSource(strings = {"abc009"})
public void testGetUserNotFoundByEmail(String email) {
    User user = userDao.query()
                       .filter(new FilterList()
                               .like(User::getEmail, email)
                       ).select(new ColumnList(
                                    User::getUsername, 
                                    User::getPassword, 
                                    User::getEmail)
                       ).first();
   log.info("Load user: {}", user);
   assertTrue(user == null);
}

```
**3. Nested Query Conditions & Sorting & Computed Columns**

``` java
@Autowired
private ProductDao productDao;

/**
Hibernate: 
    select
        p1_0.name,
        p1_0.location,
        p1_0.price,
        (p1_0.price*p1_0.discount) 
    from
        example_product p1_0 
    where
        p1_0.price>=? 
        and (
            p1_0.location=? 
            or p1_0.location=?
        ) 
    order by
        4 desc
 **/
@Test
public void test2() {
    productDao.query(ProductVo.class)
              .filter(new FilterList()
                          .gte(Product::getPrice, BigDecimal.valueOf(200))
                              .and(() -> new FilterList()
                                         .eq(Product::getLocation, "Australia")
                                             .or()
                                         .eq(Product::getLocation, "Thailand")
                               )
              ).sort(JpaSort.desc(Fields.toInteger(4))
              ).select(new ColumnList(
                            Product::getName, 
                            Product::getLocation, 
                            Product::getPrice
                       ).addColumns(
                            Fields.multiply(Product::getPrice, Product::getDiscount).as("actualPrice"))
                       ).list().forEach(vo -> {
                            log.info(vo.toString());
                       });
}
```

**4. Grouping & Aggregation & Filtering**

```java
/**
Hibernate: 
    select
        p1_0.location,
        max(p1_0.price),
        min(p1_0.price),
        avg(p1_0.price),
        count(1) 
    from
        example_product p1_0 
    group by
        p1_0.location 
    having
        avg(p1_0.price)>? 
    order by
        4 desc 
    offset
        ? rows
**/
@Test
public void test4() {
    productDao.customQuery()
              .groupBy(new FieldList(Product::getLocation))
              .having(Restrictions.gt(Fields.avg(Product::getPrice), 50d))
              .sort(JpaSort.desc(4))
              .select(new ColumnList(Product::getLocation).addColumns(
                   Fields.max(Product::getPrice).as("maxPrice"),
                   Fields.min(Product::getPrice).as("minPrice"),
                   Fields.avg(Product::getPrice).as("avgPrice"), 
                   Fields.count(1).as("amount"))
               ).setTransformer(Transformers.asBean(ProductAggregationVo.class))
               .list().forEach(vo -> {
                    log.info(vo.toString());
               });
}
```
**5. Using Function in Columns**

```java
/**
Hibernate: 
    select
        ((max(p1_0.price)||?)||min(p1_0.price)),
        p1_0.location 
    from
        example_product p1_0 
    group by
        p1_0.location 
    offset
        ? rows
**/
@Test
public void test5() {
    productDao.customQuery()
              .groupBy("location")
              .select(new ColumnList().addColumns(
                          Fields.concat(Fields.concat(Fields.max("price", String.class), "/"),
                                        Fields.min("price", String.class)).as("repr")
                          ).addColumns(Product::getLocation))
              .setTransformer(Transformers.asBean(ProductAggregationVo.class))
              .list().forEach(vo -> {
                  log.info(vo.toString());
              });
}

/**
Hibernate: 
    select
        lower(p1_0.name),
        upper(p1_0.location) 
    from
        example_product p1_0 
    offset
        ? rows 
    fetch
        first ? rows only
**/
@Test
public void test6() {
    productDao.customQuery()
              .select(new ColumnList().addColumns(
                   Function.build("LOWER", String.class, Product::getName).as("name"),
                   Function.build("UPPER", String.class,Product::getLocation).as("location"))
              ).list(10).forEach(t -> {
                   log.info(t.toString());
              });
}

/**
Hibernate: 
    select
        case p1_0.location 
            when ? 
                then cast(? as varchar) 
            when ? 
                then cast(? as varchar) 
            when ? 
                then cast(? as varchar) 
            when ? 
                then cast(? as varchar) 
            when ? 
                then cast(? as varchar) 
            when ? 
                then cast(? as varchar) 
            when ? 
                then cast(? as varchar) 
            when ? 
                then cast(? as varchar) 
            else cast(? as varchar) 
    end,
    p1_0.location 
from
    example_product p1_0
**/
@Test
public void test7() {
     IfExpression<String, String> ifExpression = new IfExpression<String, String>(Product::getLocation)
                .when("Indonesia", "Asia")
                .when("Japan", "Asia")
                .when("China", "Asia")
                .when("Singapore", "Asia")
                .when("Vietnam", "Asia")
                .when("Thailand", "Asia")
                .when("Australia", "Oceania")
                .when("New Zealand", "Oceania")
                .otherwise("Other");
     productDao.customQuery().select(new ColumnList()
                                        .addColumns(ifExpression.as("area"))
                                        .addColumns(Product::getLocation)
                                    ).list().forEach(t -> {
                                         log.info(t.toString());
                                    });
}
```
**6. Inner Join & Pagination**

```java
@Autowired
private OrderDao orderDao;

@Autowired
private OrderProductDao orderProductDao;

/**
Hibernate: 
    select
        o1_0.id,
        o1_0.order_date,
        o1_0.total_price,
        o1_0.user_id,
        u1_0.id,
        u1_0.email,
        u1_0.password,
        u1_0.username 
    from
        example_order o1_0 
    join
        example_user u1_0 
            on u1_0.id=o1_0.user_id 
    where
        u1_0.username=? 
    order by
        o1_0.order_date desc
**/
@ParameterizedTest
@ValueSource(strings = {"Petter", "Jack"})
public void test3(String username) {
    orderDao.customQuery().join(Order::getUser, "u", null)
                          .filter(Restrictions.eq(User::getUsername, username))
                          .sort(JpaSort.desc(Order::getOrderDate))
                          .select(new ColumnList()
                                  .addFields(Fields.root())
                                  .addTableAlias("u")
                          ).list().forEach(t -> {
                              Order order = (Order) t.get(0);
                              User user = (User) t.get(1);
                              log.info("Order: " + order + ", User: " + user);
                          });
}

/**
Hibernate: 
    select
        count(1) 
    from
        (select
            1 
        from
            example_order o1_0 
        join
            example_user u1_0 
                on u1_0.id=o1_0.user_id 
        where
            o1_0.order_date between ? and ? 
        group by
            o1_0.order_date,
            u1_0.username 
        having
            avg(o1_0.total_price)>?) derived1_0(c)
Hibernate: 
    select
        u1_0.username,
        o1_0.order_date,
        avg(o1_0.total_price) 
    from
        example_order o1_0 
    join
        example_user u1_0 
            on u1_0.id=o1_0.user_id 
    where
        o1_0.order_date between ? and ? 
    group by
        o1_0.order_date,
        u1_0.username 
    having
        avg(o1_0.total_price)>? 
    order by
        o1_0.order_date desc 
    offset
        ? rows 
    fetch
        first ? rows only
**/
@Test
public void test4() {
    orderDao.customPage().join(Order::getUser, "u", null)
                         .filter(Restrictions.between(Order::getOrderDate,
                                      LocalDate.of(2025, 2, 1).atStartOfDay(),
                                      LocalDate.of(2025, 2, 28).atStartOfDay())
                          ).groupBy(new FieldList()
                                     .addFields(Order::getOrderDate)
                                     .addFields(User::getUsername)
                          ).having(Restrictions.gt(Fields.avg(Order::getTotalPrice), 20000D))
                         .sort(JpaSort.desc(Order::getOrderDate))
                         .select(new ColumnList()
                                     .addColumns(User::getUsername)
                                     .addColumns(Order::getOrderDate)
                                     .addFields(Fields.avg(Order::getTotalPrice))
                          ).setTransformer(Transformers.asCaseInsensitiveMap())
                         .paginate(PageRequest.of(5))
                         .forEachPage(eachPage -> {
                             log.info(String.format(
              "====================== PageNumber/TotalPage: %s/%s  Total Records: %s =====================",
                             eachPage.getPageNumber(), eachPage.getTotalPages(),
                             eachPage.getTotalRecords()));
                             eachPage.getContent().forEach(vo -> {
                                 log.info(vo.toString());
                             });
                          });
}

```

**7. Left Join & Pagination**

```java
/**
Hibernate: 
    select
        count(1) 
    from
        (select
            1 
        from
            example_order o1_0 
        left join
            example_order_product op1_0 
                on o1_0.id=op1_0.order_id 
        left join
            example_product p1_0 
                on p1_0.id=op1_0.product_id 
        where
            p1_0.location=?) derived1_0(c)
Hibernate: 
    select
        o1_0.id,
        o1_0.order_date,
        o1_0.total_price,
        o1_0.user_id,
        p1_0.id,
        p1_0.discount,
        p1_0.location,
        p1_0.name,
        p1_0.price,
        p1_0.produce_date 
    from
        example_order o1_0 
    left join
        example_order_product op1_0 
            on o1_0.id=op1_0.order_id 
    left join
        example_product p1_0 
            on p1_0.id=op1_0.product_id 
    order by
        o1_0.order_date desc 
    offset
        ? rows 
    fetch
        first ? rows only
**/
@Test
public void test5() {
    orderDao.customPage()
            .leftJoin(Order::getOrderProducts, "op", null)
            .leftJoin(OrderProduct::getProduct, "p", null)
            .sort(JpaSort.desc(Order::getOrderDate))
            .select(new ColumnList()
                    .addFields(Fields.root())
                    .addTableAlias("p")
            ).setTransformer(Transformers.asMap())
            .paginate(PageRequest.of(10))
            .forEachPage(eachPage -> {
                 log.info(String.format(
             "====================== PageNumber/TotalPage: %s/%s  Total Records: %s =====================",
                 eachPage.getPageNumber(), eachPage.getTotalPages(),
                 eachPage.getTotalRecords()));
                 eachPage.getContent().forEach(vo -> {
                     log.info(vo.toString());
                 });
             });
}

/**
Hibernate: 
    select
        count(1) 
    from
        (select
            1 
        from
            example_user u1_0 
        left join
            example_order o1_0 
                on u1_0.id=o1_0.user_id 
        join
            example_order_product op1_0 
                on o1_0.id=op1_0.order_id 
        join
            example_product p1_0 
                on p1_0.id=op1_0.product_id 
        where
            p1_0.discount is not null 
            and p1_0.id in ((select
                s1_0.product_id 
            from
                example_stock s1_0 
            where
                s1_0.amount>?)) 
        group by
            p1_0.name,
            p1_0.location) derived1_0(c)
Hibernate: 
    select
        p1_0.name,
        p1_0.location,
        count(p1_0.id),
        count(o1_0.id),
        sum(op1_0.amount),
        abs((((p1_0.price*p1_0.discount)*sum(op1_0.amount))-(p1_0.price*sum(op1_0.amount)))) 
    from
        example_user u1_0 
    left join
        example_order o1_0 
            on u1_0.id=o1_0.user_id 
    join
        example_order_product op1_0 
            on o1_0.id=op1_0.order_id 
    join
        example_product p1_0 
            on p1_0.id=op1_0.product_id 
    where
        p1_0.discount is not null 
        and p1_0.id in ((select
            s1_0.product_id 
        from
            example_stock s1_0 
        where
            s1_0.amount>?)) 
    group by
        p1_0.name,
        p1_0.location 
    order by
        4 desc,
        5 desc 
    offset
        ? rows 
    fetch
        first ? rows only
**/
@Test
public void test7() {
    userDao.customPage().leftJoin(User::getOrders, "o", null)
                        .join(Order::getOrderProducts, "op", null)
                        .join(OrderProduct::getProduct, "p", null)
                        .filter(Restrictions.notNull(Product::getDiscount)
                        .and(Restrictions.in(Product::getId,
                                productDao.query().subQuery(Stock.class, "s", Long.class)
                                        .filter(Restrictions.gt(Stock::getAmount, 100L))
                                        .select(Stock::getProductId))))
                        .groupBy(new FieldList(Product::getName, Product::getLocation))
                        .sort(JpaSort.desc(4), JpaSort.desc(5))
                        .select(new ColumnList()
                                    .addColumns(Product::getName, Product::getLocation)
                                    .addColumns(Fields.count(Product::getId).as("productAmount"),
                                                  Fields.count(Order::getId).as("orderAmount"),
                                         Fields.sum(OrderProduct::getAmount).as("totalAmount"), 
                                         Fields.abs(Fields.minus(
                                                Fields.multiply(
                                                        Fields.multiply(Product::getPrice,
                                                                Product::getDiscount),
                                                        Fields.sum(OrderProduct::getAmount)),
                                                Fields.multiply(Product::getPrice,
                                                        Fields.sum(OrderProduct::getAmount))))
                                        .as("savings")))
                        .setTransformer(Transformers.asMap()).paginate(PageRequest.of(10))
                        .forEachPage(eachPage -> {
                                     log.info(String.format(
           "====================== PageNumber/TotalPage: %s/%s  Total Records: %s ======================",
                                     eachPage.getPageNumber(), eachPage.getTotalPages(),
                                     eachPage.getTotalRecords()));
                                     eachPage.getContent().forEach(vo -> {
                                         log.info(vo.toString());
                                     });
                         });
    }
```

**8. Right Join & Pagination**

```java
/**
Hibernate: 
    select
        count(1) 
    from
        (select
            1 
        from
            example_order o1_0 
        right join
            example_order_product op1_0 
                on o1_0.id=op1_0.order_id 
        right join
            example_product p1_0 
                on p1_0.id=op1_0.product_id 
        where
            p1_0.location=?) derived1_0(c)
Hibernate: 
    select
        o1_0.id,
        o1_0.total_price,
        o1_0.order_date,
        op1_0.amount,
        p1_0.name,
        p1_0.location 
    from
        example_order o1_0 
    right join
        example_order_product op1_0 
            on o1_0.id=op1_0.order_id 
    right join
        example_product p1_0 
            on p1_0.id=op1_0.product_id 
    order by
        o1_0.order_date desc,
        op1_0.amount desc 
    offset
        ? rows 
    fetch
        first ? rows only
**/
@Test
public void test6() {
    orderDao.customPage()
            .rightJoin(Order::getOrderProducts, "op", null)
            .rightJoin(OrderProduct::getProduct, "p", null)
            .sort(JpaSort.desc(Order::getOrderDate), 
                  JpaSort.desc(OrderProduct::getAmount)
             ).select(new ColumnList()
                  .addColumns(Order::getId, 
                              Order::getTotalPrice, 
                              Order::getOrderDate)
                  .addColumns(OrderProduct::getAmount)
                  .addColumns(Product::getName, 
                              Product::getLocation)
             ).setTransformer(Transformers.asMap())
              .paginate(PageRequest.of(10))
              .forEachPage(eachPage -> {
                   log.info(String.format(
            "====================== PageNumber/TotalPage: %s/%s  Total Records: %s ======================",
                   eachPage.getPageNumber(), 
                   eachPage.getTotalPages(),
                   eachPage.getTotalRecords()));
                   eachPage.getContent().forEach(vo -> {
                       log.info(vo.toString());
                   });
             });
}
```

**9. Cross Join & Pagination**

``` java
/**
Hibernate: 
    select
        count(1) 
    from
        (select
            1 
        from
            example_product p1_0,
            example_stock s1_0 
        where
            s1_0.product_id=p1_0.id 
            and p1_0.location in (?, ?, ?)) derived1_0(c)
Hibernate: 
    select
        p1_0.id,
        p1_0.name,
        s1_0.amount 
    from
        example_product p1_0,
        example_stock s1_0 
    where
        s1_0.product_id=p1_0.id 
    offset
        ? rows 
    fetch
        first ? rows only
**/
@Test
public void test8() {
    productDao.customPage()
              .crossJoin(Stock.class, "a")
              .filter(new FilterList()
                      .eq(Stock::getProductId, Product::getId)
              ).select(new ColumnList()
                       .addColumns(Product::getId, 
                                   Product::getName
                       ).addColumns(Stock::getAmount)
              ).setTransformer(Transformers.asBean(ProductStockVo.class))
               .paginate(PageRequest.of(10))
               .forEachPage(eachPage -> {
                    log.info(String.format(
              "====================== PageNumber/TotalPage: %s/%s  Total Records: %s =====================",
                    eachPage.getPageNumber(), 
                    eachPage.getTotalPages(),
                    eachPage.getTotalRecords()));
                    eachPage.getContent().forEach(vo -> {
                        log.info(vo.toString());
                    });
              });
}
```

**10. Subquery &  Join**

``` java
/**
Hibernate: 
    select
        distinct o1_0.user_id 
    from
        example_order o1_0 
    where
        exists(select
            1 
        from
            example_user u2_0 
        where
            u2_0.id=o1_0.user_id)
**/
@Test
public void test1() {
    JpaQuery<Order, Tuple> jpaQuery = orderDao.customQuery();
    JpaSubQuery<User, Long> jpaSubQuery = jpaQuery.subQuery(User.class, "u", Long.class)
                                                  .filter(Restrictions.eq(User::getId, Order::getUser))
                                                  .select(Fields.toLong(1L));
    jpaQuery.filter(Restrictions.exists(jpaSubQuery))
            .distinct()
            .select(new ColumnList(Order::getUser))
            .list().forEach(m -> {
                 log.info(m.toString());
            });
}

/**
Hibernate: 
    select
        op1_0.order_id,
        op1_0.product_id,
        op1_0.amount,
        p1_0.name,
        u1_0.username 
    from
        example_order_product op1_0 
    left join
        example_product p1_0 
            on p1_0.id=op1_0.product_id 
    join
        example_order o1_0 
            on o1_0.id=op1_0.order_id 
    join
        example_user u1_0 
            on u1_0.id=o1_0.user_id 
    where
        exists(select
            p3_0.id 
        from
            example_product p3_0 
        where
            p3_0.id=op1_0.product_id 
            and p3_0.name=?) 
    offset
        ? rows 
    fetch
        first ? rows only
**/
@ParameterizedTest
@ValueSource(strings = {"Microwave oven", "Coffee maker"})
public void test2(String itemName) {
     JpaQuery<OrderProduct, Tuple> jpaQuery = orderProductDao.customQuery();
     JpaSubQuery<Product, Long> jpaSubQuery = jpaQuery.subQuery(Product.class, "p", Long.class)
                                              .filter(new FilterList()
                                              .eq(Product::getId, OrderProduct::getProduct)
                                                  .and()
                                              .eq(Product::getName, itemName)
                                               ).select(Product::getId);
     jpaQuery.leftJoin(OrderProduct::getProduct, "p", null)
             .join(Order.class, "o", null)
             .join(User.class, "u", null)
             .filter(Restrictions.exists(jpaSubQuery))
             .select(new ColumnList(
                                    OrderProduct::getOrder, 
                                    OrderProduct::getProduct,
                                    OrderProduct::getAmount
                                   ).addColumns(Product::getName)
                                    .addColumns(User::getUsername)
             ).setTransformer(Transformers.asMap())
              .list(10)
              .forEach(m -> {
                  log.info(m.toString());
              });
}
```
**11. Update with Subquery**
```java
/**
Hibernate: 
    update
        example_stock s1_0 
    set
        amount=(s1_0.amount+cast(? as integer)) 
    where
        s1_0.product_id in ((select
            p1_0.id 
        from
            example_product p1_0 
        where
            p1_0.location=?))
**/
@ParameterizedTest
@ValueSource(strings = {"Australia", "New Zealand"})
public void test9(String location) {
    JpaSubQuery<Product, Long> subQuery = stockDao.update().subQuery(Product.class, Long.class)
                .filter(Restrictions.eq(Product::getLocation, location)).select(Product::getId);
    stockDao.update()
            .setField(Stock::getAmount, Fields.plusValue(Stock::getAmount, 1000))
            .filter(Restrictions.in(Stock::getProductId, subQuery))
            .execute();
}

/**
Hibernate: 
    select
        s1_0.product_id 
    from
        example_stock s1_0 
    order by
        s1_0.amount desc 
    offset
        ? rows 
    fetch
        first ? rows only
Hibernate: 
    update
        example_product p1_0 
    set
        price=?,
        discount=?,
        produce_date=? 
    where
        p1_0.id=?
**/
@Test
public void test11() {
     Long productId = stockDao.query(Long.class)
                              .sort(JpaSort.desc(Stock::getAmount))
                              .select(new ColumnList(Stock::getProductId))
                              .first();
     int rows = productDao.update().set(Product::getPrice, BigDecimal.valueOf(1000), 
                                        Product::getDiscount,BigDecimal.valueOf(0.8f),                                                               Product::getProduceDate, LocalDate.now())
                                   .filter(Restrictions.eq(Product::getId, productId))
                                   .execute();
     log.info("Affected rows: {}", rows);
}
```

**12. Delete with Subquery**
```java
/**
Hibernate: 
    delete 
    from
        example_order o1_0 
    where
        exists(select
            op1_0.order_id 
        from
            example_order_product op1_0 
        join
            example_product p1_0 
                on p1_0.id=op1_0.product_id 
        where
            p1_0.id=op1_0.product_id 
            and p1_0.name in (?, ?))
**/
@ParameterizedTest
@CsvSource({"'Flashlight,Iron'"})
public void test7(String str) {
     String[] itemNames = str.split(",");
     JpaSubQuery<OrderProduct, Order> subQuery =
                orderDao.query()
                        .subQuery(OrderProduct.class, "o", Order.class)
                        .join(OrderProduct::getProduct, "p", null)
                        .filter(new FilterList()
                                .eq(Product::getId, OrderProduct::getProduct)
                                .in(Product::getName, List.of(itemNames)))
                        .select(OrderProduct::getOrder);
     int rows = orderDao.delete().filter(Restrictions.exists(subQuery)).execute();
     log.info("Affected rows: {}", rows);
}

/**
Hibernate: 
    delete 
    from
        example_user u1_0 
    where
        not exists(select
            o1_0.id 
        from
            example_order o1_0 
        where
            o1_0.user_id=o1_0.id)
**/
@Test
public void testDeleteUserWithoutOrder() {
     JpaSubQuery<Order, Order> subQuery = userDao.delete()
                                                 .subQuery(Order.class)
                                                 .filter(Restrictions.eq(Order::getUser, User::getId));
     int rows = userDao.delete().filter(Restrictions.exists(subQuery).not()).execute();
     log.info("Affected rows: {}", rows);
}
```

**13. Grouping Pagination & Counting**

A grouping query returns one row per group, so counting it the naive way would mean fetching all
the groups. EasyJPA counts the groups by a derived table, which always returns a single row, and
covers the having clause as well.

``` java
/**
Hibernate: 
    select
        count(1) 
    from
        (select
            1 
        from
            example_order_product op1_0 
        join
            example_product p1_0 
                on p1_0.id=op1_0.product_id 
        group by
            p1_0.name 
        having
            count(op1_0.id)>?) derived1_0(c)
**/
@Test
public void testGroupPaginationCount() throws Exception {
    JpaPageResultSet<Tuple> resultSet = orderProductDao.customPage()
                                                       .join(OrderProduct::getProduct, "p", null)
                                                       .groupBy(new FieldList()
                                                                   .addFields(Product::getName))
                                                       .having(Restrictions.gt(
                                                                   Fields.count(OrderProduct::getId), 1L))
                                                       .select(new ColumnList()
                                                                   .addColumns(Product::getName)
                                                                   .addColumns(Fields.sum(
                                                                       OrderProduct::getAmount).as("soldAmount")));
    // The total is the amount of the groups rather than the amount of the rows
    assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
}
```

**14. Best Sellers: Join & Grouping & Having & Computed Columns**

``` java
/**
Hibernate: 
    select
        p1_0.name,
        sum(op1_0.amount),
        count(distinct op1_0.order_id),
        sum((p1_0.price*op1_0.amount)) 
    from
        example_order_product op1_0 
    join
        example_product p1_0 
            on p1_0.id=op1_0.product_id 
    group by
        p1_0.name 
    having
        count(op1_0.id)>? 
    order by
        2 desc 
    offset
        ? rows
**/
@Test
public void testBestSellers() throws Exception {
    JpaPageResultSet<Tuple> resultSet = orderProductDao.customPage()
            .join(OrderProduct::getProduct, "p", null)
            .groupBy(new FieldList().addFields(Product::getName))
            .having(Restrictions.gt(count(OrderProduct::getId), 1L))
            .sort(JpaSort.desc(2))
            .select(new ColumnList().addColumns(Product::getName)
                       .addColumns(
                           sum(OrderProduct::getAmount).as("soldAmount"),
                           countDistinct(Property.forName(OrderProduct::getOrder)).as("orderAmount"),
                           sum(multiply(Property.forName(Product::getPrice),
                                        Property.forName(OrderProduct::getAmount))).as("turnover")));
    List<SalesVo> dataList = resultSet.setTransformer(Transformers.asBean(SalesVo.class)).list();
    dataList.forEach(vo -> {
        log.info(vo.toString());
    });
}
```

**15. Grouping Subquery in the Where Clause**

``` java
/**
Hibernate: 
    select
        u1_0.id,
        u1_0.email,
        u1_0.password,
        u1_0.username,
        u1_0.vip 
    from
        example_user u1_0 
    where
        u1_0.id in ((select
            o1_0.user_id 
        from
            example_order o1_0 
        group by
            o1_0.user_id 
        having
            count(o1_0.id)>?))
**/
@Test
public void testRepeatCustomers() {
    JpaQuery<User, User> query = userDao.query();
    JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class);
    subQuery.groupBy(new FieldList().addFields(Order::getUser))
            .having(Restrictions.gt(count(Order::getId), 1L))
            .select(Property.forName(Order::getUser, Long.class));
    List<User> users = query.filter(Restrictions.in(Property.forName(null, "id", Long.class),
                                                    subQuery))
                            .selectThis()
                            .list();
    users.forEach(user -> {
        log.info(user.toString());
    });
}
```

**16. Scalar Subquery as a Column**

``` java
/**
Hibernate: 
    select
        p1_0.name,
        (select
            max(s1_0.amount) 
        from
            example_stock s1_0 
        where
            s1_0.product_id=p1_0.id) 
    from
        example_product p1_0 
    order by
        p1_0.name 
    offset
        ? rows
**/
@Test
public void testStockAgainstSales() {
    JpaQuery<Product, Tuple> query = productDao.customQuery();
    JpaSubQuery<Stock, Long> stock = query.subQuery(Stock.class, "s", Long.class)
                                          .filter(Restrictions.eq(Stock::getProductId, Product::getId))
                                          .select(Fields.max(Stock::getAmount));
    List<Map<String, Object>> dataList = query.sort(JpaSort.asc(Product::getName))
            .select(new ColumnList().addColumns(Product::getName)
                       .addColumns(Column.forSubQuery(stock, "stockAmount")))
            .setTransformer(Transformers.asCaseInsensitiveMap())
            .list();
    dataList.forEach(vo -> {
        log.info(vo.toString());
    });
}
```

**17. Nested Subqueries**

``` java
/**
Hibernate: 
    select
        p1_0.id,
        p1_0.discount,
        p1_0.location,
        p1_0.name,
        p1_0.price,
        p1_0.produce_date 
    from
        example_product p1_0 
    where
        p1_0.id in ((select
            s1_0.product_id 
        from
            example_stock s1_0 
        where
            s1_0.amount>?)) 
        and p1_0.id in ((select
            op1_0.product_id 
        from
            example_order_product op1_0 
        join
            example_order o1_0 
                on o1_0.id=op1_0.order_id 
        join
            example_user u1_0 
                on u1_0.id=o1_0.user_id 
        where
            u1_0.vip=?))
**/
@Test
public void testNestedSubQueries() {
    JpaQuery<Product, Product> query = productDao.query();
    JpaSubQuery<Stock, Long> stocked = query.subQuery(Stock.class, "s", Long.class)
                                            .filter(Restrictions.gt(Stock::getAmount, 1000L))
                                            .select(Stock::getProductId);
    JpaSubQuery<OrderProduct, Long> orderedByVip = query.subQuery(OrderProduct.class, "op", Long.class);
    orderedByVip.join(OrderProduct::getOrder, "o", null)
                .join(Order::getUser, "u", null)
                .filter(Restrictions.eq(User::getVip, true));
    orderedByVip.select(Property.forName(OrderProduct::getProduct, Long.class));

    List<Product> products = query.filter(new FilterList()
                                             .in(Property.forName(null, "id", Long.class), stocked)
                                                 .and()
                                             .in(Property.forName(null, "id", Long.class), orderedByVip)
                                  ).selectThis()
                                   .list();
    products.forEach(product -> {
        log.info(product.toString());
    });
}
```

**18. Case When Expressions**

``` java
/**
Hibernate: 
    select
        case 
            when p1_0.price>=? 
                then cast(? as varchar) 
            when p1_0.price>=? 
                then cast(? as varchar) 
            else cast(? as varchar) 
    end 
    from
        example_product p1_0
**/
@Test
public void testCaseWhenExpression() {
    CaseWhenExpression<String> level = new CaseWhenExpression<String>()
              .when(Fields.gte(Product::getPrice, BigDecimal.valueOf(200)), "high")
              .when(Fields.gte(Product::getPrice, BigDecimal.valueOf(100)), "middle")
              .otherwise("low");
    productDao.customQuery()
              .select(new ColumnList().addColumns(level.as("level")))
              .list()
              .forEach(t -> {
                  log.info(t.toString());
              });
}

/**
Hibernate: 
    select
        case 
            when p1_0.location=? 
                then cast(? as varchar) 
            else cast(? as varchar) 
    end 
    from
        example_product p1_0
**/
@Test
public void testIfExpression() {
    IfExpression<String, String> area = new IfExpression<String, String>(Product::getLocation)
              .when("China", "Asia")
              .when("Japan", "Asia")
              .when("Australia", "Oceania")
              .otherwise("Other");
    productDao.customQuery()
              .select(new ColumnList().addColumns(area.as("area")))
              .list()
              .forEach(t -> {
                  log.info(t.toString());
              });
}
```

**19. Native SQL**

Whatever the Criteria API does not cover is still reachable by a native statement, which can be
paginated, mapped to a bean, to a map or by a customized RowMapper.

``` java
@Test
public void testNativeQuery() {
    String sql = "select s.amount as amount, p.name as name, p.location as location"
               + " from example_stock s join example_product p on p.id=s.product_id"
               + " where p.location = ?";

    // Mapped to a bean, the column labels are matched case insensitively
    List<StockVo> dataList = stockDao.query(sql, new Object[] {"Australia"}, StockVo.class).list();

    // Mapped to a map, and paginated
    PageableQuery<Map<String, Object>> pageableQuery = stockDao.queryForMap(sql,
                                                                new Object[] {"Australia"});
    log.info("Total records: {}", pageableQuery.rowCount());
    pageableQuery.list(10, 0).forEach(m -> {
        log.info(m.toString());
    });

    // Mapped by a customized RowMapper
    List<String> names = stockDao.query("select p.name as name from example_product p",
                                        new Object[0],
                                        (index, data) -> (String) data.get("name")).list();

    // A single value, and an update
    Number total = stockDao.getSingleResult("select sum(amount) from example_stock", null,
                                            Number.class);
    int rows = stockDao.executeUpdate("update example_stock set amount = amount + ? where amount > ?",
                                      new Object[] {1L, 0L});
}
```

**20. Fetch Join**

An association is lazy by default, so reading it afterwards runs one more statement per entity.
Fetching it loads everything at once.

``` java
/**
Hibernate: 
    select
        o1_0.id,
        o1_0.order_date,
        o1_0.status,
        o1_0.total_price,
        o1_0.user_id,
        u1_0.id,
        u1_0.email,
        u1_0.password,
        u1_0.username,
        u1_0.vip 
    from
        example_order o1_0 
    join
        example_user u1_0 
            on u1_0.id=o1_0.user_id 
    where
        o1_0.status<>?
**/
@Test
public void testFetchToOne() {
    List<Order> orders = orderDao.query()
                                 .fetch(Order::getUser)
                                 .filter(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED))
                                 .selectThis()
                                 .list();
    orders.forEach(order -> {
        // No extra statement is run here
        log.info(order.getUser().getUsername());
    });
}

/**
 * A collection is fetched by an outer join, otherwise the entities owning an empty one would be
 * dropped. Distinct the result, since one row per element would be returned otherwise.
 */
@Test
public void testLeftFetchCollection() {
    List<User> users = userDao.query()
                              .leftFetch(User::getOrders)
                              .distinct()
                              .selectThis()
                              .list();
}
```

JPA requires the entity owning the fetched association to be the one selected, and fetching a
collection together with a pagination makes the rows be paginated in memory, so prefer fetching
the to-one associations there.

**21. Join a Derived Table**

A subquery may be joined like a table, which is how every row gets its aggregate without running a
correlated subquery per row.

``` java
/**
Hibernate: 
    select
        p1_0.name,
        p1_0.price,
        s1_0.soldAmount,
        s1_0.orderAmount 
    from
        example_product p1_0 
    join
        (select
            op1_0.product_id c0, sum(op1_0.amount) c1, count(op1_0.id) c2 
        from
            example_order_product op1_0 
        group by
            c0) s1_0(productId, soldAmount, orderAmount) 
            on s1_0.productId=p1_0.id 
    order by
        3 desc
**/
@Test
public void testJoinDerivedTable() {
    JpaQuery<Product, Tuple> query = productDao.customQuery();
    JpaSubQuery<OrderProduct, Tuple> sales = query.subQuery(OrderProduct.class, "op", Tuple.class);
    sales.groupBy(new FieldList().addFields(Property.forName("op", "product.id")))
         .select(new ColumnList()
                    .addColumns(Property.forName("op", "product.id").as("productId"))
                    .addColumns(Fields.sum("op", "amount", Integer.class).as("soldAmount"),
                                Fields.count("op", "id").as("orderAmount")));

    productDao.customQuery()
              .joinSubQuery(sales, "s", Restrictions.eq(Property.forName("s", "productId"),
                                                        Property.forName("this", "id")))
              .sort(JpaSort.desc(Property.forName("s", "soldAmount")))
              .select(new ColumnList().addColumns(Product::getName, Product::getPrice)
                         .addColumns(Property.forName("s", "soldAmount").as("soldAmount"),
                                     Property.forName("s", "orderAmount").as("orderAmount")))
              .setTransformer(Transformers.asCaseInsensitiveMap())
              .list()
              .forEach(vo -> {
                  log.info(vo.toString());
              });
}
```

Every column of the subquery has to be aliased, since those aliases become the column names of the
derived table, and that is how they are addressed afterwards. `leftJoinSubQuery` keeps the rows the
derived table knows nothing about. A pagination joins the derived table into its counting query as
well, so the total stays right.

**22. Grouping by a Date Part**

The Criteria API defines no function to take the year, the month or the day out of a date, and
every database spells its own. Hibernate renders them accordingly, so the query stays the same
wherever it runs.

``` java
/**
Hibernate: 
    select
        extract(month from o1_0.order_date),
        count(o1_0.id),
        sum(o1_0.total_price) 
    from
        example_order o1_0 
    group by
        extract(month from o1_0.order_date)
**/
@Test
public void testMonthlySales() {
    orderDao.customQuery()
            .groupBy(new FieldList().addFields(Fields.month(Order::getOrderDate)))
            .select(new ColumnList()
                       .addColumns(Fields.month(Order::getOrderDate).as("month"))
                       .addColumns(count(Order::getId).as("orderAmount"),
                                   sum(Order::getTotalPrice).as("totalPrice")))
            .list()
            .forEach(tuple -> {
                log.info("Month: {}, orders: {}", tuple.get("month"), tuple.get("orderAmount"));
            });
}
```

`Function.build` is the other way round: it passes the name straight through to the database, so
`MONTH(...)` reaches MySQL and H2 while PostgreSQL knows `EXTRACT` alone. Reach for it when a
function of that database is what you are after, and for `Fields.year/month/day` when the same
query has to run on all of them.

## Best Practice

### 1. Requirements

* JDK 17 or later
* Maven 3.9 or later, or just the wrapper shipped along, `./mvnw`
* Spring Boot 3.1 or later, Spring Boot 4 included, the latest one preferred
* H2, PostgreSQL and MySQL are perfectly supported

One jar covers every Spring Boot from 3.1 up to 4.x, since the version your project manages is the
one that ends up on the classpath: the whole set of tests is run against 3.1, 3.5 and 4.1. Spring
Boot 3.0 falls short of a few things this library builds on, namely a derived table joined by an on
condition and the parts of a date, so 3.1 is where it starts.

On EclipseLink there is one version to watch: Spring Boot 4 brings Jakarta Persistence 3.2, which
asks for EclipseLink 5.0 or later, while Spring Boot 3 stays on 3.1 and asks for EclipseLink 4.
Neither of them is dragged in by this library, the one you declare is the one used.

### 2. Set it up in three steps

**Step 1** – add the dependency

``` xml
<dependency>
    <groupId>com.github.paganini2008</groupId>
    <artifactId>easyjpa-spring-boot-starter</artifactId>
    <version>1.0.0</version>  <!-- use the latest version here -->
</dependency>
```

**Step 2** – make EasyJPA the repository implementation of Spring Data, naming the provider you
run on

``` java
@EntityScan(basePackages = {"com.github.easyjpa.test.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class,
        basePackages = {"com.github.easyjpa.test.dao"})
@Configuration(proxyBeanMethods = false)
public class JpaConfig {

}
```

Whichever factory bean is named there is the provider the whole library talks to:

| Factory bean | Provider |
| --- | --- |
| `HibernateEntityDaoFactoryBean` | Hibernate, the one Spring Boot brings along |
| `EclipseLinkEntityDaoFactoryBean` | EclipseLink |
| `StandardEntityDaoFactoryBean` | the Criteria API alone, whatever runs underneath |

Nothing else has to be configured, and section 7 of the Best Practice tells what each of them
reaches. On EclipseLink, add the dependency and put the entity manager together yourself, since
Spring Boot autoconfigures Hibernate alone:

``` xml
<dependency>
    <groupId>org.eclipse.persistence</groupId>
    <artifactId>org.eclipse.persistence.jpa</artifactId>
    <version>4.0.4</version>
</dependency>
```

``` java
@Bean
public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
    factoryBean.setDataSource(dataSource);
    factoryBean.setPackagesToScan("com.github.easyjpa.test.entity");
    factoryBean.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
    return factoryBean;
}
```

``` properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

**Step 3** – let every DAO extend `EntityDao`, which is a `JpaRepository` with the EasyJPA APIs on
top of it. Nothing else has to be implemented.

``` java
public interface UserDao extends EntityDao<User, Long> {

}

public interface OrderDao extends EntityDao<Order, Long> {

}
```

Your entities stay the plain JPA ones:

``` java
@Entity
@Table(name = "example_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts;

    ...
}
```

### 3. Pick the right entry point

| What you need | Entry point | What it returns |
| --- | --- | --- |
| The entities themselves | `dao.query()` | the entity type |
| Some columns mapped to a VO | `dao.query(Vo.class)` | the given type |
| Any columns, whatever they are | `dao.customQuery()` | a `Tuple` |
| The same, but with a total count | `dao.page()` / `dao.page(Vo.class)` / `dao.customPage()` | a `JpaPageResultSet` |
| An update or a delete | `dao.update()` / `dao.delete()` | the affected rows |
| Something the Criteria API misses | `dao.query(sql, args)` and its friends | a `PageableQuery` |

A pagination is a query plus a counting query, so build it once and take both from it:

``` java
JpaPageResultSet<Tuple> resultSet = orderDao.customPage()
        .join(Order::getUser, "u", null)
        .filter(Restrictions.gt(Order::getTotalPrice, BigDecimal.valueOf(1000)))
        .select(new ColumnList().addColumns(Order::getId).addColumns(User::getUsername));

long total = resultSet.rowCount();          // one statement, no rows fetched
List<Tuple> rows = resultSet.list(10, 0);   // the first 10 rows
```

### 4. Name every join, and address the attributes by the name

An alias is what tells EasyJPA which table an attribute belongs to. Give one to every join and
every subquery, then address the attributes in any of these three ways:

``` java
Restrictions.eq(Product::getName, "Juicer")        // by a lambda, the alias is resolved for you
Restrictions.eq("name", "Juicer")                  // by an attribute name, on the root entity
Restrictions.eq("p", "name", "Juicer")             // by an alias and an attribute name
```

A lambda names no table, it only names the entity the attribute belongs to, so EasyJPA looks the
alias up in the statement being built, taking the nearest table of that entity. The lookup never
leaves the statement, which means an outer query and its subquery tell their tables apart even
when both of them query the same entity:

``` java
JpaQuery<Stock, Stock> query = stockDao.query();
JpaSubQuery<Stock, Long> maxAmount = query.subQuery(Stock.class, "s2", Long.class)
        .select(Fields.max(Stock::getAmount));            // resolved against the subquery
query.filter(Restrictions.gte(Stock::getAmount, maxAmount))   // resolved against the outer query
     .selectThis()
     .list();
```

The root entity is always aliased `this`, and a blank alias means the same, so
`Property.forName(null, "price")` and `Property.forName("this", "price")` are one and the same
thing. When one entity is joined twice, as in a self join, name the tables apart:

``` java
// The orders which are cheaper than another order of the same user
orderDao.customQuery().crossJoin(Order.class, "o2")
        .filter(new FilterList()
                   .eq(Property.forName("o2", "user"), Property.forName("this", "user")).and()
                   .gt(Property.forName("o2", "totalPrice", BigDecimal.class),
                       Property.forName("this", "totalPrice", BigDecimal.class)))
        .select(new ColumnList().addColumns(Property.forName("this", "id").as("orderId"),
                                            Property.forName("o2", "id").as("otherOrderId")))
        .list();
```

### 5. Build a subquery from the statement which uses it

A subquery belongs to the statement it is created from, and it may be referred to before or after
it is filtered, so keep a reference to it:

``` java
JpaQuery<User, User> query = userDao.query();
JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
        .filter(Restrictions.eq(Order::getUser, User::getId))
        .select(Order::getId);
List<User> customers = query.filter(Restrictions.exists(subQuery)).selectThis().list();
```

Build the columns and the conditions after the joins they refer to are in place, since a join is
what gives its table a name.

### 6. Join by a lambda, and the branch takes care of itself

A lambda carries the entity the attribute belongs to, so every join starts from that entity, no
matter which one the previous join reached. Two branches growing from the same table are written
exactly the way they read:

``` java
orderProductDao.customPage()
        .join(OrderProduct::getOrder, "o", null)      // OrderProduct -> Order
        .join(Order::getUser, "u", null)              // Order        -> User
        .join(OrderProduct::getProduct, "p", null)    // OrderProduct -> Product, a second branch
```

``` sql
from example_order_product op1_0
join example_order o1_0 on o1_0.id=op1_0.order_id
join example_user u1_0 on u1_0.id=o1_0.user_id
join example_product p1_0 on p1_0.id=op1_0.product_id
```

The branch may be an outer join as well, which no cross join could ever express:

``` java
.leftJoin(OrderProduct::getProduct, "p", null)
```

When the attributes are addressed by their names instead of by lambdas, name the table the join
grows from:

``` java
.join("this", "product", "p", null)     // join from the root entity
.join("o", "user", "u", null)           // join from the table aliased o
```

Joining by an entity class branches as well. The class is looked for in the entity the last join
reached, and then in the ones before it, so the association is found wherever it lives:

``` java
orderProductDao.customQuery()
        .join(Order.class, "o", null)      // found on OrderProduct
        .join(User.class, "u", null)       // found on Order
        .join(Product.class, "p", null)    // not on User, not on Order, found back on OrderProduct
```

It takes the nearest association of that type, so when one entity holds two associations of the
same type, address them by a lambda or by their names instead.

Two tables having no association at all are still joined by a cross join and correlated by the
where clause:

``` java
productDao.customPage().crossJoin(Stock.class, "s")
          .filter(new FilterList().eq(Stock::getProductId, Product::getId))
```

An on condition is applied to the join it belongs to, so address the joined table by its alias:

``` java
orderDao.customQuery().leftJoin(Order::getOrderProducts, "op",
                                Restrictions.gt("op", "amount", 10))
```

### 7. Know what your provider reaches

Hibernate is the default and the one every feature is built for. EclipseLink is supported as far as
it goes, and what it does not reach is said so rather than failed over silently:

| | Hibernate | EclipseLink |
| --- | --- | --- |
| Query, join, filter, sort, group, having | yes | yes |
| Pagination and its counting | by a derived table | by a count(distinct ...) |
| Counting a grouping query with having | one statement | one row per group |
| Subquery as a filter (in, exists) | yes | yes |
| Subquery as one side of a comparison | yes | quantify it by `Fields.all/any/some` |
| Subquery as a selected column | yes | no |
| Joining a subquery as a derived table | yes | no |
| Right join | yes | no |
| Selecting an entity column by column | yes | no, select a Tuple or a bean |
| Filling a bean by its properties | yes | no, its constructor is used |
| Selecting a comparison as a boolean column | yes | no |
| Sorting by a column position | yes | no |
| A function of the database passed through | yes | no |
| year(), month(), day() | yes | no, the Criteria API defines none |

Ask before you build, whenever you write for both:

``` java
if (JpaProviders.getProvider().supportsDerivedTable()) {
    ...
}
```

### 8. Name the subquery of an update or a delete

An update and a delete correlate their subqueries just as a query does, and the subquery gets a
table name of its own, taken from the entity unless another one is given:

``` java
JpaDelete<Product> delete = productDao.delete();
JpaSubQuery<Stock, Long> subQuery = delete.subQuery(Stock.class, "s", Long.class)
        .filter(Restrictions.eq(Stock::getProductId, Product::getId))   // s.product_id = product.id
        .select(Stock::getId);
delete.filter(Restrictions.exists(subQuery).not()).execute();
```

Whatever the subquery selects out of an association, spell the key out, since that is what every
provider renders the same way:

``` java
.select(Property.forName("o", "user.id", Long.class))   // rather than Order::getUser
```

### 9. Fetch what you are going to read

An association read after the query costs one statement per entity. Fetch it along with the
entities instead:

``` java
orderDao.query().fetch(Order::getUser).selectThis().list();          // to-one
userDao.query().leftFetch(User::getOrders).distinct().selectThis().list();   // collection
```

The entity owning the association has to be the one selected, and a collection fetched together
with a pagination is paginated in memory, so keep the pagination to the to-one associations.

### 10. Join an aggregate as a derived table

When every row needs an aggregate of another table, join that aggregate instead of computing it
per row. See example 21.

### 11. Count a grouping pagination without fetching the groups

`rowCount()` counts the groups by a derived table, so a grouping pagination costs one statement no
matter how many groups there are, and a having clause is counted correctly as well. See example 13.

### 12. Transform the result into whatever your API returns

``` java
.setTransformer(Transformers.asMap())                   // Map<String, Object>
.setTransformer(Transformers.asCaseInsensitiveMap())    // the keys are case insensitive
.setTransformer(Transformers.asList())                  // List<Object>
.setTransformer(Transformers.asBean(SalesVo.class))     // a VO, matched by the column aliases
.setTransformer(Transformers.noop())                    // keep it as it is
```

Give every computed column an alias, since that is what a bean property or a map key is matched by:

``` java
.addColumns(Fields.sum(OrderProduct::getAmount).as("soldAmount"))
```

### 13. Fall back to native SQL, and keep the pagination

Whatever the Criteria API does not reach is still one call away, and the result is mapped case
insensitively, so the column labels may be cased however the database likes. See example 19.

## Running the Tests

The tests run against H2 out of the box, and the same set of them runs on every provider and every
database. Which one is picked is a matter of the active profile alone.

No Maven installation is needed, since the wrapper fetches the one this project builds with:
`./mvnw` on Linux and macOS, `mvnw.cmd` on Windows. Wherever `mvn` is written below, `./mvnw` does
the same.

``` bash
mvn test                                                # Hibernate, H2
mvn test -Dspring.profiles.active=postgresql            # Hibernate, PostgreSQL
mvn test -Dspring.profiles.active=mysql                 # Hibernate, MySQL
mvn test -Dspring.profiles.active=standard              # the Criteria API alone
mvn test -Dspring.profiles.active=standard,postgresql
mvn test -Peclipselink                                  # EclipseLink
mvn test -Peclipselink -Dtest.profiles=eclipselink,mysql
```

In an IDE, run any test class right away for Hibernate and H2, and add the profiles to the VM
options for anything else:

```
-Dspring.profiles.active=postgresql
-Dspring.profiles.active=standard
-Dspring.profiles.active=eclipselink
-Dspring.profiles.active=eclipselink,postgresql
```

`mvn test -Peclipselink` goes one step further than the profile does: it keeps Hibernate out of the
test classpath altogether, which is how EclipseLink is proven to stand on its own.

What a provider does not reach is skipped rather than failed, so a run on EclipseLink reports its
skips, and `UtilsTests#testProviderCapabilities` prints the whole list of what the provider at hand
can do.

| Provider | Database | Tests | Skipped |
| --- | --- | --- | --- |
| Hibernate | H2, PostgreSQL, MySQL | 201 | 0 |
| Criteria API alone | H2, PostgreSQL, MySQL | 201 | 6 |
| EclipseLink | H2, PostgreSQL, MySQL | 201 | 35 |

## Contribution and License

This project is open source and licensed under the **MIT License**.

## Project Link

For more information, visit the **EasyJPA GitHub repository**: [paganini2008/easyjpa](https://github.com/paganini2008/easyjpa).