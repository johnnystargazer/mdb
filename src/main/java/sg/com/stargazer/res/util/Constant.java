package sg.com.stargazer.res.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.protobuf.Timestamp;

public class Constant {
    public static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter MONTH_PATTERN = DateTimeFormatter.ofPattern("yyyyMM");
    public static final ZoneId ZONE_ID = ZoneId.of("UTC");
    private static final HashFunction HF = Hashing.murmur3_128();

    public static ZonedDateTime zonedDatetime(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()).atZone(ZONE_ID);
    }

    public static byte[] hashId(Long id) {
        return HF.newHasher().putLong(id).hash().asBytes();
    }

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

    public static List<String> getAccountRangePath(ZonedDateTime time, Long accountIdValue) {
        String timeKey = DATE_PATTERN.format(time);
        return Lists.newArrayList("Transaction", timeKey, "Account", accountIdValue.toString());
    }

    public static List<String> getCompanyRangePath(ZonedDateTime time, Long companyIdValue) {
        String timeKey = DATE_PATTERN.format(time);
        return Lists.newArrayList("Transaction", timeKey, "Company", companyIdValue.toString());
    }

    public static List<String> getRangePath(ZonedDateTime time) {
        String timeKey = DATE_PATTERN.format(time);
        return Lists.newArrayList("Transaction", timeKey);
    }
}
