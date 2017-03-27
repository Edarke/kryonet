package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers;
import com.esotericsoftware.kryonet.serializers.KryoSerialization;
import com.esotericsoftware.kryonet.utils.DataMessage;
import com.esotericsoftware.kryonet.utils.YesNoQuery;
import com.esotericsoftware.kryonet.utils.YesNoServerQuery;

/**
 * Created by Evan on 6/14/16.
 */
public class KryoTest extends AbstractSerializerTest<KryoSerialization> {

    public KryoTest() {
        super(new KryoSerialization());
    }





    private <T> void registerArray(Kryo kryo, Class<T> tag){
        kryo.register(tag, new DefaultArraySerializers.ObjectArraySerializer(kryo, tag));
    }


    @Override
    protected void register(){
        final Kryo kryo = serializer.getKryo();

        DataMessage.reg(kryo);

        kryo.register(PingTest.Ping2.class);
        kryo.register(YesNoQuery.class);
        kryo.register(YesNoServerQuery.class);
    }

}
