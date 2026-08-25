package com.aaax.core.utils.generator.id;

import lombok.SneakyThrows;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

public class NumericIdGenerator implements IdentifierGenerator {

    private String digit;
    private String startWith;

    @SneakyThrows
    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
        String query = String.format("SELECT %s FROM %s", session.getEntityPersister(obj.getClass().getName(), obj).getIdentifierPropertyName(), obj.getClass().getSimpleName());
        Stream<Long> ids = session.createQuery(query, Long.class).stream();
        // ____ Validation
        return this.getNextId(ids);
    }

    // ____ Algorithm for id generation
    private long getNextId(Stream<Long> ids) {
        int[] _ids = ids.mapToInt(Long::intValue).toArray();
        if (_ids.length == 0) { // first record
            String format = startWith.concat(String.format("%0" + digit + "d", 0));
            return Long.parseLong(format);
        }
        return findMissingId(_ids) + 1;
    }

    private int findMissingId(int[] numbers) {
        Arrays.sort(numbers);
        int length = numbers.length;
        for (int i = 0; i <= length - 1; i++) {
            int left = numbers[i];
            if (i == length - 1) {
                return left;
            }
            int right = numbers[i + 1];
            if (left + 1 != right) {
                return left;// Found a gap, indicating a missing number
            }
        }
        throw new HibernateException("TenantIdGenerator error in finding missing number.");
    }


    @Override
    public void configure(Type type, Properties properties, ServiceRegistry serviceRegistry) {
        digit = properties.getProperty("digit", "4");
        startWith = properties.getProperty("startWith", "1");
    }
}
