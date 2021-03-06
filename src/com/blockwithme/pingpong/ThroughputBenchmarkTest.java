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
package com.blockwithme.pingpong;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.Mailbox;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jactor2.core.impl.Plant;
import org.agilewiki.jactor2.core.impl.mtPlant.PlantConfiguration;
import org.agilewiki.jactor2.core.reactors.BlockingReactor;
import org.agilewiki.jactor2.core.reactors.IsolationReactor;
import org.agilewiki.jactor2.core.reactors.NonBlockingReactor;
import org.agilewiki.jactor2.core.reactors.Reactor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.blockwithme.pingpong.throughput.jactor.JActorEcho;
import com.blockwithme.pingpong.throughput.jactor.JActorParallel;
import com.blockwithme.pingpong.throughput.jactor.JActorRealRequest;
import com.blockwithme.pingpong.throughput.jactor.JActorSender;
import com.blockwithme.pingpong.throughput.jactor2.JActor2Echo;
import com.blockwithme.pingpong.throughput.jactor2.JActor2Parallel;
import com.blockwithme.pingpong.throughput.jactor2.JActor2Sender;
import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;

/**
 * Tests the number of seconds required to do parallel request/reply cycles,
 * for different possible Actor implementations.
 *
 * PAIRS is the number of actor pairs, exchanging requests and replies
 * MESSAGES is the number of messages, each pair exchanges
 */
@AxisRange(min = 0, max = 3)
@BenchmarkMethodChart(filePrefix = "ThroughputBenchmark")
public class ThroughputBenchmarkTest extends AbstractBenchmark {

    private static interface ReactorFactoy {
        Reactor create() throws Exception;
    }

    private static class NonBlockingReactorFactoy implements ReactorFactoy {
        @Override
        public Reactor create() throws Exception {
            return new NonBlockingReactor();
        }
    }

    private static class BlockingReactorFactoy implements ReactorFactoy {
        @Override
        public Reactor create() throws Exception {
            return new BlockingReactor();
        }
    }

    private static class IsolationReactorFactoy implements ReactorFactoy {
        @Override
        public Reactor create() throws Exception {
            return new IsolationReactor();
        }
    }

    /** Sets the benchmark properties, for stats and graphics generation. */
    static {
        System.setProperty("jub.consumers", "CONSOLE,H2");
        System.setProperty("jub.db.file", "benchmarks");
        System.setProperty("jub.charts.dir", "charts");
    }

    /** Allows fast "functional testing" of the tests. */
    private static final boolean QUICK = false;

    /** Allows disabling the tests easily. */
    private static final boolean RUN = true;

    /** Allows disabling the testJActorAsyncMailbox method easily. */
    private static final boolean testJActorAsyncMailbox = RUN;

    /** Allows disabling the testJActorSharedMailbox method easily. */
    private static final boolean testJActorSharedMailbox = RUN;

    /** Allows disabling the testJActor2NBAsyncMailbox method easily. */
    private static final boolean testJActor2NBAsyncMailbox = RUN;

    /** Allows disabling the testJActor2NBSharedMailbox method easily. */
    private static final boolean testJActor2NBSharedMailbox = RUN;

    /** How many actors pair per test? */
    protected static final int PAIRS = QUICK ? 100 : 1000;

    /** How many batches to send per actor pair? */
    protected static final int BATCHES = QUICK ? 100 : 1000;

    /** How many messages to send per batch? */
    protected static final int MESSAGES_PER_BATCH = QUICK ? 100 : 1000;

    /** How many threads? */
    protected static final int THREADS = 8;

    /** The JActor MailboxFactory */
    protected MailboxFactory jaMailboxFactory;

    /** Setup all "services" for all test methods.
     * @throws Exception */
    @Before
    public void setup() throws Exception {
        jaMailboxFactory = JAMailboxFactory.newMailboxFactory(THREADS);
        new IsolationFriendlyPlantMtImpl(new PlantConfiguration(THREADS) {
            @Override
            public int getInitialLocalMessageQueueSize() {
                return MESSAGES_PER_BATCH + 10;
            }

            @Override
            public int getInitialBufferSize() {
                return MESSAGES_PER_BATCH + 10;
            }
        });
    }

    /** Shuts down all "services" for all test methods.
     * @throws Exception */
    @After
    public void teardown() throws Exception {
        jaMailboxFactory.close();
        jaMailboxFactory = null;
        Plant.close();
    }

    /** Throughput test in JActors. */
    private void doJActor(final boolean shared) throws Exception {
        final Actor[] senders = new Actor[PAIRS];
        int i = 0;
        while (i < PAIRS) {
            final Mailbox echoMailbox = jaMailboxFactory.createAsyncMailbox();
            final JActorEcho echo = new JActorEcho();
            echo.initialize(echoMailbox);
            echo.setInitialBufferCapacity(MESSAGES_PER_BATCH + 10);
            final Mailbox senderMailbox;
            if (shared) {
                senderMailbox = echoMailbox;
            } else {
                senderMailbox = jaMailboxFactory.createAsyncMailbox();
            }
            final JActorSender s = new JActorSender(echo, BATCHES,
                    MESSAGES_PER_BATCH);
            s.initialize(senderMailbox);
            senders[i] = s;
            senders[i].setInitialBufferCapacity(MESSAGES_PER_BATCH + 10);
            i += 1;
        }
        final JActorParallel parallel = new JActorParallel();
        if (shared) {
            parallel.initialize(jaMailboxFactory.createMailbox());
        } else {
            parallel.initialize(jaMailboxFactory.createAsyncMailbox());
        }
        parallel.actors = senders;
        final JAFuture future = new JAFuture();
        JActorRealRequest.req.send(future, parallel);
    }

    /** Throughput test in JActor2. */
    private void doJActor2(final boolean shared, final ReactorFactoy factory)
            throws Exception {
        final JActor2Sender[] senders = new JActor2Sender[PAIRS];
        int i = 0;
        while (i < PAIRS) {
            final Reactor echoReactor = factory.create();
            final JActor2Echo echo = new JActor2Echo(echoReactor);
            final Reactor senderReactor;
            if (shared) {
                senderReactor = echoReactor;
            } else {
                senderReactor = factory.create();
            }
            final JActor2Sender s = new JActor2Sender(senderReactor, echo,
                    BATCHES, MESSAGES_PER_BATCH);
            senders[i] = s;
            i += 1;
        }
        final JActor2Parallel parallel = new JActor2Parallel(factory.create(),
                senders);
        parallel.startParReq().call();
    }

    /** Throughput test in JActors, using async Mailboxes. */
    @BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 3)
    @Test
    public void testJActorAsyncMailbox() throws Exception {
        if (testJActorAsyncMailbox) {
            doJActor(false);
        }
    }

    /** Throughput test in JActors, using shared Mailboxes. */
    @BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 3)
    @Test
    public void testJActorSharedMailbox() throws Exception {
        if (testJActorSharedMailbox) {
            doJActor(true);
        }
    }

    /** Throughput test in JActor2, using async Mailboxes. */
    @BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 3)
    @Test
    public void testJActor2NBAsyncMailbox() throws Exception {
        if (testJActor2NBAsyncMailbox) {
            doJActor2(false, new NonBlockingReactorFactoy());
        }
    }

    /** Throughput test in JActor2, using shared Mailboxes. */
    @BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 3)
    @Test
    public void testJActor2NBSharedMailbox() throws Exception {
        if (testJActor2NBSharedMailbox) {
            doJActor2(true, new NonBlockingReactorFactoy());
        }
    }
}
