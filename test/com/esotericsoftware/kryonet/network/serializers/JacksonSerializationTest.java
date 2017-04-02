package com.esotericsoftware.kryonet.network.serializers;

import com.esotericsoftware.kryonet.serializers.JacksonSerialization;
import com.esotericsoftware.kryonet.serializers.Serialization;

/**
 * Created by Evan on 4/2/17.
 */
public class JacksonSerializationTest extends SerializerUnitTest {
    @Override
    public Serialization getSerializer() {
        return new JacksonSerialization();
    }
}
