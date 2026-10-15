package com.partapp.partservice.persistence.repository;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.partapp.partservice.persistence.entity.ServiceStateEntity;
import com.partapp.partservice.persistence.mapper.ServiceStateMapper;

/**
 * Internal JSON state access used by repositories; callers use typed repository
 * methods.
 */
final class StateStore {
    private final ServiceStateMapper mapper;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    StateStore(ServiceStateMapper m) {
        mapper = m;
    }

    <K, V> V get(String n, K k, Class<V> t) {
        var r = mapper.selectOne(
                new QueryWrapper<ServiceStateEntity>().eq("namespace", n).eq("state_key", String.valueOf(k)));
        if (r == null)
            return null;
        try {
            return json.readValue(r.getStateValue(), t);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    <K, V> void put(String n, K k, V v) {
        try {
            var r = new ServiceStateEntity();
            r.setNamespace(n);
            r.setStateKey(String.valueOf(k));
            r.setStateValue(json.writeValueAsString(v));
            var q = new QueryWrapper<ServiceStateEntity>().eq("namespace", n).eq("state_key", String.valueOf(k));
            if (mapper.selectOne(q) == null)
                mapper.insert(r);
            else
                mapper.update(r, q);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    <K> void remove(String n, K k) {
        mapper.delete(new QueryWrapper<ServiceStateEntity>().eq("namespace", n).eq("state_key", String.valueOf(k)));
    }

    <V> List<V> values(String n, Class<V> t) {
        return mapper.selectList(new QueryWrapper<ServiceStateEntity>().eq("namespace", n)).stream().map(r -> {
            try {
                return json.readValue(r.getStateValue(), t);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
    }
}
