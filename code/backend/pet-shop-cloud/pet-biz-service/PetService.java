package com.petshop.biz.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.biz.client.CustomerClient;
import com.petshop.biz.entity.Pet;
import com.petshop.biz.mapper.PetMapper;
import com.petshop.common.api.Result;
import com.petshop.common.dto.CustomerDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PetService {

    @Autowired
    private PetMapper petMapper;

    @Autowired
    private CustomerClient customerClient;

    public PageInfo<Pet> page(int pageNum, int pageSize, String name, Long categoryId, Long customerId) {
        PageHelper.startPage(pageNum, pageSize);
        List<Pet> list = petMapper.list(name, categoryId, customerId);
        fillCustomerNames(list);
        return new PageInfo<>(list);
    }

    public Pet getById(Long id) {
        Pet pet = petMapper.getById(id);
        if (pet != null) {
            fillCustomerNames(Collections.singletonList(pet));
        }
        return pet;
    }

    public boolean add(Pet pet) {
        return petMapper.insert(pet) > 0;
    }

    public boolean update(Pet pet) {
        return petMapper.update(pet) > 0;
    }

    public boolean delete(Long id) {
        return petMapper.deleteById(id) > 0;
    }

    public List<Map<String, Object>> countByCategory() {
        return petMapper.countByCategory();
    }

    public List<Map<String, Object>> countByGender() {
        return petMapper.countByGender();
    }

    private void fillCustomerNames(List<Pet> pets) {
        if (pets == null || pets.isEmpty()) {
            return;
        }
        List<Long> customerIds = pets.stream()
                .map(Pet::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (customerIds.isEmpty()) {
            return;
        }
        try {
            log.info("[BIZ] 调用 Feign: pet-user-service POST /internal/customers/batch, customerIds={}", customerIds);
            Result<List<CustomerDTO>> result = customerClient.listByIds(customerIds);
            if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
                log.warn("[BIZ] Feign 客户查询返回异常: customerIds={}, code={}",
                        customerIds, result == null ? null : result.getCode());
                return;
            }
            log.info("[BIZ] Feign 客户查询返回: customerCount={}", result.getData().size());
            Map<Long, String> customerNames = result.getData().stream()
                    .filter(customer -> customer.getId() != null)
                    .collect(Collectors.toMap(CustomerDTO::getId, CustomerDTO::getName, (left, right) -> left));
            pets.forEach(pet -> pet.setCustomerName(customerNames.get(pet.getCustomerId())));
        } catch (Exception e) {
            log.warn("[BIZ] 填充宠物客户名称失败，customerIds={}", customerIds, e);
        }
    }
}
