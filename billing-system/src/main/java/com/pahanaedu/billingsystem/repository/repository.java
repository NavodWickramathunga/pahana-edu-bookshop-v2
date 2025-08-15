package com.pahanaedu.billingsystem.repository;

import com.pahanaedu.billingsystem.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByMobileNumber(String mobileNumber);
}