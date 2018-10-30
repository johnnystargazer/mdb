package sg.com.stargazer.res.rest;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import sg.com.stargazer.res.proto.ProtoField;

import com.google.protobuf.DynamicMessage;

@Value
@Slf4j
public class Filter {
    private ProtoField protoField;
    private Object value;

    public Filter(ProtoField protoField, String value) {
        this.protoField = protoField;
        this.value = protoField.fromCompareValue(value);
    }

    public boolean accept(DynamicMessage msg) {
        return this.value.equals(protoField.getValuefromMessage(msg));
    }
}
