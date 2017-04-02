package com.esotericsoftware.kryonet.network.serializers;

import com.esotericsoftware.kryo.serializers.DefaultArraySerializers;
import com.esotericsoftware.kryonet.serializers.KryoSerialization;
import com.esotericsoftware.kryonet.serializers.Serialization;

/**
 * Created by Evan on 4/2/17.
 */
public class KryoSerizationTest extends SerializerUnitTest {
    @Override
    public Serialization getSerializer() {
        KryoSerialization k = new KryoSerialization();
        k.getKryo().register(EqualityMessage.class);
        k.getKryo().register(EqualityMessage.NestedObject.class);
        k.getKryo().register(String[].class, new DefaultArraySerializers.StringArraySerializer());
        return k;
    }
}
