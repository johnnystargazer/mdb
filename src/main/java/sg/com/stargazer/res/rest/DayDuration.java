package sg.com.stargazer.res.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.res.util.Constant;

/**
 * Iterable date hour by given start end time
 */
@Slf4j
public class DayDuration implements Iterable<ZonedDateTime> {
    private static DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss[.SSS][ ['UTC']ZZ]]");
    private static final String EMPTY = "";
    private static final int DATE = 10;
    private static final int SECOND = 18;
    private static final int MILLI_SECOND = 22;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private boolean desc;

    public static ZonedDateTime parse(String text) {
        ZoneId zoneId = Constant.ZONE_ID;
        TemporalAccessor date = formatter.parseBest(text, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
        if (date instanceof LocalDate) {
            date = ((LocalDate) date).atStartOfDay(zoneId);
        } else if (date instanceof LocalDateTime) {
            date = ((LocalDateTime) date).atZone(zoneId);
        }
        return ((ZonedDateTime) date).withZoneSameInstant(zoneId);
    }

    public DayDuration(ZonedDateTime start, ZonedDateTime end) {
        this(start, end, true);
    }

    public DayDuration(ZonedDateTime start, ZonedDateTime end, boolean desc) {
        this.start = start;
        this.end = end;
        this.desc = desc;
    }

    public static DayDuration of(String startTime, String endTime) {
        Optional<ZonedDateTime> opStart = ReportTime.forBegin(startTime, Constant.ZONE_ID);
        Optional<ZonedDateTime> opEnd = ReportTime.forEnd(endTime, Constant.ZONE_ID);
        ZonedDateTime start = opStart.isPresent() ? opStart.get() : parse(startTime);
        ZonedDateTime end = opEnd.isPresent() ? opEnd.get() : parse(endTime);
        /**
         * This formatter allows the following formats
         * 2015-11-01 -> yyyy-MM-dd
         * 2015-11-01T17:12:45 -> yyyy-MM-ddTHH:mm:ss
         * 2015-11-01T17:12:45 +0800 -> yyyy-MM-ddTHH:mm:ss ZZ
         * 2015-11-01T17:12:45 UTC+0800 -> yyyy-MM-ddTHH:mm:ss 'UTC'ZZ, please note only UTC is accepted
         * 2015-11-01 17:12:45.000 -> yyyy-MM-dd HH:mm:ss.SSS
         */
        if (opEnd.isPresent()) {
            return new DayDuration(start, end);
        }
        String time = endTime.replaceAll(" ", EMPTY).replaceAll("UTC", EMPTY).replaceAll("T", EMPTY);
        int zone = time.indexOf("+");
        if (zone < 0) {
            zone = time.lastIndexOf("-");
            if (zone < 8) {
                zone = -1;
            }
        }
        if (zone > 0) {
            time = time.substring(0, zone);
        }
        /**
         * So now the time in String should be only
         * 2015-11-01 length :10
         * 2015-11-0117:12:45 length :18
         * 2015-11-0117:12:45.000 length :22
         */
        int length = time.length();
        if (DATE == length) {
            end = end.plusDays(1); // add one more day for query
        } else if (SECOND == length) {
            end = end.plusSeconds(1); // add one more second for query
        } else if (MILLI_SECOND == length) {
            end = end.plusNanos(TimeUnit.MILLISECONDS.toNanos(1)); // add one more mill second for query
        } else {
            log.error("Problem when detect time format for " + endTime);
            throw new RuntimeException("Problem when detect time format for " + endTime);
        }
        return new DayDuration(start, end);
    }

    public boolean isDesc() {
        return desc;
    }

    public ZonedDateTime getStart() {
        return this.start;
    }

    public ZonedDateTime getEnd() {
        return this.end;
    }

    @Override
    public HourDurationIterator iterator() {
        return new HourDurationIterator(start, end, desc);
    }

    @Override
    public String toString() {
        return "HourDuration{" + "start=" + start + ", end=" + end + ", desc=" + desc + '}';
    }

    public class HourDurationIterator implements Iterator<ZonedDateTime> {
        private ZonedDateTime start;
        private ZonedDateTime end;
        private ZonedDateTime current;
        private boolean desc;

        public HourDurationIterator(ZonedDateTime start, ZonedDateTime end, boolean desc) {
            this.start = start.truncatedTo(ChronoUnit.HOURS);
            this.end = end.truncatedTo(ChronoUnit.HOURS);
            this.desc = desc;
            if (this.end.equals(end) && !this.start.equals(this.end)) {
                this.end = this.end.plus(-1, ChronoUnit.HOURS);
            }
        }

        public ZonedDateTime getCurrentDateHour() {
            return current;
        }

        @Override
        public boolean hasNext() {
            boolean result;
            if (Objects.isNull(current)) {
                if (desc) {
                    result = !start.isAfter(end);
                } else {
                    result = !end.isBefore(start);
                }
            } else {
                if (desc) {
                    result = start.isBefore(current);
                } else {
                    result = end.isAfter(current);
                }
            }
            return result;
        }

        public ZonedDateTime nextAsc() {
            if (Objects.isNull(current)) {
                current = start;
            } else {
                current = current.plus(1, ChronoUnit.DAYS);
            }
            return current;
        }

        public ZonedDateTime nextDesc() {
            if (Objects.isNull(current)) {
                current = end;
            } else {
                current = current.plus(-1, ChronoUnit.DAYS);
            }
            return current;
        }

        @Override
        public ZonedDateTime next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (desc) {
                return nextDesc();
            } else {
                return nextAsc();
            }
        }
    }

    public static void main(String[] args) {
        DayDuration dayDuration = new DayDuration(ZonedDateTime.now(), ZonedDateTime.now().plusDays(2));
        HourDurationIterator it = dayDuration.iterator();
        while (it.hasNext()) {
            ZonedDateTime ne = it.next();
            System.out.println(ne);
        }
    }
}
