package com.blockwithme.pingpong.throughput.jactor2;

import org.agilewiki.jactor2.core.messaging.AsyncRequest;
import org.agilewiki.jactor2.core.processing.MessageProcessor;

public class JActor2RealRequest extends AsyncRequest<Object> {
    final JActor2RealRequestReceiver _targetActor;

    /**
     * @param _targetMessageProcessor
     */
    public JActor2RealRequest(final MessageProcessor _targetMessageProcessor,
            final JActor2RealRequestReceiver _targetActor) {
        super(_targetMessageProcessor);
        this._targetActor = _targetActor;
    }

    @Override
    public void processAsyncRequest() throws Exception {
        _targetActor.processRequest(this, this);
    }
}
