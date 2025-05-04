import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.time.LocalDateTime;

public class NoteApplication {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            NoteController controller = new NoteController();
            initializeControllerWithSampleData(controller);
            MainFrame frame = new MainFrame(controller);
            frame.setVisible(true);
        });
    }

    private static void initializeControllerWithSampleData(NoteController controller) {
        // Kiểm tra xem dữ liệu đã được tải từ file chưa
        if (controller.getNotes().isEmpty() && controller.getFolders().size() <= 1) { // Chỉ có Root folder
            // Thêm thư mục mẫu
            controller.addNewFolder("Work");
            controller.addNewFolder("Personal");
            controller.addNewFolder("Important");

            // Thêm ghi chú mẫu
            Note note1 = new Note("Shopping List", "Milk\nEggs\nBread", false);
            controller.addTag(note1, new Tag("groceries"));
            controller.addNote(note1);
            controller.moveNoteToFolder(note1, controller.getFolderByName("Personal").orElse(null));

            Note note2 = new Note("Project Ideas", "1. AI Chatbot\n2. Task Manager", true);
            controller.addTag(note2, new Tag("work"));
            controller.addNote(note2);
            controller.moveNoteToFolder(note2, controller.getFolderByName("Work").orElse(null));

            Note note3 = new Note("Meeting Notes", "Discuss project timeline", false);
            note3.setMission(true);
            controller.addNote(note3);
            controller.moveNoteToFolder(note3, controller.getFolderByName("Important").orElse(null));
            controller.setAlarm(note3, new Alarm(LocalDateTime.now().plusMinutes(1), true, "DAILY"));
        }
    }
}