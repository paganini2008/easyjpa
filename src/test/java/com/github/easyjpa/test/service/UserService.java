package com.github.easyjpa.test.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.easyjpa.test.dao.UserDao;
import com.github.easyjpa.test.entity.User;

/**
 * 
 * @Description: UserService
 * @Author: Fred Feng
 * @Date: 23/03/2025
 * @Version 1.0.0
 */
@Transactional
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserDao userDao;

    /**
     * Random save some users
     */
    public void saveRandomUsers() {
        List.of(new User("Jack", "123456", "Jack001@jpatest.com", true),
                new User("Petter", "123456", "Petter002@jpatest.com", true),
                new User("Scott", "123456", "scott003@jpatest.com", false),
                new User("Lee", "123456", "lee004@jpatest.com", false),
                new User("Terry", "123456", null, false)).forEach(user -> {
                    userDao.save(user);
                });
        log.info("Total users: {}", userDao.count());
    }

}
