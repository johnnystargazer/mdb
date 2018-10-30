package sg.com.stargazer.res.rest;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Lists;
import com.google.protobuf.DynamicMessage;

@Slf4j
public class BruteForceFilter {
    private List<Filter> filers = Lists.newArrayList();

    public boolean accept(DynamicMessage dynamicMessage) {
        for (Filter filter : filers) {
            if (!filter.accept(dynamicMessage)) {
                return false;
            }
        }
        return true;
    }

    public BruteForceFilter(List<Filter> filters) {
        this.filers = filters;
        log.info("filters {} ", filters);
    }
}
