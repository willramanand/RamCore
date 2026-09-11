package dev.willram.ramcore.resourcepack;

import com.sun.net.httpserver.HttpServer;
import dev.willram.ramcore.terminable.Terminable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * A tiny built-in HTTP server (JDK {@link HttpServer}) that serves a single resource-pack zip so the
 * server can hand clients a URL. The served file can be swapped with {@link #setPack(Path)} after a
 * rebuild. For production behind a real web server or CDN, host the zip externally instead and skip
 * this class.
 *
 * <p>All requests return the current zip; the request path is ignored, so any URL under the host
 * works. Bind to an address reachable by clients (not loopback) in production.</p>
 */
public final class ResourcePackHost implements Terminable {
    private final HttpServer server;
    private volatile Path pack;
    private volatile boolean closed;

    private ResourcePackHost(@NotNull HttpServer server, @NotNull Path pack) {
        this.server = server;
        this.pack = pack;
    }

    /**
     * Starts a host on the given address serving the zip at {@code pack}.
     *
     * @param address the bind address (host + port; port 0 picks a free port)
     * @param pack    the zip to serve
     * @return the running host
     * @throws IOException if the server cannot bind
     */
    @NotNull
    public static ResourcePackHost start(@NotNull InetSocketAddress address, @NotNull Path pack) throws IOException {
        requireNonNull(address, "address");
        requireNonNull(pack, "pack");
        HttpServer server = HttpServer.create(address, 0);
        ResourcePackHost host = new ResourcePackHost(server, pack);
        server.createContext("/", exchange -> {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                byte[] bytes = Files.readAllBytes(host.pack);
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            } catch (IOException failure) {
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        });
        server.start();
        return host;
    }

    /**
     * Starts a host bound to {@code bindHost:port}.
     *
     * @param bindHost the bind host
     * @param port     the port (0 for a free port)
     * @param pack     the zip to serve
     * @return the running host
     * @throws IOException if the server cannot bind
     */
    @NotNull
    public static ResourcePackHost start(@NotNull String bindHost, int port, @NotNull Path pack) throws IOException {
        return start(new InetSocketAddress(bindHost, port), pack);
    }

    /** The bound port. */
    public int port() {
        return this.server.getAddress().getPort();
    }

    /** Swaps the served zip (after a rebuild). */
    public void setPack(@NotNull Path pack) {
        this.pack = requireNonNull(pack, "pack");
    }

    /**
     * The download URL clients should use, given the publicly reachable host.
     *
     * @param publicHost the host clients can reach (IP or domain)
     * @param fileName   the file name to expose in the URL
     * @return the URL
     */
    @NotNull
    public URI uri(@NotNull String publicHost, @NotNull String fileName) {
        return URI.create("http://" + requireNonNull(publicHost, "publicHost") + ":" + port() + "/"
                + requireNonNull(fileName, "fileName"));
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.server.stop(0);
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }
}
