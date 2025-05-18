import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.io.File;
import java.io.InputStream; // Để đọc resource an toàn hơn
import java.net.URL;       // Để đọc resource
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlarmController {
    // private final NoteManager noteManager; // Đã chuyển sang dùng NoteController
    private final NoteController noteController; // Tham chiếu đến NoteController đã có CSDL
    private final MainFrame mainFrame;
    private final ScheduledExecutorService scheduler;
    private Clip clip;

    public AlarmController(NoteController noteController, MainFrame mainFrame) {
        this.noteController = noteController;
        this.mainFrame = mainFrame;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startAlarmChecker();
    }

    private void startAlarmChecker() {
        // Kiểm tra mỗi giây là khá thường xuyên, có thể tăng lên vài giây nếu muốn giảm tải nhẹ
        scheduler.scheduleAtFixedRate(this::checkAlarms, 0, 1, TimeUnit.SECONDS);
    }

    private void checkAlarms() {
        LocalDateTime now = LocalDateTime.now();
        // Lấy danh sách notes từ NoteController (đã kết nối CSDL)
        List<Note> notes;
        try {
            // Quan trọng: NoteController.getNotes() nên trả về danh sách notes
            // mà mỗi Note object đã được populate sẵn Alarm object (nếu có).
            // NoteService.getAllNotesForDisplay() đã làm điều này.
            notes = noteController.getNotes();
        } catch (Exception e) {
            // Nếu có lỗi khi lấy notes (ví dụ: lỗi CSDL), không thể check alarms
            System.err.println("AlarmController: Error fetching notes for alarm check: " + e.getMessage());
            e.printStackTrace(); // Nên có cơ chế log tốt hơn
            return;
        }

        if (notes == null) return; // Đề phòng

        for (Note note : notes) {
            Alarm alarm = note.getAlarm(); // Lấy Alarm object đã được populate
            // Đảm bảo note.isMissionCompleted() được load đúng từ CSDL.
            // Alarm.shouldTrigger(now) cần logic đúng.
            if (alarm != null && !note.isMissionCompleted() && alarm.shouldTrigger(now)) {
                final Note noteToProcess = note;
                final Alarm alarmToProcess = alarm;

                SwingUtilities.invokeLater(() -> {
                    triggerAlarm(noteToProcess); // Hiển thị dialog thông báo
                    if (!alarmToProcess.isRecurring()) {
                        // Gọi controller để xóa alarm khỏi DB và cập nhật note object
                        // NoteController.setAlarm(note, null) sẽ gọi service,
                        // service sẽ xóa Alarm object trong DB (nếu cần) và set note.alarmId = null
                        System.out.println("AlarmController: Non-recurring alarm triggered for note '" + noteToProcess.getTitle() + "'. Clearing alarm.");
                        noteController.setAlarm(noteToProcess, null);
                    }
                });

                // Cập nhật thời gian cho alarm lặp lại (nếu có)
                // Việc này cũng nên thông qua NoteController để lưu vào DB
                updateAlarmTimeAndSave(noteToProcess, alarmToProcess);
            }
        }
    }

    private void triggerAlarm(Note note) {
        // Sử dụng lớp AlarmNotificationDialog nội bộ
        AlarmNotificationDialog dialog = new AlarmNotificationDialog(mainFrame, note, this); // Truyền AlarmController để dừng clip
        dialog.setVisible(true);
        playSound();
    }

    private void playSound() {
        // Dừng clip cũ nếu đang chạy trước khi phát âm thanh mới
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            if (clip.isOpen()) {
                clip.close();
            }
        }
        try {
            URL soundResourceUrl = getClass().getResource("/sound/Doctor.wav"); // Đổi tên file nếu cần
            if (soundResourceUrl == null) {
                // Thử đường dẫn thay thế cho môi trường dev (không khuyến khích cho production)
                File soundFileDev = new File("src/main/resources/sound/Doctor.wav");
                if (soundFileDev.exists()) {
                    soundResourceUrl = soundFileDev.toURI().toURL();
                } else {
                    System.err.println("Alarm sound file not found at /sound/alarm.wav or dev path.");
                    // JOptionPane.showMessageDialog(mainFrame, "Alarm sound file not found!", "Sound Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundResourceUrl);
            clip = AudioSystem.getClip(); // Lấy clip mới
            clip.open(audioInput);
            clip.start();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    // Đảm bảo clip được đóng khi nó dừng (kể cả khi không phải do người dùng nhấn OK)
                    if (event.getSource() instanceof Clip) {
                        ((Clip) event.getSource()).close();
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Could not play sound: " + e.getMessage());
            e.printStackTrace(); // Log lỗi chi tiết hơn
            // JOptionPane.showMessageDialog(mainFrame, "Could not play sound: " + e.getMessage(), "Sound Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateAlarmTimeAndSave(Note note, Alarm alarm) {
        if (alarm.isRecurring()) {
            String pattern = alarm.getRecurrencePattern() != null ? alarm.getRecurrencePattern().toUpperCase() : "";
            LocalDateTime currentAlarmTime = alarm.getAlarmTime();
            LocalDateTime newTime = currentAlarmTime; // Khởi tạo
            boolean needsUpdate = false;

            switch (pattern) {
                case "DAILY":
                    newTime = currentAlarmTime.plusDays(1);
                    needsUpdate = true;
                    break;
                case "WEEKLY":
                    newTime = currentAlarmTime.plusWeeks(1);
                    needsUpdate = true;
                    break;
                case "MONTHLY":
                    newTime = currentAlarmTime.plusMonths(1);
                    needsUpdate = true;
                    break;
                case "YEARLY":
                    newTime = currentAlarmTime.plusYears(1);
                    needsUpdate = true;
                    break;
                default:
                    // Nếu pattern không hợp lệ hoặc là "ONCE" (đã được xử lý ở checkAlarms)
                    // thì không làm gì ở đây.
                    return;
            }

            if (needsUpdate) {
                // Tạo một đối tượng Alarm MỚI với thời gian mới và ID của alarm cũ để update.
                // Hoặc, nếu Alarm object được truyền vào là tham chiếu trực tiếp từ Note,
                // và NoteService.saveOrUpdateAlarm xử lý đúng, thì chỉ cần setAlarmTime.
                // Để an toàn, tạo object mới với ID cũ (nếu có).
                Alarm updatedAlarmInstance = new Alarm(alarm.getId(), newTime, true, alarm.getRecurrencePattern());
                // Nếu alarm.getId() là 0 (ví dụ, alarm mới được đặt và chưa kịp có ID từ DB),
                // thì logic này có thể cần xem lại.
                // Tuy nhiên, khi alarm lặp lại kích hoạt, nó phải đã có ID.

                System.out.println("AlarmController: Recurring alarm for note '" + note.getTitle() + "' updated to: " + newTime);
                noteController.setAlarm(note, updatedAlarmInstance); // Controller sẽ gọi service để lưu
            }
        }
        // Không cần else, vì non-recurring đã được xử lý trong checkAlarms
    }

    public void stopSoundAndScheduler() { // Đổi tên để rõ ràng hơn
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            if (clip.isOpen()) {
                clip.close();
            }
            clip = null; // Giải phóng tài nguyên
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) { // Giảm thời gian chờ
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Lớp Dialog nội bộ để thông báo Alarm
    // Đổi tên thành AlarmNotificationDialog để tránh nhầm lẫn với AlarmDialog.java (public class)
    private static class AlarmNotificationDialog extends JDialog {
        // Thêm tham chiếu AlarmController để có thể dừng clip
        private transient AlarmController alarmControllerInstance;

        public AlarmNotificationDialog(Frame owner, Note note, AlarmController controllerInstance) {
            super(owner, "⏰ Alarm Notification ⏰", true);
            this.alarmControllerInstance = controllerInstance;

            setLayout(new BorderLayout(10, 10));
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Quan trọng
            setAlwaysOnTop(true); // Đảm bảo dialog nổi lên trên

            JPanel mainContentPanel = new JPanel(new BorderLayout(5, 8));
            mainContentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JLabel titleLabel = new JLabel("<html><b>" + note.getTitle() + "</b></html>", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            mainContentPanel.add(titleLabel, BorderLayout.NORTH);

            String missionText;
            if (note.getMissionContent() != null && !note.getMissionContent().isEmpty()) {
                missionText = "Nhiệm vụ: " + note.getMissionContent() +
                        (note.isMissionCompleted() ? " (Đã hoàn thành)" : " (Chưa xong)");
            } else {
                missionText = "Không có nội dung nhiệm vụ cụ thể.";
            }

            JTextArea contentArea = new JTextArea(missionText);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            contentArea.setEditable(false);
            contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            contentArea.setOpaque(false); // Để nền của panel chứa nó hiển thị
            // contentArea.setBackground(UIManager.getColor("Label.background"));
            JScrollPane scrollPane = new JScrollPane(contentArea);
            scrollPane.setBorder(BorderFactory.createEtchedBorder()); // Thêm border cho đẹp
            mainContentPanel.add(scrollPane, BorderLayout.CENTER);

            if (note.getAlarm() != null && note.getAlarm().getAlarmTime() != null) {
                JLabel alarmTimeLabel = new JLabel("Thời gian báo thức: " +
                        note.getAlarm().getAlarmTime().format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy")),
                        SwingConstants.CENTER);
                alarmTimeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                mainContentPanel.add(alarmTimeLabel, BorderLayout.SOUTH);
            }

            add(mainContentPanel, BorderLayout.CENTER);

            JButton okButton = new JButton("OK");
            okButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            okButton.addActionListener(e -> {
                if (alarmControllerInstance != null && alarmControllerInstance.clip != null) {
                    if (alarmControllerInstance.clip.isRunning()) {
                        alarmControllerInstance.clip.stop();
                    }
                    if (alarmControllerInstance.clip.isOpen()) {
                        alarmControllerInstance.clip.close();
                    }
                }
                dispose();
            });
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
            buttonPanel.add(okButton);
            add(buttonPanel, BorderLayout.SOUTH);

            pack(); // Tính toán kích thước tối ưu
            // Đảm bảo kích thước tối thiểu và không quá lớn
            int minWidth = 350;
            int minHeight = 200;
            int maxWidth = 500;
            int maxHeight = 350;
            setSize(Math.min(maxWidth, Math.max(minWidth, getWidth() + 20)), // Thêm padding
                    Math.min(maxHeight, Math.max(minHeight, getHeight() + 20)));
            setLocationRelativeTo(owner); // Căn giữa so với owner
        }
        // findAlarmController không còn cần thiết nếu truyền trực tiếp
    }
}
