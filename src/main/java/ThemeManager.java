import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
// Import các theme từ FlatLaf Extras (ĐÃ COMMENT OUT)
// import com.formdev.flatlaf.extras.themes.FlatSolarizedLightIJTheme;
// import com.formdev.flatlaf.extras.themes.FlatSolarizedDarkIJTheme;
// import com.formdev.flatlaf.extras.themes.FlatMonokaiProIJTheme;
// import com.formdev.flatlaf.extras.themes.FlatMaterialDesignDarkIJTheme;
// import com.formdev.flatlaf.extras.themes.FlatArcDarkIJTheme;
// import com.formdev.flatlaf.extras.themes.FlatHighContrastIJTheme; // Cái này là của FlatLaf core, nhưng tên class có thể gây nhầm với extras

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ThemeManager {

    public static class ThemeInfo {
        private final String name;
        private final String className;
        private final boolean isDark;

        public ThemeInfo(String name, String className, boolean isDark) {
            this.name = name;
            this.className = className;
            this.isDark = isDark;
        }

        public String getName() { return name; }
        public String getClassName() { return className; }
        public boolean isDark() { return isDark; }
        @Override public String toString() { return name; }
    }

    private static final List<ThemeInfo> availableThemes = new ArrayList<>();
    private static int currentThemeIndex = 0;
    private static final String PREF_KEY_THEME = "selectedThemeClassName_v2"; // Đổi key để tránh xung đột với lưu trữ cũ

    static {
        availableThemes.add(new ThemeInfo("Flat Light (Mặc định)", FlatLightLaf.class.getName(), false));
        availableThemes.add(new ThemeInfo("Flat Dark", FlatDarkLaf.class.getName(), true));
        availableThemes.add(new ThemeInfo("Flat IntelliJ", FlatIntelliJLaf.class.getName(), false));
        availableThemes.add(new ThemeInfo("Flat Darcula", FlatDarculaLaf.class.getName(), true));

        // FlatHighContrastIJTheme là một theme chuẩn của FlatLaf, không phải extras.
        // Tuy nhiên, để tránh lỗi nếu có nhầm lẫn, ta có thể kiểm tra class trước khi dùng.
        try {
            Class.forName("com.formdev.flatlaf.themes.FlatHighContrastIJTheme"); // Package đúng là themes, không phải extras.themes
            availableThemes.add(new ThemeInfo("Flat High Contrast", "com.formdev.flatlaf.themes.FlatHighContrastIJTheme", true));
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy FlatHighContrastIJTheme. Kiểm tra lại thư viện FlatLaf core.");
        }


        // Các theme từ FlatLaf Extras (ĐÃ COMMENT OUT)
        // Bro cần đảm bảo đã thêm flatlaf-extras.jar vào project để bỏ comment các dòng này
        /*
        try {
            Class.forName("com.formdev.flatlaf.extras.themes.FlatSolarizedLightIJTheme");
            availableThemes.add(new ThemeInfo("Solarized Light", "com.formdev.flatlaf.extras.themes.FlatSolarizedLightIJTheme", false));

            Class.forName("com.formdev.flatlaf.extras.themes.FlatSolarizedDarkIJTheme");
            availableThemes.add(new ThemeInfo("Solarized Dark", "com.formdev.flatlaf.extras.themes.FlatSolarizedDarkIJTheme", true));

            Class.forName("com.formdev.flatlaf.extras.themes.FlatMonokaiProIJTheme");
            availableThemes.add(new ThemeInfo("Monokai Pro", "com.formdev.flatlaf.extras.themes.FlatMonokaiProIJTheme", true));

            Class.forName("com.formdev.flatlaf.extras.themes.FlatMaterialDesignDarkIJTheme");
            availableThemes.add(new ThemeInfo("Material Darker", "com.formdev.flatlaf.extras.themes.FlatMaterialDesignDarkIJTheme", true));

            Class.forName("com.formdev.flatlaf.extras.themes.FlatArcDarkIJTheme");
            availableThemes.add(new ThemeInfo("Arc Dark", "com.formdev.flatlaf.extras.themes.FlatArcDarkIJTheme", true));

        } catch (ClassNotFoundException e) {
            System.err.println("Một hoặc nhiều theme từ FlatLaf Extras không tìm thấy. Hãy đảm bảo flatlaf-extras.jar đã được thêm vào classpath.");
        }
        */
    }

    public static List<ThemeInfo> getAvailableThemes() {
        return new ArrayList<>(availableThemes);
    }

    public static ThemeInfo getCurrentThemeInfo() {
        if (availableThemes.isEmpty()) {
            return new ThemeInfo("Flat Light (Fallback)", FlatLightLaf.class.getName(), false);
        }
        // Đảm bảo currentThemeIndex luôn hợp lệ
        if (currentThemeIndex < 0 || currentThemeIndex >= availableThemes.size()) {
            currentThemeIndex = 0; // Reset về theme đầu tiên nếu index không hợp lệ
        }
        return availableThemes.get(currentThemeIndex);
    }

    public static void applyTheme(String className, Component rootComponentToUpdate) {
        try {
            // Tìm theme trong danh sách để cập nhật currentThemeIndex và isDark
            boolean themeFoundInList = false;
            for (int i = 0; i < availableThemes.size(); i++) {
                if (availableThemes.get(i).getClassName().equals(className)) {
                    currentThemeIndex = i;
                    themeFoundInList = true;
                    break;
                }
            }

            if (!themeFoundInList) {
                // Nếu theme không có trong danh sách (ví dụ, được load từ preference nhưng list đã thay đổi)
                // thì thử áp dụng trực tiếp, nhưng currentThemeIndex sẽ không chính xác.
                // Tốt hơn là fallback về theme mặc định.
                System.err.println("Theme '" + className + "' không có trong danh sách availableThemes. Áp dụng theme mặc định.");
                if (!availableThemes.isEmpty()) {
                    applyTheme(availableThemes.get(0).getClassName(), rootComponentToUpdate); // Áp dụng theme đầu tiên
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf()); // Fallback cứng
                }
                return;
            }

            UIManager.setLookAndFeel(className);

            if (rootComponentToUpdate != null) {
                SwingUtilities.updateComponentTreeUI(rootComponentToUpdate);
            } else {
                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                }
            }
            saveThemePreference(className);
            System.out.println("Đã áp dụng theme: " + className);
        } catch (Exception e) {
            System.err.println("Không thể áp dụng theme '" + className + "': " + e.getMessage());
            // e.printStackTrace(); // Bỏ comment nếu cần debug sâu
        }
    }

    public static void cycleNextTheme(Component rootComponentToUpdate) {
        if (availableThemes.isEmpty()) {
            System.err.println("Không có theme nào để chuyển đổi.");
            return;
        }
        currentThemeIndex = (currentThemeIndex + 1) % availableThemes.size();
        applyTheme(availableThemes.get(currentThemeIndex).getClassName(), rootComponentToUpdate);
    }

    public static void loadAndApplyPreferredTheme(Component rootComponentToUpdate) {
        Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
        String defaultThemeClass = FlatLightLaf.class.getName();
        if (!availableThemes.isEmpty()) {
            defaultThemeClass = availableThemes.get(0).getClassName();
        }

        String preferredThemeClass = prefs.get(PREF_KEY_THEME, defaultThemeClass);

        boolean found = false;
        for (ThemeInfo themeInfo : availableThemes) {
            if (themeInfo.getClassName().equals(preferredThemeClass)) {
                found = true;
                break;
            }
        }
        if (!found) {
            System.out.println("Theme đã lưu '" + preferredThemeClass + "' không có trong danh sách hiện tại. Sử dụng theme mặc định: " + defaultThemeClass);
            preferredThemeClass = defaultThemeClass;
        }

        System.out.println("Đang tải theme ưu tiên: " + preferredThemeClass);
        applyTheme(preferredThemeClass, rootComponentToUpdate);
    }

    private static void saveThemePreference(String className) {
        Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
        prefs.put(PREF_KEY_THEME, className);
        System.out.println("Đã lưu theme ưu tiên: " + className);
    }

    public static boolean isCurrentThemeDark() {
        if (availableThemes.isEmpty() || currentThemeIndex < 0 || currentThemeIndex >= availableThemes.size()) {
            return false;
        }
        return availableThemes.get(currentThemeIndex).isDark();
    }
}
