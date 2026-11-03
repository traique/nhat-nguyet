package vn.nhatnguyet.net;

import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import vn.nhatnguyet.BuildConfig;

/** HTTP client nhỏ dựa trên HttpURLConnection, chạy được từ Android 4.0.3. */
public final class Http {
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final int MAX_TEXT = 2 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 6;

    public static final String UA = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE
            + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 NhatNguyet/"
            + BuildConfig.VERSION_NAME;

    public interface Progress {
        void onProgress(long done, long total);
    }

    private Http() {}

    public static byte[] get(String url) throws IOException {
        return request("GET", url, null, MAX_TEXT);
    }

    public static String getText(String url) throws IOException {
        return new String(get(url), UTF8);
    }

    public static String postJson(String url, String json) throws IOException {
        return new String(request("POST", url, json.getBytes(UTF8), MAX_TEXT), UTF8);
    }

    private static byte[] request(String method, String url, byte[] body, int maxBytes) throws IOException {
        HttpURLConnection c = open(method, url, body);
        try {
            InputStream in = c.getInputStream();
            try {
                return readAll(in, maxBytes);
            } finally {
                closeQuietly(in);
            }
        } finally {
            c.disconnect();
        }
    }

    /** Tải file lớn (APK) về đĩa, có báo tiến độ. */
    public static void download(String url, File dest, long maxBytes, Progress progress) throws IOException {
        HttpURLConnection c = open("GET", url, null);
        try {
            long total = -1;
            String len = c.getHeaderField("Content-Length");
            if (len != null) {
                try {
                    total = Long.parseLong(len.trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if (total > maxBytes) throw new IOException("Tệp quá lớn");
            InputStream in = c.getInputStream();
            OutputStream out = new FileOutputStream(dest);
            try {
                byte[] buf = new byte[16 * 1024];
                long done = 0;
                long lastReport = 0;
                int r;
                while ((r = in.read(buf)) >= 0) {
                    out.write(buf, 0, r);
                    done += r;
                    if (done > maxBytes) throw new IOException("Tệp quá lớn");
                    if (progress != null && done - lastReport > 64 * 1024) {
                        lastReport = done;
                        progress.onProgress(done, total);
                    }
                }
                out.flush();
                if (progress != null) progress.onProgress(done, total);
            } finally {
                closeQuietly(out);
                closeQuietly(in);
            }
        } finally {
            c.disconnect();
        }
    }

    /** Mở kết nối và tự theo chuyển hướng (kể cả sang máy chủ khác, vd. GitHub Release). */
    private static HttpURLConnection open(String method, String url, byte[] body) throws IOException {
        String current = url;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            long sentAt = System.currentTimeMillis();
            URL u = new URL(current);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            if (c instanceof HttpsURLConnection) {
                SSLSocketFactory f = Tls.factory();
                if (f != null) ((HttpsURLConnection) c).setSSLSocketFactory(f);
            }
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setUseCaches(false);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Accept-Language", "vi-VN,vi;q=0.9,en;q=0.5");
            c.setRequestProperty("Accept", "*/*");
            boolean send = body != null && hop == 0;
            if (send) {
                c.setRequestMethod(method);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setFixedLengthStreamingMode(body.length);
                OutputStream os = c.getOutputStream();
                try {
                    os.write(body);
                } finally {
                    closeQuietly(os);
                }
            } else {
                c.setRequestMethod(hop == 0 ? method : "GET");
            }
            int code;
            try {
                code = c.getResponseCode();
            } catch (IOException e) {
                c.disconnect();
                throw e;
            }
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) throw new IOException("HTTP " + code);
                current = new URL(u, loc).toString();
                if (code == 303) body = null;
                continue;
            }
            if (code < 200 || code >= 300) {
                c.disconnect();
                throw new HttpStatusException(code);
            }
            // Tiêu đề Date (độ chính xác 1 giây) để phát hiện giờ máy lệch; bỏ qua bản lấy từ bộ đệm.
            long date = c.getDate();
            if (date > 0 && c.getHeaderField("Age") == null) {
                ClockCheck.sample(date, (sentAt + System.currentTimeMillis()) / 2);
            }
            return c;
        }
        throw new IOException("Chuyển hướng quá nhiều lần");
    }

    public static byte[] readAll(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        int r;
        while ((r = in.read(buf)) >= 0) {
            bos.write(buf, 0, r);
            if (bos.size() > maxBytes) throw new IOException("Phản hồi quá lớn");
        }
        return bos.toByteArray();
    }

    public static void closeQuietly(java.io.Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }

    /** Diễn giải lỗi mạng thành câu tiếng Việt ngắn để hiện trên bảng. */
    public static String describe(Throwable t) {
        if (t instanceof HttpStatusException) return "máy chủ báo lỗi " + ((HttpStatusException) t).code;
        if (t instanceof UnknownHostException) return "không có mạng";
        if (t instanceof SocketTimeoutException) return "mạng chậm, hết thời gian chờ";
        if (t instanceof SSLException) {
            int year = Calendar.getInstance().get(Calendar.YEAR);
            if (year < 2025) return "giờ máy sai (năm " + year + "), hãy chỉnh ngày giờ";
            return "kết nối bảo mật thất bại";
        }
        String m = t.getMessage();
        if (m == null || m.length() == 0) m = t.getClass().getSimpleName();
        if (m.length() > 60) m = m.substring(0, 60) + "…";
        return m;
    }

    public static final class HttpStatusException extends IOException {
        public final int code;

        public HttpStatusException(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }
}
