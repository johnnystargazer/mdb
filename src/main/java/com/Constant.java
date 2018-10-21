package com;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.common.collect.Lists;

public class Constant {
    public static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter MONTH_PATTERN = DateTimeFormatter.ofPattern("yyyyMM");
    public static final ZoneId ZONE_ID = ZoneId.of("UTC");

    public static ZonedDateTime fromParameter(String parameter) {
        LocalDate localDate = LocalDate.parse(parameter, DATE_PATTERN);
        return localDate.atStartOfDay(ZONE_ID);
    }

    public static List<String> getIdPath(ZonedDateTime zonedDateTime) {
        return Lists.newArrayList(Lists.newArrayList("Transaction", "id",
            MONTH_PATTERN.format(zonedDateTime.withZoneSameInstant(ZONE_ID))));
    }

    public static List<String> getExtPath(ZonedDateTime zonedDateTime) {
        return Lists.newArrayList(Lists.newArrayList("Transaction", "ext",
            MONTH_PATTERN.format(zonedDateTime.withZoneSameInstant(ZONE_ID))));
    }

    public static List<String> getRangePath(ZonedDateTime time, Long accountIdValue) {
        String timeKey = DATE_PATTERN.format(time);
        return Lists.newArrayList("Transaction", timeKey, accountIdValue.toString());
    }

    public static List<String> getRangePath(ZonedDateTime time) {
        String timeKey = DATE_PATTERN.format(time);
        return Lists.newArrayList("Transaction", timeKey);
    }
}
