package com.github.easyjpa.test.entity;

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
 * @Description: User
 * @Author: Fred Feng
 * @Date: 24/03/2025
 * @Version 1.0.0
 */
@DynamicInsert
@Entity
@Table(name = "example_user")
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = {"orders"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "email", nullable = true, length = 255)
    private String email;

    @Column(name = "vip", nullable = false)
    private Boolean vip;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REFRESH, orphanRemoval = true)
    private List<Order> orders;

    public User(Long id) {
        this.id = id;
    }

    public User(String username, String password, String email, Boolean vip) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.vip = vip;
    }

}

