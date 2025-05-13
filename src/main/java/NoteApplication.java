import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.io.File;
import java.time.LocalDateTime;

public class NoteApplication {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            // Xóa file notes.json để đảm bảo dữ liệu mới được sử dụng
            File notesFile = new File("notes.json");
            if (notesFile.exists()) {
                System.out.println("Deleting existing notes.json file to initialize fresh data.");
                notesFile.delete();
            }

            NoteController controller = new NoteController();
            initializeControllerWithSampleData(controller);
            MainFrame frame = new MainFrame(controller);
            frame.setVisible(true);
        });
    }

    private static void initializeControllerWithSampleData(NoteController controller) {
        // Không gọi initializeSampleData() để tránh trùng lặp
        // Thay vào đó, thêm dữ liệu mẫu trực tiếp
        System.out.println("Initializing sample data...");

        controller.addNewFolder("Work");
        controller.addNewFolder("Personal");
        controller.addNewFolder("Important");

        // Note 1: Shopping List (không có mission)
        Note note1 = new Note("Shopping List", "Milk\nEggs\nBread", false);
        controller.addTag(note1, new Tag("groceries"));
        controller.addNote(note1);
        controller.moveNoteToFolder(note1, controller.getFolderByName("Personal").orElse(null));

        // Note 2: Project Ideas (không có mission)
        Note note2 = new Note("Project Ideas", "1. AI Chatbot\n2. Task Manager", true);
        controller.addTag(note2, new Tag("work"));
        controller.addNote(note2);
        controller.moveNoteToFolder(note2, controller.getFolderByName("Work").orElse(null));

        // Note 3: Meeting Notes (có mission, có alarm)
        Note note3 = new Note("Meeting Notes", "Discuss project timeline", false);
        note3.setMissionContent("Prepare presentation");
        note3.setMission(true);
        controller.addNote(note3);
        controller.moveNoteToFolder(note3, controller.getFolderByName("Important").orElse(null));
        controller.setAlarm(note3, new Alarm(LocalDateTime.now().plusMinutes(1), true, "DAILY"));

        // Note 4: Study Plan (có mission, không có alarm)
        Note note4 = new Note("Study Plan", "Review Java concepts", false);
        note4.setMissionContent("Complete Chapter 5 by tonight");
        note4.setMission(true);
        controller.addNote(note4);
        controller.moveNoteToFolder(note4, controller.getFolderByName("Personal").orElse(null));

        // Note 5: Team Sync (có mission, có alarm)
        Note note5 = new Note("Team Sync", "Sync with team on project progress", false);
        note5.setMissionContent("Schedule meeting and send agenda");
        note5.setMission(true);
        controller.addNote(note5);
        controller.moveNoteToFolder(note5, controller.getFolderByName("Work").orElse(null));
        controller.setAlarm(note5, new Alarm(LocalDateTime.now().plusHours(1), false, "ONCE"));

        System.out.println("Sample data initialized: " + controller.getNotes().size() + " notes, " + controller.getFolders().size() + " folders.");
    }
}