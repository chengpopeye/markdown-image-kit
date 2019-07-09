package info.dong4j.idea.plugin.sdk.qcloud.cos.event;

/**
 * An interface that filters the incoming events before passing
 * them into the registered listeners.
 */
public interface ProgressEventFilter {

    /**
     * Returns the filtered event object that will be actually passed into
     * the listeners. Returns null if the event should be completely blocked.
     */
    ProgressEvent filter(ProgressEvent progressEvent);
}