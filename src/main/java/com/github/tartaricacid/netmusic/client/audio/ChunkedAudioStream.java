package com.github.tartaricacid.netmusic.client.audio;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetEaseMusic;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class ChunkedAudioStream extends InputStream {
    public static final int CHUNK_SIZE = 81920;
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
        try {
            if (this.contentLength != -1 && start >= this.contentLength) {
                return null;
            }
            URLConnection conn = this.url.openConnection(this.proxy);
            conn.setConnectTimeout(3_000);
            conn.setReadTimeout(3_000);
            applyRequestHeaders(conn);
            conn.setRequestProperty("Range", "bytes=" + start + "-" + (start + CHUNK_SIZE - 1));
            return conn.getInputStream();
        } catch (IOException e) {
            NetMusic.LOGGER.error("Failed to open audio chunk at {}: {}", start, e.getMessage());
            return null;
        }
    }

    private long getContentLength() {
        if (this.contentLength != -1) {
            return this.contentLength;
        }
        try {
            URLConnection conn = this.url.openConnection(this.proxy);
            applyRequestHeaders(conn);
            this.contentLength = conn.getContentLengthLong();
            return this.contentLength;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyRequestHeaders(URLConnection connection) {
        connection.setRequestProperty("User-Agent", NetEaseMusic.getUserAgent());

        String host = this.url.getHost() == null ? "" : this.url.getHost().toLowerCase(Locale.ROOT);
        if (host.contains("qq.com")) {
            connection.setRequestProperty("Referer", "https://y.qq.com/");
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
