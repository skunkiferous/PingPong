/*
 * Copyright (C) 2013 Sebastien Diot.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blockwithme.pingpong.throughput.jactor2;

import org.agilewiki.jactor2.core.blades.BladeBase;
import org.agilewiki.jactor2.core.reactors.Reactor;
import org.agilewiki.jactor2.core.requests.SyncRequest;

/**
 * @author monster
 *
 */
public class JActor2Echo extends BladeBase {

    /** Constructs a JActor2Echo. */
    public JActor2Echo(final Reactor _reactor) throws Exception {
        _initialize(_reactor);
    }

    /** Creates an echo (null) request. */
    public SyncRequest<Void> echoReq() {
        /** An echo (null) request. */
        return new SyncBladeRequest<Void>() {
            @Override
            public Void processSyncRequest() throws Exception {
                return null;
            }
        };
    }
}