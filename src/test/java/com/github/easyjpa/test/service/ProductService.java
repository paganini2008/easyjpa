package com.github.easyjpa.test.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.easyjpa.test.dao.ProductDao;
import com.github.easyjpa.test.dao.StockDao;
import com.github.easyjpa.test.entity.Product;
import com.github.easyjpa.test.entity.Stock;

/**
 * 
 * @Description: ProductService
 * @Author: Fred Feng
 * @Date: 23/03/2025
 * @Version 1.0.0
 */
@Transactional
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductDao productDao;

    @Autowired
    private StockDao stockDao;

    /**
     * Random save some products
     */
    public void saveRandomProducts() {
        List<Product> list = List.of(
                new Product("Juicer", BigDecimal.valueOf(80), BigDecimal.valueOf(0.9),
                        LocalDate.of(2025, 1, 10), "Indonesia"),
                new Product("Electric fan", BigDecimal.valueOf(30), null, LocalDate.of(2025, 2, 10),
                        "Singapore"),
                new Product("Washing machine", BigDecimal.valueOf(120), BigDecimal.valueOf(0.9),
                        LocalDate.of(2025, 3, 10), "China"),
                new Product("Coffee maker", BigDecimal.valueOf(150), null,
                        LocalDate.of(2025, 2, 27), "Australia"),
                new Product("Hair dryer", BigDecimal.valueOf(110), null, LocalDate.of(2024, 12, 10),
                        "Japan"),
                new Product("Shaver", BigDecimal.valueOf(60), BigDecimal.valueOf(0.85),
                        LocalDate.of(2024, 12, 27), "Australia"),
                new Product("Rice cooker", BigDecimal.valueOf(220), BigDecimal.valueOf(0.9),
                        LocalDate.of(2025, 1, 27), "Australia"),
                new Product("Microwave oven", BigDecimal.valueOf(320), BigDecimal.valueOf(0.9),
                        LocalDate.of(2025, 1, 5), "New Zealand"),
                new Product("Iron", BigDecimal.valueOf(25), null, LocalDate.of(2024, 12, 27),
                        "China"),
                new Product("Flashlight", BigDecimal.valueOf(10), null, LocalDate.of(2025, 3, 5),
                        "Vietnam"),
                new Product("Electric heater", BigDecimal.valueOf(256), BigDecimal.valueOf(0.8),
                        LocalDate.of(2024, 12, 20), "Thailand"),
                new Product("Water dispenser", BigDecimal.valueOf(101), BigDecimal.valueOf(0.85),
                        LocalDate.of(2024, 12, 5), "Thailand"));
        list = productDao.saveAll(list);
        log.info("Total products: {}", productDao.count());

        List<Stock> stocks = new ArrayList<>();
        for (Product product : list) {
            Stock stock = new Stock();
            stock.setAmount(99999L);
            stock.setProductId(product.getId());
            stocks.add(stock);
        }
        stockDao.saveAll(stocks);
    }

}
