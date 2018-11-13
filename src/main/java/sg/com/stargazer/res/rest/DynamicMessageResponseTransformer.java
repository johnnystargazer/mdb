package sg.com.stargazer.res.rest;

import spark.ResponseTransformer;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

public class DynamicMessageResponseTransformer implements ResponseTransformer {
    @Override
    public String render(Object model) throws Exception {
        DynamicMessage dynamicMessage = (DynamicMessage) model;
        return JsonFormat.printer().print(dynamicMessage);
    }
}
