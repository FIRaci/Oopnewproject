import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.io.*;

public class UIConfig {
    public static void applyCustomCSS() {
        try {
            // Đọc file CSS
            String cssPath = "custom-style.css";
            String cssContent = new String(Files.readAllBytes(Paths.get(cssPath)));

            // Áp dụng giao diện tùy chỉnh cho các thành phần cụ thể
            UIManager.put("Button.background", new Color(106, 17, 203)); // #6a11cb
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 14));
            UIManager.put("Button.border", BorderFactory.createEmptyBorder(8, 16, 8, 16));
            UIManager.put("Button.arc", 12);

            UIManager.put("Panel.background", new Color(244, 246, 249)); // #f4f6f9
            UIManager.put("Label.foreground", new Color(45, 45, 45));

            UIManager.put("Table.background", Color.WHITE);
            UIManager.put("Table.foreground", new Color(45, 45, 45));
            UIManager.put("Table.selectionBackground", new Color(230, 234, 255)); // #e6eaff
            UIManager.put("Table.selectionForeground", Color.BLACK);
            UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("Table.gridColor", new Color(220, 220, 220));

            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("TextField.foreground", new Color(45, 45, 45));
            UIManager.put("TextField.border", BorderFactory.createLineBorder(new Color(221, 221, 221), 2));
            UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));

            UIManager.put("List.background", Color.WHITE);
            UIManager.put("List.foreground", new Color(45, 45, 45));
            UIManager.put("List.selectionBackground", new Color(230, 234, 255));
            UIManager.put("List.selectionForeground", Color.BLACK);

            UIManager.put("Dialog.background", Color.WHITE);
            UIManager.put("Dialog.border", BorderFactory.createLineBorder(new Color(221, 221, 221), 2));

        } catch (IOException e) {
            System.err.println("Error loading CSS file: " + e.getMessage());
        }
    }
}
