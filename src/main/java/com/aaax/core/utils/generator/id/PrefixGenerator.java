package com.aaax.core.utils.generator.id;

import lombok.SneakyThrows;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrefixGenerator implements IdentifierGenerator {

    private String refPrefix;
    private boolean isDynamicPrefix = false;
    private String prefix;
    private int prefixDigit = 0;
    private String entityPackage;
    private String pattern;

    @SneakyThrows
    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {

        if (isDynamicPrefix) {
            Class clazz = Class.forName(entityPackage);
            Field field = clazz.getDeclaredField(refPrefix);
            field.setAccessible(true);
            prefix = (String) field.get(obj);
        }

        String query = String.format("SELECT %s FROM %s", session.getEntityPersister(obj.getClass().getName(), obj).getIdentifierPropertyName(), obj.getClass().getSimpleName());
        Stream<String> ids = session.createQuery(query).stream();

        // Validation_____
        if (prefixDigit <= 0) {
            throw new HibernateException("PrefixDigit is invalid");
        }
        if (prefix == null || "".equals(prefix)) {
            throw new HibernateException("Prefix should not be null");
        }

        int sequence = this.getNextId(ids);
        return finalizeIdFormat(sequence);
    }

    private Serializable finalizeIdFormat(int sequence) {
        return prefix.concat(String.format(pattern, sequence));
    }

    // Algorithm for id generation____
    private int getNextId(Stream<String> ids) {
        List<Integer> idNums = ids
                .map(po -> Integer.valueOf(po.substring(prefixDigit)))
                .collect(Collectors.toList());
        int missingNo = this.getMissingNo(idNums);
        if (missingNo <= 0) {
            throw new HibernateException("Error in PrefixGenerator.getNextId");
        }
        return missingNo;
    }


    private Integer getMissingNo(List<Integer> nums) {
        Collections.sort(nums, Collections.reverseOrder());
        int[] register = new int[nums.stream().findFirst().orElse(0) + 1]; // make the first index as unused. shift right 1 digit
        for (Integer num : nums) {
            register[num] = 1;
        }
        List<Integer> absentees = new ArrayList<>();
        for (int i = 1; i < register.length; i++) {
            if (register[i] == 0) {
                absentees.add(i);
            }
        }
        Optional<Integer> isNoAbsent = absentees.stream().sorted().findFirst();
        if (isNoAbsent.isEmpty()) {
            return register.length;
        }
        return isNoAbsent.get();
    }


    @Override
    public void configure(Type type, Properties properties, ServiceRegistry serviceRegistry) throws MappingException {
        entityPackage = properties.getProperty("entity_name");
        isDynamicPrefix = Boolean.parseBoolean(properties.getProperty("isDynamicPrefix"));
        pattern = properties.getProperty("pattern", "%03d");
        if (isDynamicPrefix) {
            refPrefix = properties.getProperty("refPrefix");
            prefixDigit = refPrefix.length();
        } else {
            prefix = properties.getProperty("prefix");
            prefixDigit = prefix.length();
        }
    }

}
