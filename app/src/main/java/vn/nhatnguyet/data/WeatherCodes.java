package vn.nhatnguyet.data;

/** Mã thời tiết WMO (Open-Meteo) -> mô tả tiếng Việt và loại biểu tượng. */
public final class WeatherCodes {
    public static final int CLEAR = 0;
    public static final int PARTLY = 1;
    public static final int CLOUDY = 2;
    public static final int FOG = 3;
    public static final int DRIZZLE = 4;
    public static final int RAIN = 5;
    public static final int SNOW = 6;
    public static final int STORM = 7;

    private WeatherCodes() {}

    public static String text(int code) {
        switch (code) {
            case 0: return "Trời quang";
            case 1: return "Ít mây";
            case 2: return "Có mây";
            case 3: return "Nhiều mây, u ám";
            case 45: return "Sương mù";
            case 48: return "Sương mù đóng băng";
            case 51: return "Mưa phùn nhẹ";
            case 53: return "Mưa phùn";
            case 55: return "Mưa phùn dày";
            case 56: return "Mưa phùn lạnh";
            case 57: return "Mưa phùn lạnh dày";
            case 61: return "Mưa nhỏ";
            case 63: return "Mưa vừa";
            case 65: return "Mưa to";
            case 66: return "Mưa lạnh";
            case 67: return "Mưa lạnh to";
            case 71: return "Tuyết nhẹ";
            case 73: return "Tuyết";
            case 75: return "Tuyết dày";
            case 77: return "Hạt tuyết";
            case 80: return "Mưa rào nhẹ";
            case 81: return "Mưa rào";
            case 82: return "Mưa rào rất to";
            case 85: return "Mưa tuyết";
            case 86: return "Mưa tuyết to";
            case 95: return "Dông";
            case 96: return "Dông kèm mưa đá";
            case 99: return "Dông mạnh kèm mưa đá";
            default: return "Thời tiết";
        }
    }

    public static int kind(int code) {
        if (code == 0) return CLEAR;
        if (code == 1 || code == 2) return PARTLY;
        if (code == 3) return CLOUDY;
        if (code == 45 || code == 48) return FOG;
        if (code >= 51 && code <= 57) return DRIZZLE;
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return RAIN;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return SNOW;
        if (code >= 95) return STORM;
        return CLOUDY;
    }
}
