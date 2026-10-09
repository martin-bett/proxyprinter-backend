package my.poo.mtgcrawl.mtgprint.service.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

/**
 * Schickt eine Anfrage mit einem HttpClient ohne automatische Weiterleitungen (Redirect.NEVER)
 * und folgt Weiterleitungen selbst, wobei jedes Ziel durch den OutboundUrlGuard muss.
 * Verhalten wie Redirect.NORMAL: 301/302/303 werden zu GET (außer 301/302 bei GET/HEAD), 307/308 behalten Methode und Body.
 */
public final class GuardedHttp {

    public static final int MAX_REDIRECTS = 5;

    private GuardedHttp() {
    }

    public static <T> HttpResponse<T> send(HttpClient client, OutboundUrlGuard guard, HttpRequest request,
            HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        guard.check(request.uri());
        HttpRequest current = request;
        for (int redirects = 0; ; redirects++) {
            HttpResponse<T> response = client.send(current, handler);
            Optional<String> location = response.headers().firstValue("Location");
            if (!isRedirect(response.statusCode()) || location.isEmpty()) {
                return response;
            }
            if (redirects >= MAX_REDIRECTS) {
                close(response);
                throw new BlockedUrlException("proxy.tooManyRedirects", "Too many redirects.",
                        Map.of("url", request.uri().toString()));
            }
            URI next = guard.check(current.uri().resolve(location.get()));
            close(response);
            current = redirect(current, next, response.statusCode());
        }
    }

    static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static HttpRequest redirect(HttpRequest previous, URI next, int status) {
        String method = previous.method();
        boolean toGet = status == 303 || ((status == 301 || status == 302) && !method.equals("GET") && !method.equals("HEAD"));
        if (toGet) {
            return HttpRequest.newBuilder(previous, (name, value) -> !name.equalsIgnoreCase("Content-Type"))
                    .uri(next).GET().build();
        }
        return HttpRequest.newBuilder(previous, (name, value) -> true).uri(next).build();
    }

    private static void close(HttpResponse<?> response) {
        if (response.body() instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // Body einer Weiterleitung wird nicht gebraucht
            }
        }
    }
}
