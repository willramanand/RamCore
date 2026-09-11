package dev.willram.ramcore.resourcepack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ResourcePackHostTest {

    @Test
    public void servesTheZipBytes(@TempDir Path dir) throws IOException, InterruptedException {
        Path zip = dir.resolve("pack.zip");
        byte[] content = {10, 20, 30, 40};
        Files.write(zip, content);

        try (ResourcePackHost host = ResourcePackHost.start("127.0.0.1", 0, zip)) {
            assertTrue(host.port() > 0);
            HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + host.port() + "/pack.zip")).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode());
            assertArrayEquals(content, response.body());
            assertEquals("application/zip", response.headers().firstValue("Content-Type").orElse(""));
        }
    }

    @Test
    public void servesUpdatedPackAfterSwap(@TempDir Path dir) throws IOException, InterruptedException {
        Path first = dir.resolve("a.zip");
        Path second = dir.resolve("b.zip");
        Files.write(first, new byte[]{1});
        Files.write(second, new byte[]{2, 2});

        try (ResourcePackHost host = ResourcePackHost.start("127.0.0.1", 0, first)) {
            host.setPack(second);
            HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(host.uri("127.0.0.1", "pack.zip")).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertArrayEquals(new byte[]{2, 2}, response.body());
        }
    }
}
