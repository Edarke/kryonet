package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryonet.serializers.JacksonSerialization;

/**
 * Created by Evan on 6/13/16.
 */
public class JacksonTest extends AbstractSerializerTest<JacksonSerialization> {

    public JacksonTest() {
        super(new JacksonSerialization());
    }

    @Override
    protected void register() {

    }
}
