package com.pahanaedu.billingsystem.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "customers")
public class Customer {

    @Id
    private String id; // MongoDB will generate this ID
    private String accountNumber;
    private String name;
    private String address;
    private String telephoneNumber;
    private int unitsConsumed;
}