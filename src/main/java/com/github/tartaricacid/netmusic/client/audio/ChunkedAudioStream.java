package com.github.tartaricacid.netmusic.client.audio;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetEaseMusic;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class ChunkedAudioStream extends InputStream {
    public static final int CHUNK_SIZE = 524288;
    private static final int CONNECT_TIMEOUT_MS = 12000;
    private static final int READ_TIMEOUT_MS = 12000;
    private static final int MAX_CHUNK_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 200L;

    private InputStream currentStream;
    private long currentStart;
    private final URL url;
    private long contentLength = -1;
    private final Proxy proxy;

    public ChunkedAudioStream(URL url, Proxy proxy) {
        this.url = url;
        this.currentStart = 0;
        this.proxy = proxy == null ? Proxy.NO_PROXY : proxy;
        this.currentStream = openChunk(this.currentStart);
        this.contentLength = getContentLength();
    }

    @Override
    public int read() throws IOException {
        if (this.currentStream == null) {
            return -1;
        }
        int b = this.currentStream.read();
        if (b != -1) {
            return b;
        }

        this.currentStream.close();
        this.currentStart += CHUNK_SIZE;
        this.currentStream = openChunk(this.currentStart);
        if (this.currentStream == null) {
            return -1;
        }
        return this.currentStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.currentStream == null) {
            return -1;
        }
        int bytesRead = this.currentStream.read(b, off, len);
        if (bytesRead != -1) {
            return bytesRead;
        }

        this.currentStream.close();
        this.currentStart += CHUNK_SIZE;
        this.currentStream = openChunk(this.currentStart);
        if (this.currentStream == null) {
            return -1;
        }
        return this.currentStream.read(b, off, len);
    }

    private InputStream openChunk(long start) {
        if (this.contentLength != -1 && start >= this.contentLength) {
            return null;
        }
        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_CHUNK_RETRIES; attempt++) {
            try {
                return openChunkOnce(start);
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_CHUNK_RETRIES) {
                    try {
                        Thread.sleep(RETRY_BACKOFF_MS * attempt);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        if (lastException != null) {
            NetMusic.LOGGER.warn("Failed to open audio chunk at {}: {}", start, lastException.getMessage());
        }
        return null;
    }

    private InputStream openChunkOnce(long start) throws IOException {
        URLConnection connection = this.url.openConnection(this.proxy);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        applyRequestHeaders(connection);
        connection.setRequestProperty("Range", "bytes=" + start + "-" + (start + CHUNK_SIZE - 1));

        if (connection instanceof HttpURLConnection httpConnection) {
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == 416) {
                return null;
            }
            boolean partialContent = responseCode == HttpURLConnection.HTTP_PARTIAL;
            if (start > 0 && !partialContent) {
                // Some endpoints may ignore Range and return the whole file from byte 0.
                // For subsequent chunks this would replay from the start, so stop safely.
                InputStream ignoredStream = httpConnection.getInputStream();
                if (ignoredStream != null) {
                    ignoredStream.close();
                }
                return null;
            }
        }
        return connection.getInputStream();
    }

    private long getContentLength() {
        if (this.contentLength != -1) {
            return this.contentLength;
        }
        try {
            URLConnection conn = this.url.openConnection(this.proxy);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            applyRequestHeaders(conn);
            this.contentLength = conn.getContentLengthLong();
            return this.contentLength;
        } catch (IOException e) {
            NetMusic.LOGGER.debug("Failed to fetch content length for {}: {}", this.url, e.getMessage());
            this.contentLength = -1;
            return -1;
        }
    }

    private void applyRequestHeaders(URLConnection connection) {
        connection.setRequestProperty("User-Agent", NetEaseMusic.getUserAgent());
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "keep-alive");

        String host = this.url.getHost() == null ? "" : this.url.getHost().toLowerCase(Locale.ROOT);
        if (host.contains("qq.com")) {
            connection.setRequestProperty("Referer", "https://y.qq.com/");
            connection.setRequestProperty("Origin", "https://y.qq.com");
            if (StringUtils.isNotBlank(GeneralConfig.QQ_VIP_COOKIE)) {
                connection.setRequestProperty("Cookie", GeneralConfig.QQ_VIP_COOKIE);
            }
            return;
        }
        if (host.contains("music.163.com")) {
            connection.setRequestProperty("Referer", NetEaseMusic.getReferer());
            if (StringUtils.isNotBlank(GeneralConfig.NETEASE_VIP_COOKIE)) {
                connection.setRequestProperty("Cookie", GeneralConfig.NETEASE_VIP_COOKIE);
            }
        }
    }
}
