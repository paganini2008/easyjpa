package com.github.easyjpa.test.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.annotations.DynamicInsert;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @Description: Product
 * @Author: Fred Feng
 * @Date: 19/03/2025
 * @Version 1.0.0
 */
@DynamicInsert
@Entity
@Table(name = "example_product")
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = "orderProducts")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "price", nullable = false, precision = 11, scale = 2)
    private BigDecimal price;

    @Column(name = "discount", nullable = true, precision = 5, scale = 2)
    private BigDecimal discount;

    @Column(name = "produce_date", nullable = false)
    private LocalDate produceDate;

    @Column(name = "location", nullable = true, length = 45)
    private String location;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts;

    public Product(String name, BigDecimal price, BigDecimal discount, LocalDate produceDate,
            String location) {
        this.name = name;
        this.price = price;
        this.discount = discount;
        this.produceDate = produceDate;
        this.location = location;
    }

}
