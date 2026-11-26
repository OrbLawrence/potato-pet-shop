package com.petshop.biz.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.biz.entity.Store;
import com.petshop.biz.mapper.StoreMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class StoreService {

    @Autowired
    private StoreMapper storeMapper;

    public PageInfo<Store> page(int pageNum, int pageSize, String name) {
        log.info("[BIZ] 分页查询门店: pageNum={}, pageSize={}, name={}", pageNum, pageSize, name);
        PageHelper.startPage(pageNum, pageSize);
        List<Store> list = storeMapper.list(name);
        return new PageInfo<>(list);
    }

    public List<Store> listAll() {
        log.info("[BIZ] 查询全部门店");
        return storeMapper.list(null);
    }

    public List<Store> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("[BIZ] 内部批量查询门店: ids={}", ids);
        List<Store> stores = storeMapper.listByIds(ids);
        log.info("[BIZ] 内部批量查询门店完成: requested={}, found={}", ids.size(), stores.size());
        return stores;
    }

    public Store getById(Long id) {
        return storeMapper.getById(id);
    }

    public boolean add(Store store) {
        return storeMapper.insert(store) > 0;
    }

    public boolean update(Store store) {
        return storeMapper.update(store) > 0;
    }

    public boolean delete(Long id) {
        return storeMapper.deleteById(id) > 0;
    }
}
