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

    public NoteController() { // Nhận MainFrame để quản lý theme
        this.mainFrameInstance = mainFrameInstance;

        // Khởi tạo DAO và Service
        NoteDAO noteDAO = new NoteDAOImpl();
        FolderDAO folderDAO = new FolderDAOImpl();
        TagDAO tagDAO = new TagDAOImpl();
        AlarmDAO alarmDAO = new AlarmDAOImpl();
        this.noteService = new NoteService(noteDAO, folderDAO, tagDAO, alarmDAO);

        // Khởi tạo currentFolder (Root)
        try {
            this.currentFolder = this.noteService.getFolderByName("Root");
            if (this.currentFolder == null) {
                System.out.println("Root folder not found, creating one...");
                Folder root = new Folder("Root");
                this.noteService.createNewFolder(root); // Service sẽ cập nhật ID cho 'root'
                this.currentFolder = root;
                if (this.currentFolder.getId() == 0 && !"Root (DB Error)".equals(this.currentFolder.getName())) { // Đảm bảo có ID
                    this.currentFolder = this.noteService.getFolderByName("Root");
                }
                System.out.println("Root folder created/retrieved with ID: " + (this.currentFolder != null ? this.currentFolder.getId() : "null"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            this.currentFolder = new Folder("Root (DB Error)"); // Folder tạm thời nếu lỗi DB
            this.currentFolder.setId(-1L);
            JOptionPane.showMessageDialog(this.mainFrameInstance,
                    "Critical Error: Could not initialize Root folder. Please check database connection.\n" + e.getMessage(),
                    "Database Initialization Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        // Khởi tạo trạng thái theme ban đầu (có thể đọc từ một file config sau này)
        // Ví dụ: isDarkTheme = loadThemePreference();
        // applyCurrentTheme(); // Áp dụng theme khi khởi động
    }

    public List<Note> getSortedNotes() {
        try {
            List<Note> notesToDisplay;
            if (currentFolder != null && currentFolder.getId() > 0) {
                // TODO: Tối ưu: Tạo phương thức noteService.getNotesByFolderIdSorted(folderId, sortCriteria)
                // Tạm thời: Lấy tất cả notes của folder rồi sort ở đây
                // Hoặc, NoteService.getAllNotesForDisplay() đã populate transient Folder rồi thì lọc dễ hơn:
                notesToDisplay = noteService.getAllNotesForDisplay().stream()
                        .filter(note -> note.getFolderId() == currentFolder.getId())
                        .collect(Collectors.toList());

            } else if (currentFolder != null && "Root (DB Error)".equals(currentFolder.getName())) {
                return new ArrayList<>(); // Trả về rỗng nếu Root folder lỗi
            }
            else {
                // Nếu currentFolder là null (ví dụ Root chưa được tạo đúng),
                // hiển thị tất cả note không thuộc folder nào cụ thể (folderId = 0 hoặc ID của Root)
                // hoặc theo một logic mặc định khác.
                // Hiện tại, lấy tất cả note để người dùng vẫn thấy gì đó.
                System.out.println("Current folder is not properly set, displaying all notes.");
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
            // TODO: Tối ưu: Tạo phương thức noteService.searchNotes(query, folderId, sortCriteria)
            // Tạm thời: Lấy notes của folder hiện tại (hoặc tất cả nếu folder không hợp lệ) rồi lọc
            List<Note> notesToSearchIn = getSortedNotes(); // Dùng getSortedNotes để có danh sách theo folder hiện tại
            if (query == null || query.trim().isEmpty()) {
                return notesToSearchIn; // Trả về danh sách đã sort nếu query rỗng
            }
            String lowerQuery = query.toLowerCase().trim();
            return notesToSearchIn.stream()
                    .filter(note -> (note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) ||
                            (note.getContent() != null && note.getContent().toLowerCase().contains(lowerQuery)) ||
                            (note.getTags() != null && note.getTags().stream()
                                    .anyMatch(tag -> tag.getName().toLowerCase().contains(lowerQuery)))
                    )
                    .collect(Collectors.toList());
        } catch (Exception e) { // getSortedNotes có thể throw lỗi ngầm (SQLException được xử lý bên trong)
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
            noteService.createNewFolder(folder);
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

            // Mặc định là di chuyển notes vào Root, hoặc có thể cho người dùng chọn
            // boolean moveNotesToRoot = true;
            // Hiện tại NoteService.deleteExistingFolder đang có logic này, ta sẽ hỏi người dùng
            int notesActionChoice = JOptionPane.showConfirmDialog(mainFrameInstance,
                    "Move notes from folder '" + folder.getName() + "' to Root folder?\n(Choosing 'No' will delete these notes)",
                    "Notes in Folder",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (notesActionChoice == JOptionPane.CANCEL_OPTION) return;
            boolean moveNotes = (notesActionChoice == JOptionPane.YES_OPTION);

            noteService.deleteExistingFolder(folder.getId(), moveNotes);
            JOptionPane.showMessageDialog(mainFrameInstance, "Folder '" + folder.getName() + "' deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

            if (currentFolder != null && currentFolder.getId() == folder.getId()) {
                currentFolder = noteService.getFolderByName("Root"); // Chuyển về Root nếu folder hiện tại bị xóa
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
                folder.setName(oldName); // Khôi phục tên
                JOptionPane.showMessageDialog(mainFrameInstance, "Another folder with name '" + newName.trim() + "' already exists.", "Rename Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            noteService.updateExistingFolder(folder.getId(), folder);
            JOptionPane.showMessageDialog(mainFrameInstance, "Folder renamed to '" + folder.getName() + "' successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            folder.setName(oldName); // Khôi phục tên cũ nếu lỗi
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error renaming folder: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setFolderFavorite(Folder folder, boolean isFavorite) {
        if (folder == null || folder.getId() <= 0) return;
        // Yêu cầu cột 'is_favorite' trong bảng Folders và DAO/Service hỗ trợ
        // Giả sử Folder model và DB đã hỗ trợ.
        boolean oldFavoriteStatus = folder.isFavorite();
        folder.setFavorite(isFavorite);
        try {
            noteService.updateExistingFolder(folder.getId(), folder);
        } catch (SQLException e) {
            folder.setFavorite(oldFavoriteStatus); // Rollback
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error setting folder favorite status: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setFolderMission(Folder folder, boolean isMissionEnabledForAllNotes) {
        if (folder == null || folder.getId() <= 0) return;
        // Đây là một tác vụ phức tạp, nên có một phương thức riêng trong NoteService
        // ví dụ: noteService.setMissionStatusForNotesInFolder(folder.getId(), isMissionEnabledForAllNotes)
        // Phương thức đó sẽ lấy tất cả note trong folder, cập nhật và lưu chúng.
        try {
            List<Note> notesInFolder = noteService.getAllNotesForDisplay().stream()
                    .filter(n -> n.getFolderId() == folder.getId())
                    .collect(Collectors.toList());
            for (Note note : notesInFolder) {
                // Chỉ set isMission, không tự động tạo missionContent
                note.setMission(isMissionEnabledForAllNotes);
                if (!isMissionEnabledForAllNotes) { // Nếu tắt mission cho cả folder, xóa content của từng note
                    note.setMissionContent("");
                    note.setMissionCompleted(false); // Reset trạng thái completed
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
            if (note.getFolderId() <= 0) { // Nếu note chưa được gán folderId
                if (currentFolder != null && currentFolder.getId() > 0) {
                    note.setFolderId(currentFolder.getId());
                    note.setFolder(currentFolder); // Cập nhật transient object
                } else {
                    Folder root = noteService.getFolderByName("Root"); // Mặc định vào Root
                    if (root != null && root.getId() > 0) {
                        note.setFolderId(root.getId());
                        note.setFolder(root);
                    } else {
                        throw new SQLException("Default folder (Root) not found or invalid. Cannot add note.");
                    }
                }
            }
            // Xử lý Alarm nếu có (Alarm đã được tạo từ UI và truyền vào note object)
            if (note.getAlarm() != null) {
                noteService.saveOrUpdateAlarm(note.getAlarm());
                note.setAlarmId(note.getAlarm().getId());
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
            noteService.deleteExistingNote(note.getId());
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
        note.setContent(content); // Cho phép content rỗng
        // note.updateUpdatedAt(); // Được gọi tự động trong setters
        try {
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
            note.setTitle(oldTitle); // Rollback
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
            note.setFavorite(oldStatus); // Rollback
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error setting note favorite status: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setNoteMission(Note note, boolean isMission) {
        if (note == null || note.getId() <= 0) return;
        boolean oldStatus = note.isMission();
        note.setMission(isMission);
        if (!isMission) { // Nếu tắt mission, xóa content và reset completed
            note.setMissionContent("");
            note.setMissionCompleted(false);
        }
        try {
            noteService.updateExistingNote(note.getId(), note);
        } catch (SQLException e) {
            note.setMission(oldStatus); // Rollback
            // Cần rollback cả missionContent và missionCompleted nếu logic phức tạp hơn
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error setting note mission status: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void addTag(Note note, Tag tagFromUI) { // tagFromUI thường chỉ có name, chưa có ID
        if (note == null || note.getId() <= 0 || tagFromUI == null || tagFromUI.getName() == null || tagFromUI.getName().trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Invalid note or tag name.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            // Kiểm tra xem tag đã tồn tại trong note object chưa (dựa trên name)
            boolean tagAlreadyInNoteObject = note.getTags().stream()
                    .anyMatch(existingTag -> existingTag.getName().equalsIgnoreCase(tagFromUI.getName()));
            if (tagAlreadyInNoteObject) {
                JOptionPane.showMessageDialog(mainFrameInstance, "Tag '" + tagFromUI.getName() + "' already added to this note.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Service sẽ xử lý việc tìm tag trong DB bằng tên, hoặc tạo mới nếu chưa có,
            // và trả về đối tượng Tag với ID đúng.
            // Tuy nhiên, NoteService.updateExistingNote hiện tại kỳ vọng list tags trong Note object đã đúng.
            // Nên logic đảm bảo Tag có ID nên nằm ở đây hoặc trong Service trước khi update Note.
            Tag tagToProcess = noteService.getTagByName(tagFromUI.getName()); // Thêm getTagByName vào NoteService nếu chưa có
            if (tagToProcess == null) { // Tag chưa có trong DB, tạo mới
                tagToProcess = new Tag(tagFromUI.getName());
                noteService.saveOrUpdateTag(tagToProcess); // Cần phương thức này trong NoteService (gọi TagDAO.addTag)
                // saveOrUpdateTag sẽ set ID cho tagToProcess
            }

            note.addTag(tagToProcess); // Thêm tag (đã có ID) vào danh sách của note object
            noteService.updateExistingNote(note.getId(), note); // Lưu note với danh sách tag mới
            JOptionPane.showMessageDialog(mainFrameInstance, "Tag '" + tagToProcess.getName() + "' added to note.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            // Cần logic rollback cẩn thận hơn ở đây nếu note.addTag đã xảy ra
            // note.removeTag(tagFromUI); // Có thể không đúng nếu tagFromUI khác tagToProcess về ID
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error adding tag to note: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    // Cần thêm saveOrUpdateTag vào NoteService và TagDAO nếu chưa có
    // public Tag NoteService.saveOrUpdateTag(Tag tag) throws SQLException {
    //     if (tag.getId() <= 0) {
    //         Tag existing = tagDAO.getTagByName(tag.getName());
    //         if (existing != null) {
    //             tag.setId(existing.getId());
    //             return existing;
    //         }
    //         long id = tagDAO.addTag(tag); // addTag đã set ID cho tag
    //     } else {
    //         tagDAO.updateTag(tag.getId(), tag);
    //     }
    //     return tag;
    // }


    public void removeTag(Note note, Tag tag) { // tag ở đây thường là object đã có ID, lấy từ UI
        if (note == null || note.getId() <= 0 || tag == null || tag.getId() <= 0) { // Kiểm tra cả tag.id
            JOptionPane.showMessageDialog(mainFrameInstance, "Invalid note or tag to remove.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean removed = note.getTags().removeIf(t -> t.getId() == tag.getId());

        if (removed) {
            try {
                noteService.updateExistingNote(note.getId(), note);
                JOptionPane.showMessageDialog(mainFrameInstance, "Tag '" + tag.getName() + "' removed from note.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                note.addTag(tag); // Rollback việc xóa khỏi object
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
        Folder oldTransientFolder = note.getFolder(); // Lưu lại để rollback nếu cần

        note.setFolderId(folder.getId());
        note.setFolder(folder); // Cập nhật transient object
        try {
            noteService.updateExistingNote(note.getId(), note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Note '" + note.getTitle() + "' moved to folder '" + folder.getName() + "'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            note.setFolderId(oldFolderId); // Rollback
            note.setFolder(oldTransientFolder); // Rollback transient object
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error moving note: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<Note> getNotes() {
        try {
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

    public void changeTheme() { // Không cần truyền JFrame nếu đã lưu ở field
        try {
            if (isDarkTheme) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            }
            isDarkTheme = !isDarkTheme;
            // saveThemePreference(isDarkTheme); // Lưu trạng thái theme
            if (mainFrameInstance != null) {
                SwingUtilities.updateComponentTreeUI(mainFrameInstance);
            } else {
                // Cố gắng cập nhật tất cả các top-level window
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
        // Trả về trạng thái đã lưu thay vì đọc từ UIManager mỗi lần,
        // để tránh trường hợp UIManager chưa kịp cập nhật hoặc có lỗi.
        return isDarkTheme ? "dark" : "light";
    }

    public List<Note> getMissions() {
        try {
            return noteService.getAllNotesForDisplay().stream()
                    .filter(note -> note.isMission() && (note.getMissionContent() != null && !note.getMissionContent().isEmpty()))
                    // Thêm logic sắp xếp mission của bạn (ví dụ: chưa hoàn thành lên trước, theo alarm...)
                    .sorted(Comparator.comparing(Note::isMissionCompleted) // false (chưa hoàn thành) lên trước
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

        note.setMissionContent(missionContent == null ? "" : missionContent);
        // note.setMission(...) được gọi trong setMissionContent
        // Nếu missionContent rỗng, isMission sẽ là false. Nếu isMission là false, isMissionCompleted cũng nên là false.
        if (!note.isMission()) {
            note.setMissionCompleted(false);
        }

        try {
            noteService.updateExistingNote(note.getId(), note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Mission for note '" + note.getTitle() + "' updated.", "Mission Update", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            note.setMissionContent(oldMissionContent); // Rollback
            note.setMission(oldIsMission);
            note.setMissionCompleted(oldIsCompleted);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error updating mission: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void completeMission(Note note, boolean completed) {
        if (note == null || note.getId() <= 0) return;

        boolean oldCompletedStatus = note.isMissionCompleted();
        Long alarmIdToPotentiallyManage = note.getAlarmId();

        note.setMissionCompleted(completed);
        // String originalMissionContent = note.getMissionContent(); // Giữ lại nếu cần

        if (completed) {
            note.setAlarmId(null); // Xóa liên kết alarmId trên note object khi hoàn thành
            note.setAlarm(null);   // Xóa đối tượng alarm transient
            // note.setMissionContent("[COMPLETED] " + originalMissionContent); // Ví dụ: đánh dấu content
        } else {
            // Nếu "un-complete", không tự động khôi phục alarm. Người dùng phải đặt lại nếu muốn.
            // note.setMissionContent(originalMissionContent.replace("[COMPLETED] ", "")); // Ví dụ
        }

        try {
            noteService.updateExistingNote(note.getId(), note); // Lưu trạng thái mới của note

            if (completed && alarmIdToPotentiallyManage != null && alarmIdToPotentiallyManage > 0) {
                // Nếu mission hoàn thành và có alarm cũ, thì xóa alarm đó khỏi DB.
                noteService.deleteAlarm(alarmIdToPotentiallyManage);
            }
            JOptionPane.showMessageDialog(mainFrameInstance,
                    "Mission '" + note.getTitle() + (completed ? "' completed." : "' marked as not completed."),
                    "Mission Status", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            note.setMissionCompleted(oldCompletedStatus); // Rollback
            if (completed) note.setAlarmId(alarmIdToPotentiallyManage); // Rollback nếu đang cố complete và lỗi
            // note.setMissionContent(originalMissionContent); // Rollback content
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error updating mission completion status: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setAlarm(Note noteToProcess, Object o) {
        if (noteToProcess == null || noteToProcess.getId() <= 0) return;
    }

    public void saveOrUpdateAlarm(Alarm alarm) {
        if (alarm == null || alarm.getId() <= 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Invalid alarm.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            noteService.saveOrUpdateAlarm(alarm);
            JOptionPane.showMessageDialog(mainFrameInstance, "Alarm saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateExistingNote(long id, Note note) throws SQLException {
        // Retrieve the list of notes (or fetch from noteService)
        List<Note> notes = getNotes();

        // Find the note with the matching id (if it exists)
        for (Note existingNote : notes) {
            if (existingNote.getId() == id) {
                // Update the existing note's properties using the new note object
                existingNote.setTitle(note.getTitle());
                existingNote.setContent(note.getContent());
                existingNote.setTags(note.getTags());
                // Optionally update folder or other metadata, if needed

                // Persist the updated note (if using service or database)
                noteService.updateExistingNote(existingNote.getId(), existingNote);
                return;
            }
        }

        // If no matching note was found, throw an exception or handle as appropriate
        throw new IllegalArgumentException("Note with ID " + id + " not found.");
    }
}