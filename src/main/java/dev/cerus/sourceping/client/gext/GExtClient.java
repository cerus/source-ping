package dev.cerus.sourceping.client.gext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class GExtClient {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLongSerializationPolicy(LongSerializationPolicy.STRING).create();

    private final ExecutorService executorService;
    private final String baseUrl;

    public GExtClient(final ExecutorService executorService, final String baseUrl) {
        this.executorService = executorService;
        this.baseUrl = baseUrl;
    }

    CompletableFuture<List<Player>> getPlayers(final int serverId) {
        return this.execAsync(this.baseUrl + "/request.php?t=servers&serverid=" + serverId, "GET", bytes -> {
            final JsonElement result = JsonParser.parseString(new String(bytes));
            return GSON.fromJson(result.getAsJsonObject().get("players"), new TypeToken<>() {});
        });
    }

    private <T> CompletableFuture<T> execAsync(final String url, final String method, final Function<byte[], T> transformer) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        this.executorService.submit(() -> {
            try {
                final byte[] bytes = this.execRaw(url, method);
                future.complete(transformer.apply(bytes));
            } catch (final Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private byte[] execRaw(final String url, final String method) throws IOException {
        final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod(method);
        con.setRequestProperty("User-Agent", "cerus/source-ping");
        con.setConnectTimeout(5 * 1000);
        con.setReadTimeout(5 * 1000);

        con.setDoInput(true);
        InputStream in;
        try {
            in = con.getInputStream();
        } catch (final IOException ignored) {
            in = con.getErrorStream();
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[512];
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        return out.toByteArray();
    }

}
