// src/main/java/com/pahanaedu/billingsystem/PahanaEduBillingSystemApplication.java
package com.pahanaedu.billingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.pahanaedu.billingsystem.model.User;
import com.pahanaedu.billingsystem.repository.UserRepository;

@SpringBootApplication
public class PahanaEduBillingSystemApplication implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(PahanaEduBillingSystemApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Initialize an admin user if not exists
        if (userRepository.findByMobileNumber("admin").isEmpty()) {
            User adminUser = new User();
            adminUser.setMobileNumber("admin");
            adminUser.setPassword(passwordEncoder.encode("admin123")); // Default admin password
            adminUser.setRole("ADMIN");
            userRepository.save(adminUser);
            System.out.println("Default admin user created: admin/admin123");
        }

        // Initialize a regular user if not exists (for testing user login)
        if (userRepository.findByMobileNumber("user").isEmpty()) {
            User normalUser = new User();
            normalUser.setMobileNumber("user");
            normalUser.setPassword(passwordEncoder.encode("user123")); // Default user password
            normalUser.setRole("USER");
            userRepository.save(normalUser);
            System.out.println("Default regular user created: user/user123");
        }
    }
}
```java
// src/main/java/com/pahanaedu/billingsystem/config/SecurityConfig.java
package com.pahanaedu.billingsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.beans.factory.annotation.Autowired;
import com.pahanaedu.billingsystem.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Configure AuthenticationManager to use our CustomUserDetailsService
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder())
                .and()
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // Disable CSRF for simplicity in this example; enable in production
                .authorizeRequests(authorize -> authorize
                        .requestMatchers("/", "/index.html", "/user_login.html", "/admin_login.html", "/user_register.html", "/style.css").permitAll() // Allow access to static files and login/register pages
                        .requestMatchers("/api/auth/**").permitAll() // Allow registration and login API calls
                        .requestMatchers("/api/customers").permitAll() // Allow customer registration without authentication
                        .requestMatchers("/api/customers/**").authenticated() // Restrict customer details endpoints to authenticated users
                        .requestMatchers("/user_dashboard.html").hasAnyRole("USER", "ADMIN") // User dashboard accessible by USER and ADMIN
                        .requestMatchers("/admin_dashboard.html").hasRole("ADMIN") // Admin dashboard accessible only by ADMIN
                        .anyRequest().authenticated() // All other requests require authentication
                )
                .formLogin() // Enable form-based login (Spring Security handles redirect to /login by default)
                .loginPage("/user_login.html") // Custom login page for users
                .loginProcessingUrl("/perform_user_login") // URL to which login form is submitted (Spring Security handles this)
                .defaultSuccessUrl("/user_dashboard.html", true) // Redirect to user dashboard on successful user login
                .failureUrl("/user_login.html?error=true") // Redirect to user login with error on failure
                .and()
                .logout()
                .logoutUrl("/perform_logout") // URL to logout (Spring Security handles this)
                .logoutSuccessUrl("/index.html?logout=true") // Redirect to index on successful logout
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .and()
                .httpBasic(); // Enable HTTP Basic Authentication for REST clients if needed

        return http.build();
    }
}
```java
// src/main/java/com/pahanaedu/billingsystem/config/WebConfig.java
package com.pahanaedu.billingsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Apply CORS to all /api endpoints
                .allowedOrigins("*") // Allow all origins for development (restrict in production)
                .allowedMethods("GET", "POST", "PUT", "DELETE") // Allowed HTTP methods
                .allowedHeaders("*"); // Allowed headers
    }
}
```java
// src/main/java/com/pahanaedu/billingsystem/model/User.java
package com.pahanaedu.billingsystem.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data; // From Lombok dependency, generates getters, setters, toString, equals, hashCode

@Data // Lombok annotation
@Document(collection = "users") // Maps this class to the "users" collection in MongoDB
public class User {

    @Id // Marks this field as the primary key in MongoDB
    private String id;
    private String mobileNumber; // Used as the username for login
    private String password;
    private String role; // e.g., "USER", "ADMIN"
}
```java
// src/main/java/com/pahanaedu/billingsystem/model/Customer.java
package com.pahanaedu.billingsystem.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data; // From Lombok dependency

@Data // Lombok annotation
@Document(collection = "customers") // Maps this class to the "customers" collection in MongoDB
public class Customer {

    @Id // MongoDB will automatically generate this ID if not set
    private String id;
    private String accountNumber; // Unique identifier for the customer
    private String name;
    private String address;
    private String telephoneNumber;
    private int unitsConsumed;
}
```java
// src/main/java/com/pahanaedu/billingsystem/repository/UserRepository.java
package com.pahanaedu.billingsystem.repository;

import com.pahanaedu.billingsystem.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

// MongoRepository provides standard CRUD operations.
// <User, String>: User is the entity type, String is the type of its @Id field.
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByMobileNumber(String mobileNumber); // Custom query method to find a user by mobile number
}
```java
// src/main/java/com/pahanaedu/billingsystem/repository/CustomerRepository.java
package com.pahanaedu.billingsystem.repository;

import com.pahanaedu.billingsystem.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CustomerRepository extends MongoRepository<Customer, String> {
    Optional<Customer> findByAccountNumber(String accountNumber); // Find a customer by their unique account number
}
```java
// src/main/java/com/pahanaedu/billingsystem/service/CustomUserDetailsService.java
package com.pahanaedu.billingsystem.service;

import com.pahanaedu.billingsystem.model.User;
import com.pahanaedu.billingsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

// This service is crucial for Spring Security to load user details from MongoDB
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String mobileNumber) throws UsernameNotFoundException {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with mobile number: " + mobileNumber));

        // Return Spring Security's User object
        return new org.springframework.security.core.userdetails.User(
                user.getMobileNumber(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())) // Spring Security expects roles with "ROLE_" prefix
        );
    }
}
```java
// src/main/java/com/pahanaedu/billingsystem/service/UserService.java
package com.pahanaedu.billingsystem.service;

