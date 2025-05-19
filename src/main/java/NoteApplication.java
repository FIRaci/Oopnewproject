import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.sql.SQLException; // Thêm
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
// import java.io.File; // Không cần xóa notes.json nữa

public class NoteApplication {

    // --- Cấu hình SSH Tunnel (Thay đổi các giá trị này nếu bạn dùng SSH Tunnel) ---
    private static final boolean USE_SSH_TUNNEL = false; // Đặt thành true nếu bạn cần SSH
    private static final String SSH_USER = "your_ssh_user";
    private static final String SSH_PASSWORD = "your_ssh_password"; // Cân nhắc dùng key-based auth
    private static final String SSH_HOST = "your_ssh_host_ip_or_domain";
    private static final int SSH_PORT = 22; // Cổng SSH chuẩn
    private static final String REMOTE_DB_HOST = "localhost"; // Host của DB từ góc nhìn của SSH server (thường là localhost)
    private static final int LOCAL_FORWARD_PORT = 9998; // Cổng local bạn sẽ dùng để kết nối DB qua tunnel
    // DBConnectionManager sẽ dùng cổng này (localhost:9998)
    private static final int REMOTE_DB_PORT = 9999; // Cổng thực của PostgreSQL server trên remote machine

    public static void main(String[] args) {
        // 1. Thiết lập Look and Feel (nên làm đầu tiên)
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf Look and Feel: " + e.getMessage());
            // Có thể dùng Look and Feel mặc định của hệ thống nếu FlatLaf lỗi
        }

