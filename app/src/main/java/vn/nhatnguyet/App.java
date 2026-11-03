package vn.nhatnguyet;

import android.app.Application;

import vn.nhatnguyet.lunar.Lunar;
import vn.nhatnguyet.lunar.PersonalDays;
import vn.nhatnguyet.net.Tls;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Theme.loadFonts(this);
        Tls.install(this);
        Lunar.setPersonal(PersonalDays.parse(new Prefs(this).personalDaysRaw(), null));
    }
}
