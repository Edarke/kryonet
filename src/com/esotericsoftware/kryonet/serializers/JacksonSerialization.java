package com.esotericsoftware.kryonet.serializers;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Evan on 6/12/16.
 */
public class JacksonSerialization implements Serialization {
    private final ObjectMapper mapper;


    private final ByteBufferInputStream byteBufferInputStream = new ByteBufferInputStream();
    private final ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream();

    private final ByteBufferInput input;
    private final ByteBufferOutput output;


    /**Constructs an serialization with an ObjectMapper that has the following properties:
     PropertyAccessor.FIELD  PUBLIC_ONLY
     FAIL_ON_UNKNOWN_PROPERTIES:    false
    */
     public JacksonSerialization(){
        this(getDefaultMapper());
    }


    public JacksonSerialization(ObjectMapper objectMapper){
        mapper = objectMapper;
        input = new ByteBufferInput();
        output = new ByteBufferOutput();
    }


    public static ObjectMapper getDefaultMapper(){
        return new ObjectMapper()
                .enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }


    @Override
    public synchronized void write (ByteBuffer buffer, Object object) {
        output.setBuffer(buffer);
        try {
            Wrapper wrapper = new Wrapper();
            wrapper.message = object;
            mapper.writeValue(output, wrapper);
        } catch (IOException e) {
            e.printStackTrace();
        }
        output.flush();
    }

    @Override
    public synchronized Object read (ByteBuffer buffer) {
        input.setBuffer(buffer);
        try {
            return mapper.readValue(input, Wrapper.class).message;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static final class Wrapper {
        public Object message;
    }

}
