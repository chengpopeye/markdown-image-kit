package info.dong4j.idea.plugin.sdk.qcloud.cos.endpoint;

/**
 * resolve the endpoint like dns.
 *
 */

public interface EndpointResolver {
    /**
     * return the general api endpoint addr, the result can be ip or ip:port. if you don't do
     * anything like DefaultEndpointResolver. the http library will implement the dns resolve.
     *
     * @param generalApiEndpoint
     * @return the endpoint addr
     */
    String resolveGeneralApiEndpoint(String generalApiEndpoint);

    /**
     * return the get service api endpoint addr, the result can be ip or ip:port. if you don't do
     * anything like DefaultEndpointResolver. the http library will implement the dns resolve.
     *
     * @param getServiceApiEndpoint
     * @return the endpoint addr
     */
    String resolveGetServiceApiEndpoint(String getServiceApiEndpoint);
}
