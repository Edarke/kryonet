package com.esotericsoftware.kryonet.serializers;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryonet.util.KryoNetException;
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

    /**Constructs an serialization with an ObjectMapper that has the following properties:
     PropertyAccessor.FIELD  PUBLIC_ONLY
     FAIL_ON_UNKNOWN_PROPERTIES:    false
    */
     public JacksonSerialization(){
        this(getDefaultMapper());
    }


    public JacksonSerialization(ObjectMapper objectMapper){
        mapper = objectMapper;
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


    /** Returns the object mapper for this serialization instance.
     * Changing the configuration of the mapper is not thread safe, so any changes must be
     * done prior to starting the endpoint.*/
    public ObjectMapper getMapper(){
        return mapper;
    }

    @Override
    public void write (ByteBuffer buffer, Object object) {
        try {
            output.setBuffer(buffer);
            Wrapper wrapper = new Wrapper();
            wrapper.message = object;
            mapper.writeValue(output, wrapper);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final ByteBufferInput input = new ByteBufferInput();
    private final ByteBufferOutput output = new ByteBufferOutput();

    @Override
    public Object read (ByteBuffer buffer) {
        try {
            input.setBuffer(buffer);
            return mapper.readValue(input, Wrapper.class).message;
        } catch (IOException e) {
            throw new KryoNetException("Jackson unable to deserialize message", e);
        }
    }


    public static final class Wrapper {
        public Object message;
    }

}
