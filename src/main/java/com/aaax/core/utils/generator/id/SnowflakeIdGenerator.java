package com.aaax.core.utils.generator.id;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class SnowflakeIdGenerator implements IdentifierGenerator {
    /** 12-bit sequence; exclusive cap. (`2 ^ 12` is XOR → 14 — do not use.) */
    static final long MAX_SEQUENCE = 1L << 12;

    private long machineId = 0;
    private long timestamp = 0;
    private long sequence = 0;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        long id;
        while ((id = doGenerate()) == 0) ;
        return id;
    }

    private synchronized long doGenerate() {
        long currentTimestamp = System.currentTimeMillis();

        if (currentTimestamp > timestamp) {
            timestamp = currentTimestamp;
            sequence = 0;
        } else if (currentTimestamp == timestamp) {
            if (sequence >= MAX_SEQUENCE) {
                return 0;
            }
        }
        //clock drift
        else if (currentTimestamp < timestamp) {
            return 0;
        }
        long _machineId = Optional.of(SnowflakeIdGeneratorConfiguration.MACHINE_ID).orElse(Math.toIntExact(machineId));
        log.trace("snowflake machineId={} identifier={}", _machineId, SnowflakeIdGeneratorConfiguration.IDENTIFIER);
        return (timestamp << (64 - 1 - 41)) | (_machineId << (64 - 1 - 41 - 10)) | sequence++;
    }

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
        this.machineId = SnowflakeIdGeneratorConfiguration.MACHINE_ID;
        if (machineId < 0 || machineId > 1024) throw new IllegalStateException("illegal machine id.");
    }
}
