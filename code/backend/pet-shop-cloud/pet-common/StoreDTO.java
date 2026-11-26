package com.petshop.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class StoreDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String avatar;
}
