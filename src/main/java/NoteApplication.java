import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.io.*;
import java.util.Enumeration; // Cho setUIFont
import java.time.LocalDateTime; // Giữ lại nếu initializeSampleData dùng
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class NoteApplication {

    private static void setUIFont(javax.swing.plaf.FontUIResource f) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    public static void main(String[] args) {
        ThemeManager.loadAndApplyPreferredTheme(null);
        setUIFont(new javax.swing.plaf.FontUIResource("SansSerif", Font.PLAIN, 13));

        SwingUtilities.invokeLater(() -> {
            System.out.println("[NoteApplication] EDT: Bắt đầu khởi tạo ứng dụng...");

            NoteManager noteManager = new NoteManager();
            NoteService noteService = new NoteService(noteManager);
            NoteController controller = new NoteController(null, noteService);
            MainFrame mainFrame = new MainFrame(controller);
            controller.setMainFrameInstance(mainFrame);

            System.out.println("[NoteApplication] EDT: Đặt MainFrame thành visible.");
            mainFrame.setVisible(true);

            SwingUtilities.invokeLater(() -> {
                System.out.println("[NoteApplication] EDT (inner): Đang làm mới MainMenuScreen.");
                mainFrame.showMainMenuScreen();
            });

            System.out.println("[NoteApplication] EDT: Hoàn tất khởi tạo ứng dụng trên EDT.");
        });
    }

}
