import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
// import java.sql.SQLException; // Không còn cần thiết

public class NoteController {
    private final NoteService noteService;
    private Folder currentFolder;
    private JFrame mainFrameInstance; // Giữ lại để hiển thị JOptionPane và quản lý theme
    private boolean isDarkTheme = false;

    // Constructor đã được sửa để nhận NoteService
    public NoteController(JFrame mainFrameInstance, NoteService noteService) {
        this.mainFrameInstance = mainFrameInstance; // Có thể là null ban đầu nếu MainFrame khởi tạo sau
        if (noteService == null) {
            throw new IllegalArgumentException("NoteService cannot be null in NoteController constructor.");
        }
        this.noteService = noteService;

        // Khởi tạo currentFolder (Root)
        // NoteService sẽ lấy Root folder từ NoteManager
        this.currentFolder = this.noteService.getFolderByName("Root");
        if (this.currentFolder == null) {
            System.out.println("Thư mục Root không tìm thấy bởi NoteService, đang thử tạo mới...");
            Folder rootPlaceholder = new Folder("Root"); // Tạo đối tượng Root mới
            this.currentFolder = noteService.createNewFolder(rootPlaceholder); // Yêu cầu service tạo (NoteManager sẽ gán ID)

            if(this.currentFolder == null || this.currentFolder.getId() == 0) { // Kiểm tra lại sau khi tạo
                System.err.println("LỖI NGHIÊM TRỌNG: Không thể tạo hoặc lấy thư mục Root qua NoteService.");
                // Fallback an toàn hơn nữa, tạo một đối tượng tạm thời để tránh NullPointer ở các chỗ khác
                this.currentFolder = new Folder("Root (Lỗi Khởi Tạo Nghiêm Trọng)");
                this.currentFolder.setId(-System.currentTimeMillis()); // ID âm đặc biệt để nhận biết lỗi
                // Cân nhắc việc hiển thị lỗi cho người dùng hoặc ghi log chi tiết hơn ở đây
                if (this.mainFrameInstance != null) { // Chỉ hiển thị dialog nếu có frame
                    JOptionPane.showMessageDialog(this.mainFrameInstance,
                            "Lỗi nghiêm trọng: Không thể khởi tạo thư mục Root. Vui lòng kiểm tra file dữ liệu (notes.json).",
                            "Lỗi Khởi Tạo",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                System.out.println("Thư mục Root đã được tạo/lấy với ID: " + this.currentFolder.getId());
            }
        } else {
            System.out.println("NoteController đã khởi tạo với thư mục Root ID: " + this.currentFolder.getId());
        }
    }

    // Getter cho mainFrameInstance để các lớp khác có thể truy cập nếu cần (ví dụ AlarmController)
    public JFrame getMainFrameInstance() {
        return mainFrameInstance;
    }

    // Setter cho mainFrameInstance nếu nó được khởi tạo sau NoteController
    public void setMainFrameInstance(JFrame mainFrameInstance) {
        this.mainFrameInstance = mainFrameInstance;
    }

    public List<Note> getSortedNotes() {
        List<Note> notesToDisplay;
        Folder effectiveCurrentFolder = getCurrentFolder(); // Đảm bảo currentFolder hợp lệ

        if (effectiveCurrentFolder != null && !"Root".equalsIgnoreCase(effectiveCurrentFolder.getName())) {
            // Lấy note từ folder cụ thể (không phải Root)
            // Đảm bảo rằng chúng ta đang lọc dựa trên ID của folder được quản lý
            final long currentFolderId = effectiveCurrentFolder.getId();
            if (currentFolderId == 0 && !"Root (Lỗi Khởi Tạo Nghiêm Trọng)".equals(effectiveCurrentFolder.getName())) {
                // Nếu là folder chưa lưu (ví dụ, placeholder lỗi), trả về danh sách rỗng
                System.err.println("Lấy ghi chú từ thư mục chưa được lưu hoặc không hợp lệ: " + effectiveCurrentFolder.getName());
                return new ArrayList<>();
            }
            notesToDisplay = noteService.getAllNotesForDisplay().stream()
                    .filter(note -> note.getFolder() != null && note.getFolder().getId() == currentFolderId)
                    .collect(Collectors.toList());
        } else { // currentFolder là Root hoặc không hợp lệ (đã fallback về Root)
            // Hiển thị tất cả các ghi chú nếu currentFolder là Root
            // Hoặc, nếu muốn Root chỉ chứa note không thuộc folder nào khác, logic cần phức tạp hơn
            // Hiện tại, getAllNotesForDisplay() trả về tất cả, bao gồm cả note trong các folder con của Root (nếu có)
            // Điều này có thể cần điều chỉnh tùy theo ý muốn hiển thị "Root"
            notesToDisplay = noteService.getAllNotesForDisplay();
            System.out.println("Hiển thị tất cả ghi chú (do currentFolder là Root hoặc không hợp lệ). Số lượng: " + notesToDisplay.size());
        }

        return notesToDisplay.stream()
                .sorted(Comparator.comparing(Note::isFavorite, Comparator.reverseOrder())
                        .thenComparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<Note> searchNotes(String query) {
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
    }

    public void selectFolder(Folder folder) {
        if (folder != null && folder.getId() != 0) {
            this.currentFolder = folder;
        } else if (folder != null && "Root".equalsIgnoreCase(folder.getName())) {
            this.currentFolder = noteService.getFolderByName("Root"); // Lấy đối tượng Root được quản lý
            if (this.currentFolder == null) { // Xử lý trường hợp Root không tìm thấy (rất hiếm)
                System.err.println("Lỗi nghiêm trọng: Không thể tìm thấy thư mục Root khi chọn.");
                this.currentFolder = new Folder("Root (Lỗi Chọn Lọc)");
                this.currentFolder.setId(-System.currentTimeMillis());
            }
        } else {
            System.err.println("Cảnh báo: Cố gắng chọn một thư mục không hợp lệ hoặc null. Giữ thư mục hiện tại.");
            // Giữ nguyên currentFolder nếu folder được truyền vào không hợp lệ
            if (this.currentFolder == null || this.currentFolder.getId() == 0) { // Đảm bảo currentFolder luôn hợp lệ
                this.currentFolder = noteService.getFolderByName("Root");
                if (this.currentFolder == null) {
                    this.currentFolder = new Folder("Root (Lỗi Chọn Lọc - Fallback)");
                    this.currentFolder.setId(-System.currentTimeMillis());
                }
            }
        }
    }

    public Folder getCurrentFolder() {
        // Đảm bảo currentFolder luôn là một đối tượng hợp lệ, ít nhất là Root
        if (this.currentFolder == null || this.currentFolder.getId() == 0) {
            Folder root = noteService.getFolderByName("Root");
            if (root != null && root.getId() != 0) {
                this.currentFolder = root;
            } else {
                // Trường hợp rất xấu: không có Root hoặc Root không có ID
                System.err.println("Lỗi nghiêm trọng: Không tìm thấy thư mục Root hợp lệ trong getCurrentFolder.");
                // Tạo một placeholder để tránh crash, nhưng cần điều tra
                // Nếu constructor đã xử lý tốt, trường hợp này không nên xảy ra.
                if (this.currentFolder == null || !"Root (Lỗi Khởi Tạo Nghiêm Trọng)".equals(this.currentFolder.getName())) {
                    this.currentFolder = new Folder("Root (Lỗi Trạng Thái)");
                    this.currentFolder.setId(-System.currentTimeMillis()); // ID âm để dễ nhận biết lỗi
                }
            }
        }
        return this.currentFolder;
    }

    public List<Folder> getFolders() {
        return noteService.getAllFolders();
    }

    public void addNewFolder(String name) {
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Tên thư mục không được để trống.", "Lỗi Nhập Liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Folder existingFolder = noteService.getFolderByName(name.trim());
            if (existingFolder != null) {
                JOptionPane.showMessageDialog(mainFrameInstance, "Thư mục với tên '" + name.trim() + "' đã tồn tại.", "Lỗi Tạo Thư Mục", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Folder folder = new Folder(name.trim());
            noteService.createNewFolder(folder); // Service sẽ gọi NoteManager để gán ID và lưu
            JOptionPane.showMessageDialog(mainFrameInstance, "Thư mục '" + folder.getName() + "' đã được tạo.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(mainFrameInstance, e.getMessage(), "Lỗi Tạo Thư Mục", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi thêm thư mục: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteFolder(Folder folder) {
        if (folder == null || folder.getId() == 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Không thể xóa thư mục không hợp lệ hoặc chưa được lưu.", "Lỗi Thao Tác", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("Root".equalsIgnoreCase(folder.getName())) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Không thể xóa thư mục Root.", "Thao Tác Bị Từ Chối", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int choice = JOptionPane.showConfirmDialog(mainFrameInstance,
                    "Bạn có chắc muốn xóa thư mục \"" + folder.getName() + "\"?\n" +
                            "Hành động này cũng sẽ ảnh hưởng đến các ghi chú trong thư mục này.",
                    "Xác Nhận Xóa Thư Mục",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.NO_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }

            int notesActionChoice = JOptionPane.showConfirmDialog(mainFrameInstance,
                    "Di chuyển các ghi chú từ thư mục '" + folder.getName() + "' vào thư mục Root?\n(Chọn 'Không' sẽ xóa các ghi chú này)",
                    "Ghi Chú Trong Thư Mục",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (notesActionChoice == JOptionPane.CANCEL_OPTION) return;
            boolean moveNotes = (notesActionChoice == JOptionPane.YES_OPTION);

            noteService.deleteExistingFolder(folder.getId(), moveNotes);
            JOptionPane.showMessageDialog(mainFrameInstance, "Thư mục '" + folder.getName() + "' đã được xóa.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);

            if (currentFolder != null && currentFolder.getId() == folder.getId()) {
                currentFolder = noteService.getFolderByName("Root");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi xóa thư mục: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void renameFolder(Folder folder, String newName) {
        if (folder == null || folder.getId() == 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Không thể đổi tên thư mục không hợp lệ hoặc chưa được lưu.", "Lỗi Thao Tác", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newName == null || newName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Tên thư mục mới không được để trống.", "Lỗi Nhập Liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("Root".equalsIgnoreCase(folder.getName()) && !"Root".equalsIgnoreCase(newName.trim())) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Không thể đổi tên thư mục Root thành tên khác.", "Thao Tác Bị Từ Chối", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!"Root".equalsIgnoreCase(folder.getName()) && "Root".equalsIgnoreCase(newName.trim())) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Không thể đổi tên thư mục thành 'Root'. 'Root' là tên dành riêng.", "Thao Tác Bị Từ Chối", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String oldName = folder.getName();
        folder.setName(newName.trim());
        try {
            noteService.updateExistingFolder(folder);
            JOptionPane.showMessageDialog(mainFrameInstance, "Thư mục đã được đổi tên thành '" + folder.getName() + "'.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException e) {
            folder.setName(oldName);
            JOptionPane.showMessageDialog(mainFrameInstance, e.getMessage(), "Lỗi Đổi Tên", JOptionPane.WARNING_MESSAGE);
        }
        catch (Exception e) {
            folder.setName(oldName);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi đổi tên thư mục: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setFolderFavorite(Folder folder, boolean isFavorite) {
        if (folder == null || folder.getId() == 0) return;
        boolean oldFavoriteStatus = folder.isFavorite();
        folder.setFavorite(isFavorite);
        try {
            noteService.updateExistingFolder(folder);
        } catch (Exception e) {
            folder.setFavorite(oldFavoriteStatus);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi cập nhật trạng thái yêu thích của thư mục: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void addNote(Note note) {
        if (note == null) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Không thể thêm ghi chú null.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (note.getFolder() == null || note.getFolder().getId() == 0) {
                Folder folderToAssign = getCurrentFolder();
                note.setFolder(folderToAssign);
                if (folderToAssign != null) { // Kiểm tra null cho folderToAssign
                    note.setFolderId(folderToAssign.getId());
                } else {
                    // Xử lý trường hợp getCurrentFolder() trả về null (rất hiếm nếu logic đúng)
                    System.err.println("Lỗi: Không thể xác định thư mục để gán cho ghi chú mới.");
                    JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi: Không thể xác định thư mục cho ghi chú mới.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            noteService.createNewNote(note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Ghi chú '" + note.getTitle() + "' đã được thêm.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi thêm ghi chú: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteNote(Note note) {
        if (note == null || note.getId() == 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Không thể xóa ghi chú không hợp lệ hoặc chưa được lưu.", "Lỗi Thao Tác", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            noteService.deleteExistingNote(note.getId());
            JOptionPane.showMessageDialog(mainFrameInstance, "Ghi chú '" + note.getTitle() + "' đã được xóa.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi xóa ghi chú: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateNote(Note note, String title, String content) {
        if (note == null || note.getId() == 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Không thể cập nhật ghi chú không hợp lệ hoặc chưa được lưu.", "Lỗi Thao Tác", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (title == null || title.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Tiêu đề ghi chú không được để trống.", "Lỗi Nhập Liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        note.setTitle(title.trim());
        note.setContent(content);
        note.updateUpdatedAt(); // Cập nhật thời gian sửa đổi

        try {
            noteService.updateExistingNote(note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Ghi chú '" + note.getTitle() + "' đã được cập nhật.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi cập nhật ghi chú: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void renameNote(Note note, String newTitle) {
        if (note == null || note.getId() == 0) return;
        if (newTitle == null || newTitle.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Tiêu đề mới không được để trống.", "Lỗi Nhập Liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String oldTitle = note.getTitle();
        note.setTitle(newTitle.trim());
        note.updateUpdatedAt();
        try {
            noteService.updateExistingNote(note);
        } catch (Exception e) {
            note.setTitle(oldTitle);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi đổi tên ghi chú: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setNoteFavorite(Note note, boolean isFavorite) {
        if (note == null || note.getId() == 0) return;
        boolean oldStatus = note.isFavorite();
        note.setFavorite(isFavorite);
        note.updateUpdatedAt();
        try {
            noteService.updateExistingNote(note);
        } catch (Exception e) {
            note.setFavorite(oldStatus);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi cập nhật trạng thái yêu thích của ghi chú: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void addTag(Note note, Tag tagFromUI) {
        if (note == null || note.getId() == 0 || tagFromUI == null || tagFromUI.getName() == null || tagFromUI.getName().trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Ghi chú hoặc tên tag không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Tag managedTag = noteService.getOrCreateTag(tagFromUI.getName().trim());

            boolean tagAlreadyInNoteObject = note.getTags().stream()
                    .anyMatch(existingTag -> existingTag.getId() == managedTag.getId());

            if (tagAlreadyInNoteObject) {
                JOptionPane.showMessageDialog(mainFrameInstance, "Tag '" + managedTag.getName() + "' đã có trong ghi chú này.", "Thông Tin", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            note.addTag(managedTag);
            note.updateUpdatedAt();
            noteService.updateExistingNote(note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Tag '" + managedTag.getName() + "' đã được thêm vào ghi chú.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi thêm tag vào ghi chú: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void removeTag(Note note, Tag tagToRemove) {
        if (note == null || note.getId() == 0 || tagToRemove == null || tagToRemove.getId() == 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Ghi chú hoặc tag không hợp lệ để xóa.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean removed = note.getTags().removeIf(t -> t.getId() == tagToRemove.getId());
        if (removed) {
            note.updateUpdatedAt();
            try {
                noteService.updateExistingNote(note);
                JOptionPane.showMessageDialog(mainFrameInstance, "Tag '" + tagToRemove.getName() + "' đã được xóa khỏi ghi chú.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                note.addTag(tagToRemove);
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi xóa tag khỏi ghi chú: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void moveNoteToFolder(Note note, Folder folder) {
        if (note == null || note.getId() == 0 || folder == null || folder.getId() == 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Ghi chú hoặc thư mục đích không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (note.getFolder() != null && note.getFolder().getId() == folder.getId()) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Ghi chú đã ở trong thư mục '" + folder.getName() + "'.", "Thông Tin", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Folder oldFolder = note.getFolder();
        note.setFolder(folder);
        note.setFolderId(folder.getId());
        note.updateUpdatedAt();
        try {
            // NoteService.updateExistingNote sẽ gọi NoteManager.updateNote,
            // nơi xử lý việc cập nhật note trong danh sách của folder cũ và mới.
            noteService.updateExistingNote(note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Ghi chú '" + note.getTitle() + "' đã được chuyển đến thư mục '" + folder.getName() + "'.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            note.setFolder(oldFolder);
            if (oldFolder != null) note.setFolderId(oldFolder.getId()); else note.setFolderId(0);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi di chuyển ghi chú: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<Note> getNotes() {
        return noteService.getAllNotesForDisplay();
    }

    public Optional<Folder> getFolderByName(String name) {
        if (name == null || name.trim().isEmpty()) return Optional.empty();
        Folder folder = noteService.getFolderByName(name.trim());
        return Optional.ofNullable(folder);
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
                if (mainFrameInstance instanceof MainFrame) {
                    ((MainFrame) mainFrameInstance).triggerThemeUpdate(isDarkTheme);
                }
            } else {
                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi thay đổi giao diện: " + e.getMessage(), "Lỗi Giao Diện", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getCurrentTheme() {
        return isDarkTheme ? "dark" : "light";
    }

    public List<Note> getMissions() {
        return noteService.getAllNotesForDisplay().stream()
                .filter(note -> note.isMission() && (note.getMissionContent() != null && !note.getMissionContent().isEmpty()))
                .sorted(Comparator.comparing(Note::isMissionCompleted)
                        .thenComparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public void updateMission(Note note, String missionContent) {
        if (note == null || note.getId() == 0) return;
        String oldMissionContent = note.getMissionContent();
        boolean oldIsMission = note.isMission();

        note.setMissionContent(missionContent == null ? "" : missionContent.trim());
        if (!note.isMission()) {
            note.setMissionCompleted(false);
        }
        note.updateUpdatedAt();
        try {
            noteService.updateExistingNote(note);
            JOptionPane.showMessageDialog(mainFrameInstance, "Nhiệm vụ cho ghi chú '" + note.getTitle() + "' đã được cập nhật.", "Cập Nhật Nhiệm Vụ", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            note.setMissionContent(oldMissionContent);
            note.setMission(oldIsMission);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi cập nhật nhiệm vụ: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void completeMission(Note note, boolean completed) {
        if (note == null || note.getId() == 0) return;
        boolean oldCompletedStatus = note.isMissionCompleted();
        Alarm oldAlarm = note.getAlarm();

        note.setMissionCompleted(completed);
        note.updateUpdatedAt();

        try {
            if (completed && note.getAlarm() != null) {
                System.out.println("Nhiệm vụ '" + note.getTitle() + "' hoàn thành, xóa báo thức liên quan (ID: " + note.getAlarm().getId() + ")");
                note.setAlarm(null);
            }
            noteService.updateExistingNote(note);

            JOptionPane.showMessageDialog(mainFrameInstance,
                    "Nhiệm vụ '" + note.getTitle() + (completed ? "' đã hoàn thành." : "' được đánh dấu chưa hoàn thành."),
                    "Trạng Thái Nhiệm Vụ", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            note.setMissionCompleted(oldCompletedStatus);
            if (completed) {
                note.setAlarm(oldAlarm);
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi cập nhật trạng thái hoàn thành nhiệm vụ: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setAlarm(Note note, Alarm alarm) {
        if (note == null || note.getId() == 0) {
            JOptionPane.showMessageDialog(mainFrameInstance, "Ghi chú không hợp lệ để đặt báo thức.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Alarm oldAlarm = note.getAlarm();
        note.updateUpdatedAt();

        try {
            if (alarm != null) {
                noteService.ensureAlarmHasId(alarm); // Đảm bảo alarm có ID
                note.setAlarm(alarm);
            } else {
                note.setAlarm(null);
            }
            noteService.updateExistingNote(note);

            String message = (alarm != null) ? "Báo thức đã được đặt/cập nhật cho ghi chú '" + note.getTitle() + "'."
                    : "Báo thức đã được xóa cho ghi chú '" + note.getTitle() + "'.";
            System.out.println(message);
        } catch (Exception e) {
            note.setAlarm(oldAlarm);
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Lỗi khi đặt báo thức: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateExistingNoteInControllerList(long id, Note updatedNote) {
        List<Note> notes = getNotes();
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId() == id) {
                notes.set(i, updatedNote);
                return;
            }
        }
        System.err.println("Cảnh báo: updateExistingNoteInControllerList không tìm thấy note với ID: " + id);
    }

    public void updateExistingNote(long id, Note note) {
        try {
            // Fetch the existing note
            Note existingNote = noteService.getNoteById(id);
            if (existingNote == null) {
                JOptionPane.showMessageDialog(mainFrameInstance, "Note with ID " + id + " not found.", "Update Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Update fields of the existing note with the new note's data
            existingNote.setTitle(note.getTitle());
            existingNote.setContent(note.getContent());
            existingNote.setFavorite(note.isFavorite());
            existingNote.setMission(note.isMission());
            existingNote.setMissionContent(note.getMissionContent());
            existingNote.setMissionCompleted(note.isMissionCompleted());
            existingNote.setAlarm(note.getAlarm());
            existingNote.setTags(note.getTags());
            existingNote.updateUpdatedAt();

            // Persist the updated note
            noteService.updateExistingNote(existingNote);

            // Notify about the update
            JOptionPane.showMessageDialog(mainFrameInstance, "Note with ID " + id + " successfully updated.", "Update Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrameInstance, "Error updating note: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    public NoteService getNoteService() {
        return noteService;
    }
}