        // 2. (Tùy chọn) Kết nối SSH Tunnel nếu cần
        if (USE_SSH_TUNNEL) {
            try {
                System.out.println("Attempting to establish SSH tunnel...");
                SshTunnelManager.connect(SSH_USER, SSH_PASSWORD, SSH_HOST, SSH_PORT,
                        REMOTE_DB_HOST, LOCAL_FORWARD_PORT, REMOTE_DB_PORT);
                System.out.println("SSH tunnel connection seems successful.");

                // Thêm shutdown hook để đóng tunnel khi ứng dụng thoát
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Application shutting down, disconnecting SSH tunnel...");
                    SshTunnelManager.disconnect();
                }));
            } catch (Exception e) { // Bắt Exception chung cho JSchException và các lỗi khác
                System.err.println("Failed to establish SSH tunnel: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Could not establish SSH tunnel. Database connectivity might be affected.\n" + e.getMessage(),
                        "SSH Tunnel Error", JOptionPane.ERROR_MESSAGE);
                // Quyết định có nên thoát ứng dụng ở đây không nếu SSH là bắt buộc
                // System.exit(1);
            }
        }

        // Nhắc nhở về việc tạo schema CSDL (quan trọng!)
        System.out.println("Reminder: Ensure your PostgreSQL database schema (tables: Folders, Tags, Notes, Alarms, Note_Tag) is created and an initial 'Root' folder exists.");
        System.out.println("Reminder: Update database credentials in DBConnectionManager.java if you haven't.");
        if (USE_SSH_TUNNEL) {
            System.out.println("Reminder: JDBC URL in DBConnectionManager.java should point to localhost:" + LOCAL_FORWARD_PORT + " when using SSH tunnel.");
        }


        // 3. Khởi chạy ứng dụng Swing trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            NoteController controller = new NoteController(new JFrame()); // JFrame không còn được truyền vào constructor

            // 4. Khởi tạo dữ liệu mẫu (giờ sẽ ghi vào CSDL)
            // Bạn có thể kiểm tra xem có cần tạo dữ liệu mẫu không, ví dụ: dựa trên số lượng note hiện có
            try {
                if (controller.getNotes().isEmpty()) { // Chỉ tạo dữ liệu mẫu nếu DB rỗng
                    System.out.println("Database appears empty, initializing with sample data...");
                    initializeControllerWithSampleData(controller);
                } else {
                    System.out.println("Database already contains data. Skipping sample data initialization.");
                }
            } catch (Exception e) { // Bắt lỗi chung khi kiểm tra hoặc tạo dữ liệu mẫu
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error during sample data initialization: " + e.getMessage(),
                        "Data Initialization Error", JOptionPane.WARNING_MESSAGE);
            }

            // 5. Tạo và hiển thị MainFrame
            // MainFrame nhận controller. NoteController không cần tham chiếu MainFrame trong constructor nữa.
            MainFrame frame = new MainFrame(controller);
            // Nếu NoteController cần tham chiếu đến frame (ví dụ để changeTheme),
            // MainFrame có thể gọi một setter trên controller: controller.setMainFrame(frame);
            // Hoặc MainFrame tự gọi controller.changeTheme(frame);
            // Hiện tại, NoteController.changeTheme() đã nhận JFrame làm tham số.

            frame.setVisible(true);
        });
    }

    private static void initializeControllerWithSampleData(NoteController controller) {
        System.out.println("Initializing sample data into database via NoteController/NoteService...");
        try {
            // --- Tạo Thư mục ---
            controller.addNewFolder("Công việc"); // addNewFolder sẽ tự kiểm tra trùng tên
            controller.addNewFolder("Cá nhân");
            controller.addNewFolder("Quan trọng");

            // Lấy lại các folder đã tạo (để có ID đúng từ DB)
            Optional<Folder> workFolderOpt = controller.getFolderByName("Công việc");
            Optional<Folder> personalFolderOpt = controller.getFolderByName("Cá nhân");
            Optional<Folder> importantFolderOpt = controller.getFolderByName("Quan trọng");
            // Mặc định dùng Root nếu folder không tìm thấy (dù không nên xảy ra nếu addNewFolder thành công)
            Folder rootFolder = controller.getFolderByName("Root").orElse(controller.getCurrentFolder());


            Folder workFolder = workFolderOpt.orElse(rootFolder);
            Folder personalFolder = personalFolderOpt.orElse(rootFolder);
            Folder importantFolder = importantFolderOpt.orElse(rootFolder);


            // --- Tạo Tags (nếu muốn có sẵn) ---
            // Controller hiện không có phương thức addTag độc lập, tag được thêm vào note.
            // Nếu muốn tạo tag trước, cần thêm phương thức vào NoteService/Controller, ví dụ:
            // controller.createTagIfNotExists("project");
            // controller.createTagIfNotExists("meeting");
            // controller.createTagIfNotExists("idea");

            Tag tagProject = new Tag("project");
            Tag tagMeeting = new Tag("meeting");
            Tag tagIdea = new Tag("idea");
            // Để các tag này được lưu, chúng cần được thêm vào một note,
            // hoặc bạn cần 1 phương thức controller.ensureTagsExist(List<Tag> tags) gọi service.

            // --- Tạo Notes ---
            // Note 1 (Công việc)
            Note note1 = new Note("Báo cáo tuần", "Hoàn thành báo cáo công việc tuần này.", false);
            note1.setFolderId(workFolder.getId()); // Gán folderId
            note1.setFolder(workFolder);          // Gán transient folder
            note1.addTag(tagProject);             // Thêm tag vào object note
            note1.addTag(tagMeeting);
            // Tạo và gán Alarm
            Alarm alarm1 = new Alarm(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0), false, null);
            note1.setAlarm(alarm1); // Gán object Alarm, controller.addNote sẽ xử lý lưu alarm và gán alarmId
            controller.addNote(note1); // addNote sẽ lưu note, alarm, và các liên kết tag

            // Note 2 (Cá nhân)
            Note note2 = new Note("Mua sắm cuối tuần", "Đi siêu thị mua đồ dùng cá nhân.", true);
            note2.setFolderId(personalFolder.getId());
            note2.setFolder(personalFolder);
            note2.addTag(tagIdea);
            // Note này có Mission
            note2.setMission(true);
            note2.setMissionContent("Nhớ mua sữa và bánh mì.");
            controller.addNote(note2);

            // Note 3 (Quan trọng)
            Note note3 = new Note("Học Java Swing", "Ôn tập các khái niệm về LayoutManager và Event Handling.", false);
            note3.setFolderId(importantFolder.getId());
            note3.setFolder(importantFolder);
            note3.addTag(tagProject);
            note3.addTag(tagIdea);
            Alarm alarm3 = new Alarm(LocalDateTime.now().plusHours(2), true, "DAILY"); // Lặp lại hàng ngày
            note3.setAlarm(alarm3);
            controller.addNote(note3);

            // Note 4 (Trong Root, có mission và alarm)
            Note note4 = new Note("Đọc sách", "Đọc 1 chương sách 'Clean Code'", false);
            // FolderId sẽ được tự động gán vào Root nếu không set (logic trong controller.addNote)
            // Hoặc gán tường minh:
            if (rootFolder != null && rootFolder.getId() > 0) {
                note4.setFolderId(rootFolder.getId());
                note4.setFolder(rootFolder);
            }
            note4.setMission(true);
            note4.setMissionContent("Ghi lại 3 ý tưởng chính.");
            Alarm alarm4 = new Alarm(LocalDateTime.now().plusMinutes(30), false, null);
            note4.setAlarm(alarm4);
            controller.addNote(note4);


            System.out.println("Sample data initialization complete.");

        } catch (Exception e) { // Bắt Exception chung vì nhiều loại lỗi có thể xảy ra
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, // Sử dụng null nếu mainFrameInstance chưa có
                    "Error initializing sample data into database: " + e.getMessage(),
                    "Sample Data Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}