import com.pahanaedu.billingsystem.model.User;
import com.pahanaedu.billingsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Set default role if not provided
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        return userRepository.save(user);
    }

    public Optional<User> findByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber);
    }
}
```java
// src/main/java/com/pahanaedu/billingsystem/service/CustomerService.java
package com.pahanaedu.billingsystem.service;

import com.pahanaedu.billingsystem.model.Customer;
import com.pahanaedu.billingsystem.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Optional<Customer> getCustomerByAccountNumber(String accountNumber) {
        return customerRepository.findByAccountNumber(accountNumber);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer updateCustomer(String id, Customer customerDetails) {
        Optional<Customer> existingCustomerOptional = customerRepository.findById(id);
        if (existingCustomerOptional.isPresent()) {
            Customer existingCustomer = existingCustomerOptional.get();
            existingCustomer.setAccountNumber(customerDetails.getAccountNumber());
            existingCustomer.setName(customerDetails.getName());
            existingCustomer.setAddress(customerDetails.getAddress());
            existingCustomer.setTelephoneNumber(customerDetails.getTelephoneNumber());
            existingCustomer.setUnitsConsumed(customerDetails.getUnitsConsumed());
            return customerRepository.save(existingCustomer);
        } else {
            throw new RuntimeException("Customer not found with id " + id);
        }
    }

    public void deleteCustomer(String id) {
        customerRepository.deleteById(id);
    }
}
```java
// src/main/java/com/pahanaedu/billingsystem/controller/AuthController.java
package com.pahanaedu.billingsystem.controller;

import com.pahanaedu.billingsystem.model.User;
import com.pahanaedu.billingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
        import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        Optional<User> existingUser = userService.findByMobileNumber(user.getMobileNumber());
        if (existingUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User with this mobile number already exists.");
        }
        userService.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> authenticateUser(@RequestBody Map<String, String> loginRequest) {
        String mobileNumber = loginRequest.get("mobileNumber");
        String password = loginRequest.get("password");

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(mobileNumber, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details to return role information
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().stream()
                    .filter(a -> a.getAuthority().startsWith("ROLE_"))
                    .map(a -> a.getAuthority().substring(5)) // Remove "ROLE_" prefix
                    .findFirst()
                    .orElse("UNKNOWN"); // Default role if none found

            return ResponseEntity.ok("Login successful! Role: " + role);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }
}
```java
// src/main/java/com/pahanaedu/billingsystem/controller/CustomerController.java
package com.pahanaedu.billingsystem.controller;

import com.pahanaedu.billingsystem.model.Customer;
import com.pahanaedu.billingsystem.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // For role-based access control
import org.springframework.web.bind.annotation.*;
        import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // This endpoint is for adding new customers (can be done without login, e.g., by a guest user registering for a new account)
    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        Optional<Customer> existingCustomer = customerService.getCustomerByAccountNumber(customer.getAccountNumber());
        if (existingCustomer.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // Account number already exists
        }
        Customer newCustomer = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(newCustomer);
    }

    // Endpoint for users (customers) to view their own details
    // Assuming mobile number from authenticated user can map to customer's telephone number or account number
    // For simplicity, we'll fetch by account number from request for now, or you can integrate with logged-in user context
    @GetMapping("/account/{accountNumber}")
    // @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Example of securing the endpoint
    public ResponseEntity<Customer> getCustomerByAccountNumber(@PathVariable String accountNumber) {
        Optional<Customer> customer = customerService.getCustomerByAccountNumber(accountNumber);
        return customer.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Admin-specific endpoints
    @GetMapping // Get all customers (Admin only)
    @PreAuthorize("hasRole('ADMIN')") // Only ADMINs can access this
    public ResponseEntity<List<Customer>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    @PutMapping("/{id}") // Update customer by ID (Admin only)
    @PreAuthorize("hasRole('ADMIN')") // Only ADMINs can access this
    public ResponseEntity<Customer> updateCustomer(@PathVariable String id, @RequestBody Customer customerDetails) {
        try {
            Customer updatedCustomer = customerService.updateCustomer(id, customerDetails);
            return ResponseEntity.ok(updatedCustomer);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/{id}") // Delete customer by ID (Admin only)
    @PreAuthorize("hasRole('ADMIN')") // Only ADMINs can access this
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
```xml
        <!-- pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version> <!-- Use a stable Spring Boot version -->
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.pahanaedu</groupId>
    <artifactId>billing-system</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>PahanaEduBillingSystem</name>
<description>Billing System for Pahana Edu</description>
    <properties>
        <java.version>17</java.version> <!-- Ensure this matches your JDK version -->
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
        ```properties
# src/main/resources/application.properties
server.port=8080

        # MongoDB Atlas Connection URI
# REPLACE <username> and <password> with your MongoDB Atlas credentials
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@cluster0.abcde.mongodb.net/pahanadb?retryWrites=true&w=majority

        # Spring Security Configuration (for initial setup, will be overridden by SecurityConfig.java)
# These are default, but CustomUserDetailsService will take over
spring.security.user.name=
spring.security.user.password=
spring.security.user.roles=

        # To allow Spring Security to interpret roles from the User model (e.g., "USER" -> ROLE_USER)
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=
