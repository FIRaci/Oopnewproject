import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatDarculaLaf;

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
    private static final String PREF_KEY_THEME = "selectedThemeClassName_v2";

    static {
        availableThemes.add(new ThemeInfo("Flat Light (Mặc định)", FlatLightLaf.class.getName(), false));
        availableThemes.add(new ThemeInfo("Flat Dark", FlatDarkLaf.class.getName(), true));
        availableThemes.add(new ThemeInfo("Flat IntelliJ", FlatIntelliJLaf.class.getName(), false));
        availableThemes.add(new ThemeInfo("Flat Darcula", FlatDarculaLaf.class.getName(), true));

        try {
            Class.forName("com.formdev.flatlaf.themes.FlatHighContrastIJTheme");
            availableThemes.add(new ThemeInfo("Flat High Contrast", "com.formdev.flatlaf.themes.FlatHighContrastIJTheme", true));
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy FlatHighContrastIJTheme. Kiểm tra lại thư viện FlatLaf core.");
        }

    }

    public static List<ThemeInfo> getAvailableThemes() {
        return new ArrayList<>(availableThemes);
    }

    public static ThemeInfo getCurrentThemeInfo() {
        if (availableThemes.isEmpty()) {
            return new ThemeInfo("Flat Light (Fallback)", FlatLightLaf.class.getName(), false);
        }
        if (currentThemeIndex < 0 || currentThemeIndex >= availableThemes.size()) {
            currentThemeIndex = 0;
        }
        return availableThemes.get(currentThemeIndex);
    }

    public static void applyTheme(String className, Component rootComponentToUpdate) {
        try {
            boolean themeFoundInList = false;
            for (int i = 0; i < availableThemes.size(); i++) {
                if (availableThemes.get(i).getClassName().equals(className)) {
                    currentThemeIndex = i;
                    themeFoundInList = true;
                    break;
                }
            }

            if (!themeFoundInList) {
                System.err.println("Theme '" + className + "' không có trong danh sách availableThemes. Áp dụng theme mặc định.");
                if (!availableThemes.isEmpty()) {
                    applyTheme(availableThemes.get(0).getClassName(), rootComponentToUpdate);
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
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
