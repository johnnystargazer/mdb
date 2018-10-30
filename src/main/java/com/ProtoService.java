package com;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rest.ProtoField;

@Slf4j
public class ProtoService {
    private FileDescriptorSet fileDescriptorSet;
    private Descriptor type;
    private Map<String, ProtoField> fieldDescriptors;
    

    public List<ProtoField> getAllFields() {
        return Lists.newArrayList(fieldDescriptors.values());
    }

    public ProtoField getFieldDescriptorByName(String name) {
        return fieldDescriptors.get(name);
    }

    public DynamicMessage getMessage(byte[] bs) throws InvalidProtocolBufferException {
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(type, bs);
        return dynamicMessage;
    }

    public ProtoService(InputStream inputStream) throws Exception {
        fileDescriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(inputStream);
        log.info("total fields {} ", fileDescriptorSet);
        DescriptorProtos.FileDescriptorProto time =
            fileDescriptorSet.getFileList().stream().filter(a -> a.getName().equals("google/protobuf/timestamp.proto"))
                .findFirst().get();
        Descriptors.FileDescriptor dep = Descriptors.FileDescriptor.buildFrom(time, new Descriptors.FileDescriptor[0]);
        DescriptorProtos.FileDescriptorProto tx =
            fileDescriptorSet.getFileList().stream().filter(a -> a.getName().equals("tx.proto")).findFirst().get();
        Descriptors.FileDescriptor desc =
            Descriptors.FileDescriptor.buildFrom(tx, new Descriptors.FileDescriptor[] { dep });
        type = desc.findMessageTypeByName("Transaction");
        desc = Descriptors.FileDescriptor.buildFrom(time, new Descriptors.FileDescriptor[0]);
        fieldDescriptors = new HashMap<>();
        type.getFields().stream().forEach(a -> {
            fieldDescriptors.put(a.getName(), new ProtoField(a));
        });
    }
}
