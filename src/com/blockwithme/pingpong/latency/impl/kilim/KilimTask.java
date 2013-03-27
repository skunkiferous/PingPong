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
package com.blockwithme.pingpong.latency.impl.kilim;

import kilim.Pausable;

/**
 * The Class KilimtTask.
 */
public class KilimTask extends kilim.Task {

    public static volatile int MESSAGES = 1000000;

    /** {@inheritDoc} */
    @Override
    public void execute() throws Pausable, Exception {
        final kilim.Mailbox<Object> pingerMB = new kilim.Mailbox<Object>();
        final kilim.Mailbox<Object> pongerMB = new kilim.Mailbox<Object>();
        final KilimPonger ponger = new KilimPonger(pingerMB, pongerMB);
        final KilimPinger pinger = new KilimPinger(pingerMB, ponger);
        final Integer result = pinger.hammer(MESSAGES);
        if (result.intValue() != MESSAGES) {
            throw new IllegalStateException("Expected " + MESSAGES
                    + " but got " + result);
        }
    }

    public static void main(final String[] args) throws Exception {
        new KilimTask().start();
    }

}