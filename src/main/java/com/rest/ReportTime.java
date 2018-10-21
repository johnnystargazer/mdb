package com.rest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;

/**
 * Class convert String into ZonedDateTime which match pattern
 * [0-9]{4}-[0-9]{2}- [0-9]{2}|today|yesterday|thisWeek|thisMonth|thisYear|[0-9]+(daysAgo)
 * <p>We plan to use greater equals than for start day and less than for end day. So end day will be one day after
 * client's request.</p> <p> Given that now is 2015-12-10 If client specify 9daysAgo for start date, the return value
 * will be 2015-12-1 00:00:00 If client specify 9daysAgo for end date, the return value will be 2015-12-2 00:00:00</p>
 * <p>If client specify today for start date, the return value will be 2015-12-10 00:00:00 If client specify today for
 * end date, the return value will be 2015-12-11 00:00:00</p> <p>Also notice that end date's value for
 * "today","thisWeek","thisMonth","thisYear","0daysAgo" will be same.</p>
 */
public class ReportTime {
    private static final String REGEX = "^[0-9]{4}-[0-9]{2}-[0-9]{2}$|^today$|^yesterday$|^thisWeek$|"
        + "^thisMonth$|^thisYear$|^[0-9]+(daysAgo)$";
    private static final ZonedDateTimeConverter AGO = new AgoConverter();
    private static final ZonedDateTimeConverter FORMAT = new TimeFormatConverter();
    public static final String FILTER_TIME_FORMAT = "yyyy-MM-dd";
    public static final String DESC = "-";
    public static final String SORT_SPLIT = ",";
    public static final String TODAY = "today";
    public static final String YESTERDAY = "yesterday";
    public static final String THIS_WEEK = "thisWeek";
    public static final String THIS_MONTH = "thisMonth";
    public static final String THIS_YEAR = "thisYear";
    public static final String DAYS_AGO = "daysAgo";
    private static final ImmutableMap<String, ZonedDateTimeConverter> ZONE_DATE_TIME_CONVERT =
        new ImmutableMap.Builder<String, ZonedDateTimeConverter>().put(TODAY, new TodayConverter())
            .put(YESTERDAY, new YesterdayConverter()).put(THIS_WEEK, new WeekConverter())
            .put(THIS_MONTH, new MonthConverter()).put(THIS_YEAR, new YearConverter()).build();
    private static Pattern PATTERN = Pattern.compile(REGEX);

    /**
     * Convert String to ZonedDateTime base on given zoneId
     *
     * @return The beginning of query period
     */
    public static Optional<ZonedDateTime> forBegin(String data, ZoneId zoneId) {
        Matcher matcher = PATTERN.matcher(data);
        if (!matcher.find()) {
            return Optional.empty();
        }
        ZonedDateTimeConverter zonedDateTimeConverter = getConvertFromMatcher(matcher);
        return Optional.of(zonedDateTimeConverter.getBegin(ZonedDateTime.now(zoneId), data));
    }

    /**
     * Convert String to ZonedDateTime base on given zoneId
     *
     * @return The end of query period
     */
    public static Optional<ZonedDateTime> forEnd(String data, ZoneId zoneId) {
        Matcher matcher = PATTERN.matcher(data);
        if (!matcher.find()) {
            return Optional.empty();
        }
        ZonedDateTimeConverter zonedDateTimeConverter = getConvertFromMatcher(matcher);
        return Optional.of(zonedDateTimeConverter.getEnd(ZonedDateTime.now(zoneId), data));
    }

    private static ZonedDateTimeConverter getConvertFromMatcher(Matcher matcher) {
        ZonedDateTimeConverter zonedDateTimeConverter;
        /**
         * Note : only ago has group 1 currently
         */
        if (matcher.group(1) != null) {
            zonedDateTimeConverter = AGO;
        } else {
            /**
             * Find by name predefiend name first
             */
            zonedDateTimeConverter = ZONE_DATE_TIME_CONVERT.get(matcher.group(0));
            if (zonedDateTimeConverter == null) {
                zonedDateTimeConverter = FORMAT;
            }
        }
        return zonedDateTimeConverter;
    }

