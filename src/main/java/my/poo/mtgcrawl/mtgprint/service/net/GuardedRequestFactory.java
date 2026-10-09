package my.poo.mtgcrawl.mtgprint.service.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpRequest;

/**
 * Request-Factory für das RestTemplate: prüft jedes Ziel mit dem OutboundUrlGuard und lässt HttpURLConnection
 * keine Weiterleitungen selbst folgen. Das übernimmt {@link #redirectInterceptor()}, der jedes neue Ziel wieder
 * über diese Factory (und damit über die Prüfung) anlegt.
 */
public class GuardedRequestFactory extends SimpleClientHttpRequestFactory {

    private final OutboundUrlGuard guard;

    public GuardedRequestFactory(OutboundUrlGuard guard) {
        this.guard = guard;
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        return super.createRequest(guard.check(uri), httpMethod);
    }

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        super.prepareConnection(connection, httpMethod);
        connection.setInstanceFollowRedirects(false);
    }

    public ClientHttpRequestInterceptor redirectInterceptor() {
        return new RedirectInterceptor();
    }

    private class RedirectInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {
            ClientHttpResponse response = execution.execute(request, body);
            URI uri = request.getURI();
            HttpMethod method = request.getMethod();
            HttpHeaders headers = request.getHeaders();
            for (int redirects = 0; ; redirects++) {
                int status = response.getStatusCode().value();
                URI location = response.getHeaders().getLocation();
                if (!GuardedHttp.isRedirect(status) || location == null) {
                    return response;
                }
                response.close();
                if (redirects >= GuardedHttp.MAX_REDIRECTS) {
                    throw new BlockedUrlException("proxy.tooManyRedirects", "Too many redirects.",
                            Map.of("url", request.getURI().toString()));
                }
                uri = uri.resolve(location);
                boolean toGet = status == 303 || ((status == 301 || status == 302)
                        && method != HttpMethod.GET && method != HttpMethod.HEAD);
                if (toGet) {
                    method = HttpMethod.GET;
                }
                ClientHttpRequest next = createRequest(uri, method);
                boolean get = method == HttpMethod.GET;
                headers.forEach((name, values) -> {
                    if (!name.equalsIgnoreCase(HttpHeaders.HOST) && !name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)
                            && !(get && name.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE))) {
                        next.getHeaders().addAll(name, values);
                    }
                });
                if (method != HttpMethod.GET && method != HttpMethod.HEAD && body.length > 0) {
                    next.getBody().write(body);
                }
                response = next.execute();
            }
        }
    }
}
