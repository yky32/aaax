package com.aaax.core.utils;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;


@Slf4j
public class CronUtil {

    public static boolean isRun(String cronExpression) {
        ZonedDateTime localDateTime = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"));
        return isRun(cronExpression, localDateTime);
    }

    public static boolean isRun(String cronExpression, ZonedDateTime targetDt) {
        log.info("--- CronUtil.cronExpression => {}", cronExpression);
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J);
        CronParser parser = new CronParser(cronDefinition);
        Cron cron = parser.parse(cronExpression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        boolean match = executionTime.isMatch(targetDt);
        if (match) {
            log.info("--- CronUtil.isMatch => {} by now: {}", true, targetDt);
        } else {
            log.info("--- CronUtil.misMatch => {} by now: {}", false, targetDt);
        }
        return match;
    }
}
