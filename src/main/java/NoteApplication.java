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
        controller.addNewFolder("Work");
        controller.addNewFolder("Personal");
        controller.addNewFolder("Important");

        // Add 25 notes, one for each hour from 0 to 23
        for (int hour = 0; hour < 24; hour++) {
            String title = "Task " + hour + " AM/PM";
            String content = "This is a reminder for task " + hour;

            // Create a new note
            Note note = new Note(title, content, false);
            note.setMission(true);
            note.setMissionContent("Complete task for hour " + hour);

            // Set alarm for the specific hour
            LocalDateTime alarmTime = LocalDateTime.now().withHour(hour).withMinute(0).withSecond(0).withNano(0);
            controller.setAlarm(note, new Alarm(alarmTime, false, "ONCE"));

            // Add the note to the controller
            controller.addNote(note);
            controller.moveNoteToFolder(note, controller.getFolderByName("Work").orElse(null));
        }

        System.out.println("Sample data initialized: " + controller.getNotes().size() + " notes, " + controller.getFolders().size() + " folders.");
    }

}