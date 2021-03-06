package info.dong4j.idea.plugin.sdk.qcloud.cos.event;

import info.dong4j.idea.plugin.sdk.qcloud.cos.exception.CosClientException;

public interface ProgressListener {
    ProgressListener NOOP = new ProgressListener() {
        @Override public void progressChanged(ProgressEvent progressEvent) {}
    };

    /**
     * Called when progress has changed, such as additional bytes transferred,
     * transfer failed, etc. The execution of the callback of this listener is managed
     * by {@link SDKProgressPublisher}.  Implementation of this interface
     * should never block.
     * <p>
     * If the implementation follows the best practice and doesn't block, it
     * should then extends from {@link SyncProgressListener}.
     * <p>
     * Note any exception thrown by the listener will get ignored.
     * Should there be need to capture any such exception, you may consider
     * wrapping the listener with {@link ExceptionReporter#wrap(ProgressListener)}.
     *
     * @param progressEvent
     *            The event describing the progress change.
     *
     * @see SDKProgressPublisher
     * @see ExceptionReporter
     */
    void progressChanged(ProgressEvent progressEvent);

    /**
     * A utility class for capturing and reporting the first exception thrown by
     * a given progress listener. Note once an exception is thrown by the
     * underlying listener, all subsequent events will no longer be notified to
     * the listener.
     */
    class ExceptionReporter implements ProgressListener, DeliveryMode {
        private final ProgressListener listener;
        private final boolean syncCallSafe;
        private volatile Throwable cause;

        public ExceptionReporter(ProgressListener listener) {
            if (listener == null)
                throw new IllegalArgumentException();
            this.listener = listener;
            if (listener instanceof DeliveryMode) {
                DeliveryMode cs = (DeliveryMode) listener;
                syncCallSafe = cs.isSyncCallSafe();
            } else
                syncCallSafe = false;
        }

        /**
         * Delivers the progress event to the underlying listener but only if
         * there has not been an exception previously thrown by the listener.
         * <p>
         * {@inheritDoc}
         */
        @Override public void progressChanged(ProgressEvent progressEvent) {
            if (cause != null)
                return;
            try {
                this.listener.progressChanged(progressEvent);
            } catch(Throwable t) {
                cause = t;
            }
        }

        /**
         * Throws the underlying exception, if any, as an
         * {@link CosClientException}; or do nothing otherwise.
         */
        public void throwExceptionIfAny() {
            if (cause != null)
                throw new CosClientException(cause);
        }

        /**
         * Returns the underlying exception, if any; or null otherwise.
         */
        public Throwable getCause() {
            return cause;
        }

        /**
         * Returns a wrapper for the given listener to capture the first
         * exception thrown.
         */
        public static ExceptionReporter wrap(ProgressListener listener) {
            return new ExceptionReporter(listener);
        }

        @Override public boolean isSyncCallSafe() { return syncCallSafe; }
    }
}


