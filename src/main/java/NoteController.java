import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.sql.SQLException;

public class NoteController {
    private final NoteService noteService;
    private Folder currentFolder;
    private JFrame mainFrameInstance; // Tham chiếu đến MainFrame để cập nhật theme
    private boolean isDarkTheme = false;

    // SỬA 1: Sửa constructor để nhận MainFrame
    public NoteController(JFrame mainFrameInstance) {
        this.mainFrameInstance = mainFrameInstance;

        // Khởi tạo DAO và Service
        NoteDAO noteDAO = new NoteDAOImpl(); // Giả sử các class này tồn tại
        FolderDAO folderDAO = new FolderDAOImpl(); // Giả sử các class này tồn tại
        TagDAO tagDAO = new TagDAOImpl(); // Giả sử các class này tồn tại
        AlarmDAO alarmDAO = new AlarmDAOImpl(); // Giả sử các class này tồn tại
        this.noteService = new NoteService(noteDAO, folderDAO, tagDAO, alarmDAO);

        // Khởi tạo currentFolder (Root)
        try {
            this.currentFolder = this.noteService.getFolderByName("Root");
            if (this.currentFolder == null) {
                System.out.println("Root folder not found, creating one...");
                Folder root = new Folder("Root");
                this.noteService.createNewFolder(root);
                this.currentFolder = this.noteService.getFolderByName("Root"); // Lấy lại để đảm bảo có ID
                if (this.currentFolder == null) { // Kiểm tra lại sau khi tạo
                    throw new SQLException("Failed to create or retrieve Root folder after creation attempt.");
                }
                System.out.println("Root folder created/retrieved with ID: " + this.currentFolder.getId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            this.currentFolder = new Folder("Root (DB Error)");
            this.currentFolder.setId(-1L); // ID không hợp lệ để biểu thị lỗi
            JOptionPane.showMessageDialog(this.mainFrameInstance,
                    "Critical Error: Could not initialize Root folder. Please check database connection.\n" + e.getMessage(),
                    "Database Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<Note> getSortedNotes() {
        try {
            List<Note> notesToDisplay;
            if (currentFolder != null && currentFolder.getId() > 0) {
                // CHÚ THÍCH QUAN TRỌNG: noteService.getAllNotesForDisplay() hoặc phương thức tương đương
                // PHẢI đảm bảo rằng mỗi đối tượng Note được trả về đã được điền (populated)
                // với đối tượng Alarm tương ứng của nó (note.getAlarm()) nếu có.
                // Nếu không, AlarmController sẽ không bao giờ thấy báo thức nào để kích hoạt.
                notesToDisplay = noteService.getAllNotesForDisplay().stream()
                        .filter(note -> note.getFolderId() == currentFolder.getId())
                        .collect(Collectors.toList());

            } else if (currentFolder != null && "Root (DB Error)".equals(currentFolder.getName())) {
                return new ArrayList<>();
            }
            else {
                System.out.println("Current folder is not properly set or is Root, displaying all notes.");
                // Tương tự, getAllNotesForDisplay() cần populate Alarm objects
                notesToDisplay = noteService.getAllNotesForDisplay();
            }

            return notesToDisplay.stream()
                    .sorted(Comparator.comparing(Note::isFavorite, Comparator.reverseOrder())
                            .thenComparing(Note::getUpdatedAt, Comparator.reverseOrder()))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error fetching notes: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    public List<Note> searchNotes(String query) {
        try {
            List<Note> notesToSearchIn = getSortedNotes();
            if (query == null || query.trim().isEmpty()) {
                return notesToSearchIn;
            }
            String lowerQuery = query.toLowerCase().trim();
            return notesToSearchIn.stream()
                    .filter(note -> (note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) ||
                            (note.getContent() != null && note.getContent().toLowerCase().contains(lowerQuery)) ||
                            (note.getTags() != null && note.getTags().stream()
                                    .anyMatch(tag -> tag.getName().toLowerCase().contains(lowerQuery)))
                    )
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error searching notes: " + e.getMessage(), "Search Error", JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    public void selectFolder(Folder folder) {
        this.currentFolder = folder;
    }

    public Folder getCurrentFolder() {
        return currentFolder;
    }

    public List<Folder> getFolders() {
        try {
            return noteService.getAllFolders();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error fetching folders: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    public void addNewFolder(String name) {
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Folder name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Folder existingFolder = noteService.getFolderByName(name.trim());
            if (existingFolder != null) {
                JOptionPane.showMessageDialog(mainFrameInstance, "Folder with name '" + name.trim() + "' already exists.", "Creation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Folder folder = new Folder(name.trim());
            noteService.createNewFolder(folder); // Service sẽ cập nhật ID cho 'folder'
            JOptionPane.showMessageDialog(mainFrameInstance, "Folder '" + folder.getName() + "' created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error adding folder: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteFolder(Folder folder) {
        if (folder == null || folder.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Cannot delete an invalid or unsaved folder.", "Operation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("Root".equalsIgnoreCase(folder.getName())) {
            JOptionPane.showMessageDialog(mainFrameInstance, "The Root folder cannot be deleted.", "Operation Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int choice = JOptionPane.showConfirmDialog(mainFrameInstance,
                    "Are you sure you want to delete the folder \"" + folder.getName() + "\"?\n" +
                            "This will also affect notes within this folder.",
                    "Confirm Delete Folder",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.NO_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }

            int notesActionChoice = JOptionPane.showConfirmDialog(mainFrameInstance,
                    "Move notes from folder '" + folder.getName() + "' to Root folder?\n(Choosing 'No' will delete these notes)",
                    "Notes in Folder",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (notesActionChoice == JOptionPane.CANCEL_OPTION) return;
            boolean moveNotes = (notesActionChoice == JOptionPane.YES_OPTION);

            noteService.deleteExistingFolder(folder.getId(), moveNotes);
            JOptionPane.showMessageDialog(mainFrameInstance, "Folder '" + folder.getName() + "' deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

            if (currentFolder != null && currentFolder.getId() == folder.getId()) {
                currentFolder = noteService.getFolderByName("Root");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error deleting folder: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void renameFolder(Folder folder, String newName) {
        if (folder == null || folder.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Cannot rename an invalid or unsaved folder.", "Operation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newName == null || newName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "New folder name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("Root".equalsIgnoreCase(folder.getName()) && !"Root".equalsIgnoreCase(newName.trim())) {
            JOptionPane.showMessageDialog(mainFrameInstance, "The Root folder cannot be renamed to something else.", "Operation Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!"Root".equalsIgnoreCase(folder.getName()) && "Root".equalsIgnoreCase(newName.trim())) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Cannot rename a folder to 'Root'. 'Root' is a reserved name.", "Operation Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String oldName = folder.getName();
        folder.setName(newName.trim());
        try {
            Folder existingFolderWithNewName = noteService.getFolderByName(newName.trim());
            if (existingFolderWithNewName != null && existingFolderWithNewName.getId() != folder.getId()) {
                folder.setName(oldName);
                JOptionPane.showMessageDialog(mainFrameInstance, "Another folder with name '" + newName.trim() + "' already exists.", "Rename Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            noteService.updateExistingFolder(folder.getId(), folder);
            JOptionPane.showMessageDialog(mainFrameInstance, "Folder renamed to '" + folder.getName() + "' successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            folder.setName(oldName);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error renaming folder: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setFolderFavorite(Folder folder, boolean isFavorite) {
        if (folder == null || folder.getId() <= 0) return;
        boolean oldFavoriteStatus = folder.isFavorite();
        folder.setFavorite(isFavorite);
        try {
            noteService.updateExistingFolder(folder.getId(), folder);
        } catch (SQLException e) {
            folder.setFavorite(oldFavoriteStatus);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error setting folder favorite status: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setFolderMission(Folder folder, boolean isMissionEnabledForAllNotes) {
        if (folder == null || folder.getId() <= 0) return;
        try {
            // NoteService nên có một phương thức để làm điều này hiệu quả hơn
            // thay vì controller phải lặp và gọi updateExistingNote nhiều lần.
            // Ví dụ: noteService.setMissionStatusForNotesInFolder(folder.getId(), isMissionEnabledForAllNotes);
            List<Note> notesInFolder = noteService.getAllNotesForDisplay().stream()
                    .filter(n -> n.getFolderId() == folder.getId())
                    .collect(Collectors.toList());
            for (Note note : notesInFolder) {
                note.setMission(isMissionEnabledForAllNotes);
                if (!isMissionEnabledForAllNotes) {
                    note.setMissionContent("");
                    note.setMissionCompleted(false);
                }
                noteService.updateExistingNote(note.getId(), note);
            }
            JOptionPane.showMessageDialog(mainFrameInstance, "Mission status for notes in folder '" + folder.getName() + "' updated.", "Folder Mission Update", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error setting mission status for notes in folder: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void addNote(Note note) {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Cannot add a null note.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (note.getFolderId() <= 0) {
                if (currentFolder != null && currentFolder.getId() > 0 && !"Root (DB Error)".equals(currentFolder.getName())) {
                    note.setFolderId(currentFolder.getId());
                    note.setFolder(currentFolder);
                } else {
                    Folder root = noteService.getFolderByName("Root");
                    if (root != null && root.getId() > 0) {
                        note.setFolderId(root.getId());
                        note.setFolder(root);
                    } else {
                        throw new SQLException("Default folder (Root) not found or invalid. Cannot add note.");
                    }
                }
            }
            if (note.getAlarm() != null) {
                // Đảm bảo Alarm được lưu và có ID trước khi Note được lưu với alarm_id
                noteService.saveOrUpdateAlarm(note.getAlarm()); // Điều này nên cập nhật ID vào note.getAlarm()
                note.setAlarmId(note.getAlarm().getId());
            } else {
                note.setAlarmId(null); // Đảm bảo alarmId là null nếu không có Alarm object
            }

            noteService.createNewNote(note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Note '" + note.getTitle() + "' added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error adding note: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteNote(Note note) {
        if (note == null || note.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Cannot delete an invalid or unsaved note.", "Operation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Long alarmIdToDelete = note.getAlarmId();
            noteService.deleteExistingNote(note.getId());
            if (alarmIdToDelete != null && alarmIdToDelete > 0) {
                noteService.deleteAlarm(alarmIdToDelete); // Xóa alarm liên quan khỏi bảng Alarms
            }
            JOptionPane.showMessageDialog(mainFrameInstance, "Note '" + note.getTitle() + "' deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error deleting note: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateNote(Note note, String title, String content) {
        if (note == null || note.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Cannot update an invalid or unsaved note.", "Operation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (title == null || title.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Note title cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        note.setTitle(title.trim());
        note.setContent(content);
        try {
            // Giả sử note object đã chứa thông tin alarmId đúng (nếu có)
            // và noteService.updateExistingNote sẽ lưu tất cả các trường cần thiết.
            noteService.updateExistingNote(note.getId(), note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Note '" + note.getTitle() + "' updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error updating note: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void renameNote(Note note, String newTitle) {
        if (note == null || note.getId() <= 0) return;
        if (newTitle == null || newTitle.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "New title cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String oldTitle = note.getTitle();
        note.setTitle(newTitle.trim());
        try {
            noteService.updateExistingNote(note.getId(), note);
        } catch (SQLException e) {
            note.setTitle(oldTitle);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error renaming note: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setNoteFavorite(Note note, boolean isFavorite) {
        if (note == null || note.getId() <= 0) return;
        boolean oldStatus = note.isFavorite();
        note.setFavorite(isFavorite);
        try {
            noteService.updateExistingNote(note.getId(), note);
        } catch (SQLException e) {
            note.setFavorite(oldStatus);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error setting note favorite status: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setNoteMission(Note note, boolean isMission) {
        if (note == null || note.getId() <= 0) return;
        boolean oldStatus = note.isMission();
        note.setMission(isMission);
        if (!isMission) {
            note.setMissionContent("");
            note.setMissionCompleted(false);
        }
        try {
            noteService.updateExistingNote(note.getId(), note);
        } catch (SQLException e) {
            note.setMission(oldStatus);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error setting note mission status: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void addTag(Note note, Tag tagFromUI) {
        if (note == null || note.getId() <= 0 || tagFromUI == null || tagFromUI.getName() == null || tagFromUI.getName().trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Invalid note or tag name.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            boolean tagAlreadyInNoteObject = note.getTags().stream()
                    .anyMatch(existingTag -> existingTag.getName().equalsIgnoreCase(tagFromUI.getName().trim()));
            if (tagAlreadyInNoteObject) {
                JOptionPane.showMessageDialog(mainFrameInstance, "Tag '" + tagFromUI.getName().trim() + "' already added to this note.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Tag tagToProcess = noteService.getTagByName(tagFromUI.getName().trim());
            if (tagToProcess == null) {
                tagToProcess = new Tag(tagFromUI.getName().trim());
                noteService.saveOrUpdateTag(tagToProcess); // Phương thức này cần tồn tại và đặt ID cho tagToProcess
            }

            note.addTag(tagToProcess);
            noteService.updateExistingNote(note.getId(), note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Tag '" + tagToProcess.getName() + "' added to note.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error adding tag to note: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void removeTag(Note note, Tag tag) {
        if (note == null || note.getId() <= 0 || tag == null || tag.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Invalid note or tag to remove.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean removed = note.getTags().removeIf(t -> t.getId() == tag.getId());
        if (removed) {
            try {
                noteService.updateExistingNote(note.getId(), note);
                JOptionPane.showMessageDialog(mainFrameInstance, "Tag '" + tag.getName() + "' removed from note.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                note.addTag(tag);
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrameInstance, "Error removing tag from note: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void moveNoteToFolder(Note note, Folder folder) {
        if (note == null || note.getId() <= 0 || folder == null || folder.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Invalid note or destination folder.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (note.getFolderId() == folder.getId()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Note is already in the folder '" + folder.getName() + "'.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        long oldFolderId = note.getFolderId();
        Folder oldTransientFolder = note.getFolder();

        note.setFolderId(folder.getId());
        note.setFolder(folder);
        try {
            noteService.updateExistingNote(note.getId(), note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Note '" + note.getTitle() + "' moved to folder '" + folder.getName() + "'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            note.setFolderId(oldFolderId);
            note.setFolder(oldTransientFolder);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error moving note: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<Note> getNotes() {
        try {
            // CHÚ THÍCH QUAN TRỌNG: noteService.getAllNotesForDisplay()
            // PHẢI đảm bảo rằng mỗi đối tượng Note được trả về đã được điền (populated)
            // với đối tượng Alarm tương ứng của nó (note.getAlarm()) nếu có.
            return noteService.getAllNotesForDisplay();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error fetching all notes: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    public Optional<Folder> getFolderByName(String name) {
        if (name == null || name.trim().isEmpty()) return Optional.empty();
        try {
            return Optional.ofNullable(noteService.getFolderByName(name.trim()));
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error fetching folder by name: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return Optional.empty();
        }
    }

    public void changeTheme() {
        try {
            if (isDarkTheme) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            }
            isDarkTheme = !isDarkTheme;
            if (mainFrameInstance != null) {
                SwingUtilities.updateComponentTreeUI(mainFrameInstance);
            } else {
                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Failed to change theme: " + e.getMessage(), "Theme Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getCurrentTheme() {
        return isDarkTheme ? "dark" : "light";
    }

    public List<Note> getMissions() {
        try {
            return noteService.getAllNotesForDisplay().stream()
                    .filter(note -> note.isMission() && (note.getMissionContent() != null && !note.getMissionContent().isEmpty()))
                    .sorted(Comparator.comparing(Note::isMissionCompleted)
                            .thenComparing(Note::getUpdatedAt, Comparator.reverseOrder()))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error fetching missions: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    public void updateMission(Note note, String missionContent) {
        if (note == null || note.getId() <= 0) return;
        String oldMissionContent = note.getMissionContent();
        boolean oldIsMission = note.isMission();
        boolean oldIsCompleted = note.isMissionCompleted();

        note.setMissionContent(missionContent == null ? "" : missionContent.trim());
        if (!note.isMission()) { // isMission được cập nhật trong setMissionContent
            note.setMissionCompleted(false);
        }
        try {
            noteService.updateExistingNote(note.getId(), note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Mission for note '" + note.getTitle() + "' updated.", "Mission Update", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            note.setMissionContent(oldMissionContent);
            note.setMission(oldIsMission);
            note.setMissionCompleted(oldIsCompleted);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error updating mission: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void completeMission(Note note, boolean completed) {
        if (note == null || note.getId() <= 0) return;
        boolean oldCompletedStatus = note.isMissionCompleted();
        Long alarmIdToManage = note.getAlarmId(); // Lưu lại alarmId hiện tại của note

        note.setMissionCompleted(completed);

        try {
            if (completed) {
                // Nếu hoàn thành mission, xóa alarmId khỏi note object
                note.setAlarmId(null);
                note.setAlarm(null); // Xóa cả transient alarm object
            }
            // Lưu trạng thái mới của note (bao gồm alarmId đã được cập nhật hoặc xóa)
            noteService.updateExistingNote(note.getId(), note);

            // Nếu mission hoàn thành VÀ note TRƯỚC ĐÓ có alarm, thì xóa alarm đó khỏi bảng Alarms
            if (completed && alarmIdToManage != null && alarmIdToManage > 0) {
                noteService.deleteAlarm(alarmIdToManage);
            }

            JOptionPane.showMessageDialog(mainFrameInstance,
                    "Mission '" + note.getTitle() + (completed ? "' completed." : "' marked as not completed."),
                    "Mission Status", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            note.setMissionCompleted(oldCompletedStatus); // Rollback trạng thái completed
            if (completed) { // Nếu đang cố gắng hoàn thành và bị lỗi, khôi phục alarmId
                note.setAlarmId(alarmIdToManage);
                // Cần lấy lại đối tượng Alarm từ DB nếu muốn khôi phục note.setAlarm(previousAlarmObject)
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error updating mission completion status: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // SỬA 2: Triển khai `setAlarm` một cách đầy đủ
    public void setAlarm(Note note, Alarm alarm) {
        if (note == null || note.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Invalid note to set alarm for.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Long oldAlarmId = note.getAlarmId();

            if (alarm == null) { // Xóa báo thức hiện tại
                note.setAlarmId(null);
                note.setAlarm(null);
                noteService.updateExistingNote(note.getId(), note); // Lưu note với alarmId là null

                if (oldAlarmId != null && oldAlarmId > 0) {
                    noteService.deleteAlarm(oldAlarmId); // Xóa alarm cũ khỏi bảng Alarms
                }
                System.out.println("Alarm cleared for note: " + note.getTitle());
                // JOptionPane.showMessageDialog(mainFrameInstance, "Alarm cleared for note '" + note.getTitle() + "'.", "Alarm Cleared", JOptionPane.INFORMATION_MESSAGE);
            } else { // Đặt hoặc cập nhật báo thức mới
                // Lưu hoặc cập nhật đối tượng Alarm trong DB.
                // noteService.saveOrUpdateAlarm() nên cập nhật ID vào đối tượng `alarm` được truyền vào.
                noteService.saveOrUpdateAlarm(alarm);

                note.setAlarmId(alarm.getId());
                note.setAlarm(alarm); // Giữ tham chiếu đến đối tượng Alarm trong Note
                noteService.updateExistingNote(note.getId(), note); // Lưu note với alarmId mới/đã cập nhật
                System.out.println("Alarm set/updated for note: " + note.getTitle() + " to " + alarm);
                // JOptionPane.showMessageDialog(mainFrameInstance, "Alarm set/updated for note '" + note.getTitle() + "'.", "Alarm Set", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Cân nhắc rollback: nếu đang cố đặt alarm mới và lỗi, có thể muốn khôi phục oldAlarmId cho note.
            // note.setAlarmId(oldAlarmId); // Ví dụ về rollback đơn giản
            // if (oldAlarmId != null) note.setAlarm(noteService.getAlarmById(oldAlarmId)); // Cần getAlarmById
            JOptionPane.showMessageDialog(mainFrameInstance, "Error setting alarm: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    public void saveOrUpdateAlarm(Alarm alarm) {
        if (alarm == null) { // Sửa: không nên kiểm tra alarm.getId() <=0 vì alarm mới có thể có id = 0
            JOptionPane.showMessageDialog(mainFrameInstance, "Invalid alarm object (null).", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            noteService.saveOrUpdateAlarm(alarm); // Phương thức này sẽ xử lý việc alarm mới (ID=0) hay cũ (ID>0)
            // JOptionPane.showMessageDialog(mainFrameInstance, "Alarm data (ID: "+alarm.getId()+") saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            System.out.println("Alarm data (ID: "+alarm.getId()+") saved via NoteController.saveOrUpdateAlarm.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error saving alarm data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateExistingNote(long id, Note note) throws SQLException {
        List<Note> notes = getNotes();
        for (Note existingNote : notes) {
            if (existingNote.getId() == id) {
                existingNote.setTitle(note.getTitle());
                existingNote.setContent(note.getContent());
                existingNote.setTags(note.getTags());
                noteService.updateExistingNote(existingNote.getId(), existingNote);
                return;
            }
        }
        throw new IllegalArgumentException("Note with ID " + id + " not found.");
    }
}