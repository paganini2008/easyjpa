package com.github.easyjpa.test.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.easyjpa.Fields;
import com.github.easyjpa.Restrictions;
import com.github.easyjpa.test.dao.OrderDao;
import com.github.easyjpa.test.dao.OrderProductDao;
import com.github.easyjpa.test.dao.ProductDao;
import com.github.easyjpa.test.dao.StockDao;
import com.github.easyjpa.test.dao.UserDao;
import com.github.easyjpa.test.entity.Order;
import com.github.easyjpa.test.entity.OrderProduct;
import com.github.easyjpa.test.entity.OrderStatus;
import com.github.easyjpa.test.entity.Product;
import com.github.easyjpa.test.entity.Stock;
import com.github.easyjpa.test.entity.User;

/**
 * 
 * UserOrderService is used to create some test data for mocking business scenario purpose.
 * 
 * @Author: Fred Feng
 * @Date: 21/03/2025
 * @Version 1.0.0
 */
@Transactional
@Service
public class UserOrderService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private StockDao stockDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderProductDao orderProductDao;

    /**
     * Random make some orders with random products
     */
    public void makeRandomOrders() {
        List<User> users = userDao.findAll();
        List<Product> products = productDao.findAll();
        for (User user : users) {
            for (int i = 0, n = ThreadLocalRandom.current().nextInt(1, 10); i < n; i++) {
                makeOrder(user.getId(), randomShoppingCart(products));
            }
        }
    }

    public void makeOrder(Long userId, Map<String, Integer> items) {
        User user = userDao.getReferenceById(userId);
        List<OrderProduct> orderProducts = new ArrayList<OrderProduct>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemName = entry.getKey();
            Product product = productDao.findOne(Restrictions.eq(Product::getName, itemName)).get();
            totalPrice =
                    totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(entry.getValue()))
                            .multiply(product.getDiscount() != null ? product.getDiscount()
                                    : BigDecimal.ONE));
            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setAmount(entry.getValue());
            orderProduct.setProduct(product);
            orderProducts.add(orderProduct);
            stockDao.update()
                    .setField(Stock::getAmount,
                            Fields.minusValue(Stock::getAmount, Long.valueOf(entry.getValue())))
                    .filter(Restrictions.eq(Stock::getProductId, product.getId())).execute();
        }
        Order order = new Order();
        order.setOrderDate(randomLocalDate().atStartOfDay());
        order.setStatus(OrderStatus.values()[orderSeq++ % OrderStatus.values().length]);
        order.setTotalPrice(totalPrice);
        order.setUser(user);
        Order ref = orderDao.save(order);

        orderProducts.forEach(op -> op.setOrder(ref));
        orderProductDao.saveAll(orderProducts);
    }

    private int orderSeq = 0;

    private LocalDate randomLocalDate() {
        int month = ThreadLocalRandom.current().nextInt(1, 3);
        int dayOfMonth = ThreadLocalRandom.current().nextInt(20, 28);
        return LocalDate.of(2025, month, dayOfMonth);
    }

    private Map<String, Integer> randomShoppingCart(List<Product> products) {
        Map<String, Integer> items = new HashMap<String, Integer>();
        for (int i = 0, n = ThreadLocalRandom.current().nextInt(1, 100); i < n; i++) {
            int index = ThreadLocalRandom.current().nextInt(products.size());
            Product product = products.get(index);
            if (items.containsKey(product.getName())) {
                int amount = items.get(product.getName());
                amount += ThreadLocalRandom.current().nextInt(1, 20);
                items.put(product.getName(), amount);
            } else {
                items.put(product.getName(), ThreadLocalRandom.current().nextInt(1, 20));
            }
        }
        return items;
    }

}
