import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;


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
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", "start /min ollama serve && timeout /t 2 && ollama run gemma3:1b"
            );
            pb.redirectErrorStream(true);
            pb.start();
            System.out.println("[NoteApplication] Đã khởi động ollama serve và model gemma3:1b (nếu chưa chạy).");
        } catch (Exception ex) {
            System.err.println("[NoteApplication] Lỗi khi khởi động ollama serve/model: " + ex.getMessage());
        }

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