    /**
     * Clean hour, minute etc then plus 1 day
     */
    private static ZonedDateTime oneDayPlus(ZonedDateTime now) {
        return ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 0, 0, 0, 0, now.getZone())
            .plusDays(1L);
    }

    public interface ZonedDateTimeConverter {
        ZonedDateTime getBegin(ZonedDateTime now, String data);

        ZonedDateTime getEnd(ZonedDateTime now, String data);
    }

    /**
     * Convert [0-9]{4}-[0-9]{2}-[0-9]{2}
     */
    static class TimeFormatConverter implements ZonedDateTimeConverter {
        @Override
        public ZonedDateTime getBegin(ZonedDateTime now, String data) {
            try {
                LocalDate date = LocalDate.parse(data, DateTimeFormatter.ofPattern(FILTER_TIME_FORMAT));
                return date.atStartOfDay(now.getZone());
            } catch (DateTimeParseException dateTimeParseException) {
                return null;
// throw new ReportRequestParseException(dateTimeParseException, ILLEGAL_TIME_DATA,
// ExceptionCodes.REPORT_ILLEGAL_TIME_DATA, data);
            }
        }

        @Override
        public ZonedDateTime getEnd(ZonedDateTime now, String data) {
            LocalDate date = LocalDate.parse(data, DateTimeFormatter.ofPattern(FILTER_TIME_FORMAT));
            return date.atStartOfDay(now.getZone()).plusDays(1L);
        }
    }

    /**
     * Convert [0-9]+(daysAgo)
     */
    static class AgoConverter implements ZonedDateTimeConverter {
        @Override
        public ZonedDateTime getBegin(ZonedDateTime now, String data) {
            Long ago = Integer.valueOf(data.substring(0, data.length() - DAYS_AGO.length())) * -1L;
            return ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 0, 0, 0, 0, now.getZone())
                .plusDays(ago);
        }

        @Override
        public ZonedDateTime getEnd(ZonedDateTime now, String data) {
            Long ago = Integer.valueOf(data.substring(0, data.length() - DAYS_AGO.length())) * -1L;
            return ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 0, 0, 0, 0, now.getZone())
                .plusDays(ago + 1L);
        }
    }

    /**
     * Convert thisMonth
     */
    static class YearConverter implements ZonedDateTimeConverter {
        @Override
        public ZonedDateTime getBegin(ZonedDateTime now, String data) {
            return ZonedDateTime.of(now.getYear(), 1, 1, 0, 0, 0, 0, now.getZone());
        }

        @Override
        public ZonedDateTime getEnd(ZonedDateTime now, String data) {
            return oneDayPlus(now);
        }
    }

    /**
     * Convert thisYear
     */
    static class MonthConverter implements ZonedDateTimeConverter {
        @Override
        public ZonedDateTime getBegin(ZonedDateTime now, String data) {
            return ZonedDateTime.of(now.getYear(), now.getMonthValue(), 1, 0, 0, 0, 0, now.getZone());
        }

        @Override
        public ZonedDateTime getEnd(ZonedDateTime now, String data) {
            return oneDayPlus(now);
        }
    }

    /**
     * Convert today
     */
    static class TodayConverter implements ZonedDateTimeConverter {
        @Override
        public ZonedDateTime getBegin(ZonedDateTime now, String data) {
            return ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 0, 0, 0, 0, now.getZone());
        }

        @Override
        public ZonedDateTime getEnd(ZonedDateTime now, String data) {
            return oneDayPlus(now);
        }
    }

    /**
     * Convert thisWeek
     */
    static class WeekConverter implements ZonedDateTimeConverter {
        @Override
        public ZonedDateTime getBegin(ZonedDateTime now, String data) {
            long day = now.getDayOfWeek().ordinal() * -1L;
            return ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 0, 0, 0, 0, now.getZone())
                .plusDays(day);
        }

        @Override
        public ZonedDateTime getEnd(ZonedDateTime now, String data) {
            return oneDayPlus(now);
        }
    }

    /**
     * Convert yesterday
     */
    static class YesterdayConverter implements ZonedDateTimeConverter {
        @Override
        public ZonedDateTime getBegin(ZonedDateTime now, String data) {
            return ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 0, 0, 0, 0, now.getZone())
                .plusDays(-1L);
        }

        @Override
        public ZonedDateTime getEnd(ZonedDateTime now, String data) {
            return ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 0, 0, 0, 0, now.getZone());
        }
    }
}
