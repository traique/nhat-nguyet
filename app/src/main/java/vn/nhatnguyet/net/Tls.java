package vn.nhatnguyet.net;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import org.conscrypt.Conscrypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import vn.nhatnguyet.R;

/**
 * Thiết lập TLS dùng chung cho toàn app.
 *
 * Android 4.0.x không có TLS 1.2 (4.1–4.4 có nhưng tắt sẵn), trong khi hầu hết
 * máy chủ hiện nay đòi TLS 1.2+. Trên Android < 5.0 app nạp Conscrypt (BoringSSL
 * đóng gói sẵn) làm nhà cung cấp TLS. Kho chứng chỉ tin cậy = chứng chỉ của máy
 * + bộ gốc Mozilla nhúng trong app (máy cũ thiếu Let's Encrypt, Sectigo ECC…).
 */
public final class Tls {
    private static final String TAG = "Tls";
    private static volatile SSLSocketFactory factory;
    private static volatile String engine = "system";

    private static volatile Context appContext;

    private Tls() {}

    /** Gọi trong Application.onCreate: nạp kho chứng chỉ ở luồng nền. */
    public static void install(Context context) {
        appContext = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                init(appContext);
            }
        }, "nn-tls").start();
    }

    public static synchronized void init(Context context) {
        if (factory != null) return;
        boolean conscrypt = false;
        if (Build.VERSION.SDK_INT < 21) {
            try {
                if (Conscrypt.isAvailable()) {
                    Provider p = Conscrypt.newProvider();
                    Security.insertProviderAt(p, 1);
                    conscrypt = true;
                }
            } catch (Throwable t) {
                Log.w(TAG, "Conscrypt unavailable", t);
            }
        }
        try {
            X509TrustManager tm = trustManager(context.getApplicationContext());
            SSLContext ctx = conscrypt ? SSLContext.getInstance("TLS", "Conscrypt") : SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{tm}, null);
            factory = new ModernSocketFactory(ctx.getSocketFactory(), conscrypt);
            engine = conscrypt ? "Conscrypt" : "system";
            HttpsURLConnection.setDefaultSSLSocketFactory(factory);
        } catch (Throwable t) {
            Log.e(TAG, "TLS init failed, falling back to platform default", t);
            factory = HttpsURLConnection.getDefaultSSLSocketFactory();
            engine = "default";
        }
    }

    public static SSLSocketFactory factory() {
        SSLSocketFactory f = factory;
        if (f == null && appContext != null) {
            init(appContext);
            f = factory;
        }
        return f;
    }

    /** "Conscrypt" hoặc "system" – hiển thị trong màn Cài đặt để dễ kiểm tra. */
    public static String engine() {
        return engine;
    }

    private static X509TrustManager trustManager(Context context) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        int n = 0;
        // 1) Chứng chỉ hệ thống + người dùng cài thêm.
        try {
            KeyStore sys = KeyStore.getInstance("AndroidCAStore");
            sys.load(null, null);
            List<String> aliases = Collections.list(sys.aliases());
            for (String alias : aliases) {
                Certificate c = sys.getCertificate(alias);
                if (c != null) {
                    ks.setCertificateEntry("sys:" + alias, c);
                    n++;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "AndroidCAStore unavailable", t);
        }
        // 2) Bộ gốc Mozilla nhúng sẵn.
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        int i = 0;
        for (byte[] der : readPemBundle(context)) {
            try {
                Certificate c = cf.generateCertificate(new ByteArrayInputStream(der));
                ks.setCertificateEntry("app:" + (i++), c);
                n++;
            } catch (Throwable t) {
                // Bỏ qua chứng chỉ mà máy cũ không đọc được (vd. thuật toán lạ).
            }
        }
        Log.i(TAG, "trust anchors: " + n);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) return (X509TrustManager) tm;
        }
        throw new IllegalStateException("No X509TrustManager");
    }

    private static List<byte[]> readPemBundle(Context context) throws IOException {
        InputStream in = context.getResources().openRawResource(R.raw.cacerts);
        String text;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) >= 0) bos.write(buf, 0, r);
            text = new String(bos.toByteArray(), Charset.forName("US-ASCII"));
        } finally {
            in.close();
        }
        List<byte[]> out = new ArrayList<byte[]>();
        String begin = "-----BEGIN CERTIFICATE-----";
        String end = "-----END CERTIFICATE-----";
        int pos = 0;
        while (true) {
            int b = text.indexOf(begin, pos);
            if (b < 0) break;
            int e = text.indexOf(end, b);
            if (e < 0) break;
            String body = text.substring(b + begin.length(), e).replaceAll("\\s", "");
            try {
                out.add(Base64.decode(body, Base64.DEFAULT));
            } catch (IllegalArgumentException ignored) {
            }
            pos = e + end.length();
        }
        return out;
    }

    /** Bật TLS 1.2/1.3 và SNI cho mọi socket tạo ra. */
    static final class ModernSocketFactory extends SSLSocketFactory {
        private static final String[] WANT = {"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"};
        private final SSLSocketFactory delegate;
        private final boolean conscrypt;

        ModernSocketFactory(SSLSocketFactory delegate, boolean conscrypt) {
            this.delegate = delegate;
            this.conscrypt = conscrypt;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return patch(delegate.createSocket(s, host, port, autoClose), host);
        }

        @Override
        public Socket createSocket() throws IOException {
            return patch(delegate.createSocket(), null);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return patch(delegate.createSocket(host, port), host);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return patch(delegate.createSocket(host, port, localHost, localPort), host);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return patch(delegate.createSocket(host, port), null);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return patch(delegate.createSocket(address, port, localAddress, localPort), null);
        }

        private Socket patch(Socket s, String host) {
            if (!(s instanceof SSLSocket)) return s;
            SSLSocket ssl = (SSLSocket) s;
            try {
                String[] supported = ssl.getSupportedProtocols();
                List<String> enable = new ArrayList<String>();
                for (String w : WANT) {
                    for (String p : supported) {
                        if (w.equals(p)) {
                            enable.add(w);
                            break;
                        }
                    }
                }
                if (!enable.isEmpty()) ssl.setEnabledProtocols(enable.toArray(new String[0]));
            } catch (Throwable ignored) {
            }
            if (conscrypt && host != null) {
                try {
                    if (Conscrypt.isConscrypt(ssl)) {
                        Conscrypt.setHostname(ssl, host);
                        Conscrypt.setUseSessionTickets(ssl, true);
                    }
                } catch (Throwable ignored) {
                }
            }
            return ssl;
        }
    }
}
