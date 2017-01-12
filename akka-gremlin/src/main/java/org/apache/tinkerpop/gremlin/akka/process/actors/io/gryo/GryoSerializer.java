/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.tinkerpop.gremlin.akka.process.actors.io.gryo;

import akka.serialization.Serializer;
import org.apache.tinkerpop.gremlin.process.actors.Address;
import org.apache.tinkerpop.gremlin.process.actors.traversal.message.BarrierAddMessage;
import org.apache.tinkerpop.gremlin.process.actors.traversal.message.BarrierDoneMessage;
import org.apache.tinkerpop.gremlin.process.actors.traversal.message.SideEffectAddMessage;
import org.apache.tinkerpop.gremlin.process.actors.traversal.message.SideEffectSetMessage;
import org.apache.tinkerpop.gremlin.process.actors.traversal.message.StartMessage;
import org.apache.tinkerpop.gremlin.process.actors.traversal.message.Terminate;
import org.apache.tinkerpop.gremlin.process.actors.util.DefaultActorsResult;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoPool;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoVersion;
import org.apache.tinkerpop.shaded.kryo.io.Input;
import org.apache.tinkerpop.shaded.kryo.io.Output;
import scala.Option;

import java.io.ByteArrayOutputStream;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class GryoSerializer implements Serializer {

    private final GryoPool gryoPool;

    public GryoSerializer() {
        this.gryoPool = GryoPool.build().
                poolSize(10).
                initializeMapper(builder ->
                        builder.referenceTracking(true).
                                registrationRequired(true).
                                version(GryoVersion.V3_0).
                                addCustom(
                                        Terminate.class,
                                        StartMessage.class,
                                        BarrierAddMessage.class,
                                        BarrierDoneMessage.class,
                                        SideEffectSetMessage.class,
                                        SideEffectAddMessage.class,
                                        DefaultActorsResult.class,
                                        Address.Master.class)).create();
    }

    public GryoMapper getGryoMapper() {
        return this.gryoPool.getMapper();
    }

    @Override
    public int identifier() {
        return GryoVersion.V3_0.hashCode();
    }

    @Override
    public boolean includeManifest() {
        return false;
    }

    @Override
    public byte[] toBinary(final Object object) {
        final Output output = new Output(new ByteArrayOutputStream());
        this.gryoPool.writeWithKryo(kryo -> kryo.writeClassAndObject(output, object));
        return output.getBuffer();
    }

    @Override
    public Object fromBinary(final byte[] bytes) {
        final Input input = new Input();
        input.setBuffer(bytes);
        return this.gryoPool.readWithKryo(kryo -> kryo.readClassAndObject(input));
    }

    @Override
    public Object fromBinary(final byte[] bytes, final Class<?> aClass) {
        return fromBinary(bytes);
    }

    @Override
    public Object fromBinary(final byte[] bytes, final Option<Class<?>> option) {
        return option.isEmpty() ? this.fromBinary(bytes) : this.fromBinary(bytes, option.get());
    }
}