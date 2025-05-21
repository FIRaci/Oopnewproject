import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
            UIManager.setLookAndFeel(new FlatLightLaf());
            setUIFont(new javax.swing.plaf.FontUIResource("SansSerif", Font.PLAIN, 13));
        } catch (Exception e) {
            System.err.println("Không thể cài đặt giao diện FlatLaf hoặc Font: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            System.out.println("[NoteApplication] EDT: Bắt đầu khởi tạo ứng dụng...");

            // 1. Khởi tạo NoteManager
            System.out.println("[NoteApplication] EDT: Đang tạo NoteManager...");
            NoteManager noteManager = new NoteManager();
            System.out.println("[NoteApplication] EDT: NoteManager đã tạo.");
            if (noteManager == null) { // Kiểm tra cực kỳ cẩn thận
                System.err.println("LỖI NGHIÊM TRỌNG: NoteManager là null sau khi tạo!");
                JOptionPane.showMessageDialog(null, "Lỗi nghiêm trọng: NoteManager không thể khởi tạo.", "Lỗi Khởi Động", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2. Khởi tạo NoteService
            System.out.println("[NoteApplication] EDT: Đang tạo NoteService...");
            NoteService noteService = new NoteService(noteManager);
            System.out.println("[NoteApplication] EDT: NoteService đã tạo.");
            if (noteService == null) { // Kiểm tra cực kỳ cẩn thận
                System.err.println("LỖI NGHIÊM TRỌNG: NoteService là null sau khi tạo!");
                JOptionPane.showMessageDialog(null, "Lỗi nghiêm trọng: NoteService không thể khởi tạo.", "Lỗi Khởi Động", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 3. Khởi tạo NoteController (ban đầu không có tham chiếu frame)
            System.out.println("[NoteApplication] EDT: Đang tạo NoteController...");
            NoteController controller = new NoteController(null, noteService);
            System.out.println("[NoteApplication] EDT: NoteController đã tạo.");
            if (controller == null) { // Kiểm tra cực kỳ cẩn thận
                System.err.println("LỖI NGHIÊM TRỌNG: NoteController là null sau khi tạo!");
                JOptionPane.showMessageDialog(null, "Lỗi nghiêm trọng: NoteController không thể khởi tạo.", "Lỗi Khởi Động", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 4. Khởi tạo MainFrame, truyền controller vào
            System.out.println("[NoteApplication] EDT: Đang tạo MainFrame...");
            MainFrame mainFrame = new MainFrame(controller); // Đây là dòng 49 trong stack trace cũ của bro
            System.out.println("[NoteApplication] EDT: MainFrame đã tạo.");
            if (mainFrame == null) { // Kiểm tra cực kỳ cẩn thận
                System.err.println("LỖI NGHIÊM TRỌNG: MainFrame là null sau khi tạo!");
                JOptionPane.showMessageDialog(null, "Lỗi nghiêm trọng: MainFrame không thể khởi tạo.", "Lỗi Khởi Động", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 5. Cung cấp tham chiếu MainFrame cho NoteController
            controller.setMainFrameInstance(mainFrame);
            System.out.println("[NoteApplication] EDT: Đã đặt MainFrame instance cho NoteController.");

            // 6. Khởi tạo dữ liệu mẫu (nếu cần)
            try {
                // Quan trọng: Gọi getAllNotes() trên noteManager để kiểm tra,
                // vì controller.getNotes() có thể vẫn đang gặp vấn đề nếu controller chưa hoàn chỉnh.
                if (noteManager.getAllNotes().isEmpty()) {
                    System.out.println("[NoteApplication] EDT: Đang khởi tạo dữ liệu mẫu...");
                    initializeSampleData(noteManager); // Chỉ cần NoteManager
                } else {
                    System.out.println("[NoteApplication] EDT: Dữ liệu đã tồn tại. Bỏ qua khởi tạo dữ liệu mẫu.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame,
                        "Lỗi trong quá trình khởi tạo dữ liệu mẫu: " + e.getMessage(),
                        "Lỗi Khởi Tạo Dữ Liệu", JOptionPane.WARNING_MESSAGE);
            }

            // 7. Hiển thị Frame và làm mới UI
            System.out.println("[NoteApplication] EDT: Đặt MainFrame thành visible.");
            mainFrame.setVisible(true);

            // Đảm bảo MainMenuScreen được làm mới sau khi frame hiển thị và dữ liệu mẫu (nếu có) đã được thêm.
            SwingUtilities.invokeLater(() -> {
                System.out.println("[NoteApplication] EDT (inner): Đang làm mới MainMenuScreen.");
                mainFrame.showMainMenuScreen(); // Gọi lại để refresh nội dung
            });
            System.out.println("[NoteApplication] EDT: Hoàn tất khởi tạo ứng dụng trên EDT.");
        });
    }

    private static void initializeSampleData(NoteManager noteManager) {
        System.out.println("[initializeSampleData] Bắt đầu khởi tạo dữ liệu mẫu...");
        try {
            Folder workFolder = noteManager.getFolderByName("Công việc").orElseGet(() -> {
                Folder wf = new Folder("Công việc");
                noteManager.addFolder(wf);
                System.out.println("[initializeSampleData] Đã tạo/lấy thư mục Công việc ID: " + wf.getId());
                return wf;
            });

            Folder personalFolder = noteManager.getFolderByName("Cá nhân").orElseGet(() -> {
                Folder pf = new Folder("Cá nhân");
                noteManager.addFolder(pf);
                System.out.println("[initializeSampleData] Đã tạo/lấy thư mục Cá nhân ID: " + pf.getId());
                return pf;
            });

            Folder importantFolder = noteManager.getFolderByName("Quan trọng").orElseGet(() -> {
                Folder inf = new Folder("Quan trọng");
                noteManager.addFolder(inf);
                System.out.println("[initializeSampleData] Đã tạo/lấy thư mục Quan trọng ID: " + inf.getId());
                return inf;
            });

            Folder rootFolder = noteManager.getRootFolder();
            if (rootFolder == null) {
                System.err.println("[initializeSampleData] LỖI: Root folder là null!");
                return; // Không thể tiếp tục nếu không có Root
            }
            System.out.println("[initializeSampleData] Root folder ID: " + rootFolder.getId());


            Tag tagProject = noteManager.getOrCreateTag("project");
            Tag tagMeeting = noteManager.getOrCreateTag("meeting");
            Tag tagIdea = noteManager.getOrCreateTag("idea");
            System.out.println("[initializeSampleData] Các Tags đã được tạo/lấy.");

            Note note1 = new Note("Báo cáo tuần", "Hoàn thành báo cáo công việc tuần này.", false);
            note1.setFolder(workFolder);
            note1.addTag(tagProject);
            note1.addTag(tagMeeting);
            Alarm alarm1 = new Alarm(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0), false, null);
            note1.setAlarm(alarm1);
            noteManager.addNote(note1);
            System.out.println("[initializeSampleData] Đã thêm Note 1: " + note1.getTitle());

            Note note2 = new Note("Mua sắm cuối tuần", "Đi siêu thị mua đồ dùng cá nhân.", true);
            note2.setFolder(personalFolder);
            note2.addTag(tagIdea);
            note2.setMission(true);
            note2.setMissionContent("Nhớ mua sữa và bánh mì.");
            noteManager.addNote(note2);
            System.out.println("[initializeSampleData] Đã thêm Note 2: " + note2.getTitle());

            Note note3 = new Note("Học Java Swing", "Ôn tập các khái niệm về LayoutManager và Event Handling.", false);
            note3.setFolder(importantFolder);
            note3.addTag(tagProject);
            note3.addTag(tagIdea);
            Alarm alarm3 = new Alarm(LocalDateTime.now().plusHours(2), true, "DAILY");
            note3.setAlarm(alarm3);
            noteManager.addNote(note3);
            System.out.println("[initializeSampleData] Đã thêm Note 3: " + note3.getTitle());

            Note note4 = new Note("Đọc sách", "Đọc 1 chương sách 'Clean Code'", false);
            note4.setFolder(rootFolder);
            note4.setMission(true);
            note4.setMissionContent("Ghi lại 3 ý tưởng chính.");
            Alarm alarm4 = new Alarm(LocalDateTime.now().plusMinutes(30), false, null);
            note4.setAlarm(alarm4);
            noteManager.addNote(note4);
            System.out.println("[initializeSampleData] Đã thêm Note 4: " + note4.getTitle());

            System.out.println("[initializeSampleData] Hoàn tất khởi tạo dữ liệu mẫu.");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Lỗi khi khởi tạo dữ liệu mẫu: " + e.getMessage(),
                    "Lỗi Dữ Liệu Mẫu", JOptionPane.ERROR_MESSAGE);
        }
    }
}
