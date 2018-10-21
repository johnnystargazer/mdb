package com.rest;

import lombok.ToString;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.DynamicMessage;

@ToString
public class ProtoField {
    private JavaType javaType;
    private EnumDescriptor enumDescriptor;
    private FieldDescriptor fieldDescriptor;

    public String getName() {
        return fieldDescriptor.getName();
    }

    public ProtoField(FieldDescriptor fieldDescriptor) {
        this.javaType = fieldDescriptor.getJavaType();
        if (JavaType.ENUM.equals(javaType)) {
            this.enumDescriptor = fieldDescriptor.getEnumType();
        }
        this.fieldDescriptor = fieldDescriptor;
    }

    public Object fromCompareValue(String parameter) {
        switch (javaType) {
            case BOOLEAN:
                return Boolean.valueOf(parameter);
            case BYTE_STRING:
                return parameter.getBytes();
            case DOUBLE:
                return Double.valueOf(parameter);
            case FLOAT:
                return Float.valueOf(parameter);
            case INT:
                return Integer.valueOf(parameter);
            case LONG:
                return Long.valueOf(parameter);
            case STRING:
                return parameter;
            case ENUM:
                return enumDescriptor.findValueByName(parameter);
            default:
                throw new RuntimeException("Not support");
        }
    }

    public Object getValuefromMessage(DynamicMessage dynamicMessage) {
        return dynamicMessage.getField(fieldDescriptor);
    }

    public int getIndex() {
        return fieldDescriptor.getIndex();
    }
}
