package vn.nhatnguyet;

import android.app.Application;

import vn.nhatnguyet.net.Tls;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Theme.loadFonts(this);
        Tls.install(this);
    }
}
