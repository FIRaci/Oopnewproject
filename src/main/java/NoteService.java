import java.sql.Connection; // Giữ lại nếu quản lý transaction ở đây
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing notes, folders, tags, and alarms.
 */
public class NoteService {
    private final NoteDAO noteDAO;
    private final FolderDAO folderDAO;
    private final TagDAO tagDAO;
    private final AlarmDAO alarmDAO; // Thêm AlarmDAO

    public NoteService(NoteDAO noteDAO, FolderDAO folderDAO, TagDAO tagDAO, AlarmDAO alarmDAO) {
        this.noteDAO = noteDAO;
        this.folderDAO = folderDAO;
        this.tagDAO = tagDAO;
        this.alarmDAO = alarmDAO; // Khởi tạo AlarmDAO
    }

    // --- Phương thức quản lý Alarm ---
    public long saveOrUpdateAlarm(Alarm alarm) throws SQLException {
        if (alarm == null) {
            throw new IllegalArgumentException("Alarm object cannot be null for save/update.");
        }
        if (alarm.getAlarmTime() == null) {
            throw new IllegalArgumentException("Alarm time cannot be null.");
        }

        if (alarm.getId() <= 0) { // Alarm mới
            return alarmDAO.addAlarm(alarm); // addAlarm trong DAO đã set ID cho object alarm
        } else { // Cập nhật Alarm đã có
            alarmDAO.updateAlarm(alarm.getId(), alarm);
            return alarm.getId();
        }
    }

    public void deleteAlarm(long alarmId) throws SQLException {
        if (alarmId <= 0) {
            // throw new IllegalArgumentException("Invalid alarm ID for delete: " + alarmId);
            System.out.println("Warning: Attempted to delete alarm with invalid ID: " + alarmId);
            return;
        }
        // Quan trọng: Nếu CSDL không có "ON DELETE SET NULL" cho Notes.alarm_id -> Alarms.id_alarm,
        // bạn cần đảm bảo rằng không còn Note nào tham chiếu đến alarmId này trước khi xóa.
        // Hoặc, cập nhật các Note liên quan để set alarm_id = null.
        // Ví dụ: noteDAO.clearAlarmIdFromNotes(alarmId); (cần tạo phương thức này trong NoteDAO)
        alarmDAO.deleteAlarm(alarmId);
    }

    public Alarm getAlarmById(long alarmId) throws SQLException {
        if (alarmId <= 0) {
            return null;
        }
        return alarmDAO.getAlarmById(alarmId);
    }

    // --- Cập nhật các phương thức quản lý Note ---

    public long createNewNote(Note note) throws SQLException {
        if (note == null) throw new IllegalArgumentException("Note cannot be null");
        if (note.getFolderId() <= 0) throw new IllegalArgumentException("Invalid folder ID for the note: " + note.getFolderId());

        // 1. Đảm bảo folder tồn tại
        if (!folderDAO.folderExists(note.getFolderId())) {
            throw new SQLException("Folder with ID " + note.getFolderId() + " does not exist.");
        }

        // 2. Đảm bảo các tags tồn tại và có ID
        if (note.getTags() != null) {
            for (Tag tag : note.getTags()) {
                if (tag.getId() <= 0) {
                    long existingTagId = tagDAO.getTagIdByName(tag.getName());
                    if (existingTagId != -1) tag.setId(existingTagId);
                    else tag.setId(tagDAO.addTag(tag));
                }
            }
        }

        // 3. Xử lý Alarm: Nếu Note có đối tượng Alarm (transient)
        if (note.getAlarm() != null) {
            Alarm alarmToSave = note.getAlarm();
            saveOrUpdateAlarm(alarmToSave); // Lưu hoặc cập nhật Alarm, ID sẽ được set vào alarmToSave
            note.setAlarmId(alarmToSave.getId()); // Gán alarmId cho Note
        } else {
            // Nếu không có đối tượng Alarm, đảm bảo alarmId trên Note cũng là null (hoặc đã được set đúng từ trước)
            // note.setAlarmId(null); // Điều này thường không cần nếu logic UI đúng
        }


        // 4. Thêm Note (NoteDAOImpl.addNote đã tự quản lý transaction cho việc thêm Note và Note_Tag)
        // Tham số folderDAO, tagDAO cho noteDAO.addNote có thể không còn cần thiết nếu NoteDAOImpl tự xử lý
        // các phụ thuộc này, nhưng giữ lại để khớp interface NoteDAO.
        return noteDAO.addNote(note, folderDAO, tagDAO);
    }

    public Note getNoteDetails(long noteId) throws SQLException {
        if (noteId <= 0) return null;
        Note note = noteDAO.getNoteById(noteId, folderDAO, tagDAO);
        if (note != null) {
            // Populate transient Folder object
            if (note.getFolderId() > 0) {
                Folder folder = folderDAO.getFolderById(note.getFolderId());
                note.setFolder(folder); // Gán đối tượng Folder vào trường transient
            }
            // Populate transient Alarm object
            if (note.getAlarmId() != null && note.getAlarmId() > 0) {
                Alarm alarm = alarmDAO.getAlarmById(note.getAlarmId());
                note.setAlarm(alarm); // Gán đối tượng Alarm vào trường transient
            }
        }
        return note;
    }

    public List<Note> getAllNotesForDisplay() throws SQLException {
        List<Note> notes = noteDAO.getAllNotes(folderDAO, tagDAO);
        // Populate transient objects (Folder, Alarm) cho từng Note nếu cần hiển thị chi tiết ngay
        for (Note note : notes) {
            if (note.getFolderId() > 0) {
                note.setFolder(folderDAO.getFolderById(note.getFolderId()));
            }
            if (note.getAlarmId() != null && note.getAlarmId() > 0) {
                note.setAlarm(alarmDAO.getAlarmById(note.getAlarmId()));
            }
        }
        return notes;
    }

    public void updateExistingNote(long noteId, Note note) throws SQLException {
        if (noteId <= 0) throw new IllegalArgumentException("Invalid note ID for update: " + noteId);
        if (note == null) throw new IllegalArgumentException("Note for update cannot be null");
        if (note.getFolderId() <= 0) throw new IllegalArgumentException("Invalid folder ID for note update: " + note.getFolderId());

        // 0. Lấy alarmId cũ của note từ DB (nếu có) để so sánh
        Note existingNoteState = noteDAO.getNoteById(noteId, folderDAO, tagDAO); // Lấy trạng thái note hiện tại
        Long oldAlarmIdFromDB = (existingNoteState != null) ? existingNoteState.getAlarmId() : null;

        // 1. Đảm bảo folder tồn tại
        if (!folderDAO.folderExists(note.getFolderId())) {
            throw new SQLException("Folder with ID " + note.getFolderId() + " does not exist for update.");
        }

        // 2. Đảm bảo các tags tồn tại và có ID
        if (note.getTags() != null) {
            for (Tag tag : note.getTags()) {
                if (tag.getId() <= 0) {
                    long existingTagId = tagDAO.getTagIdByName(tag.getName());
                    if (existingTagId != -1) tag.setId(existingTagId);
                    else tag.setId(tagDAO.addTag(tag));
                }
            }
        }

        // 3. Xử lý Alarm khi update
        Alarm currentAlarmObjectOnNote = note.getAlarm(); // Đối tượng Alarm từ UI/Controller
        Long newAlarmIdForNote = null;

        if (currentAlarmObjectOnNote != null) { // UI muốn set/update một alarm
            saveOrUpdateAlarm(currentAlarmObjectOnNote); // Lưu/update alarm, ID được set vào currentAlarmObjectOnNote
            newAlarmIdForNote = currentAlarmObjectOnNote.getId();
        }
        // Nếu currentAlarmObjectOnNote là null, nghĩa là UI muốn xóa alarm khỏi note
        // (newAlarmIdForNote sẽ vẫn là null)

        note.setAlarmId(newAlarmIdForNote); // Cập nhật alarmId trên đối tượng Note

        // 4. Cập nhật Note (NoteDAOImpl.updateNote đã tự quản lý transaction cho Note và Note_Tag)
        noteDAO.updateNote(noteId, note, folderDAO, tagDAO);

        // 5. Xóa alarm cũ nếu nó không còn được note này sử dụng và không phải là alarm mới/được cập nhật
        if (oldAlarmIdFromDB != null && (newAlarmIdForNote == null || !oldAlarmIdFromDB.equals(newAlarmIdForNote))) {
            // Kiểm tra xem alarm cũ này có được sử dụng bởi note nào khác không, nếu không thì xóa.
            // Logic này phức tạp nếu alarm có thể share.
            // Đơn giản nhất: nếu CSDL có ON DELETE SET NULL và alarm không share, thì xóa alarm cũ là an toàn.
            // Nếu không, cần cẩn thận. Giả sử alarm không share và cần xóa:
            deleteAlarm(oldAlarmIdFromDB);
        }
    }

    public void deleteExistingNote(long noteId) throws SQLException {
        if (noteId <= 0) throw new IllegalArgumentException("Invalid note ID for delete: " + noteId);

        // 1. Lấy alarmId của note sắp xóa (nếu có)
        Note noteToDelete = noteDAO.getNoteById(noteId, folderDAO, tagDAO); // Tái sử dụng folderDAO, tagDAO
        // dù có thể không cần cho getAlarmId
        Long alarmIdToDelete = null;
        if (noteToDelete != null) {
            alarmIdToDelete = noteToDelete.getAlarmId();
        }

        // 2. Xóa Note (NoteDAOImpl.deleteNote đã xử lý xóa liên kết Note_Tag)
        noteDAO.deleteNote(noteId);

        // 3. Nếu Note có Alarm liên kết, xóa Alarm đó (nếu Alarm không được chia sẻ)
        //    Điều này an toàn nếu Notes.alarm_id có FOREIGN KEY với ON DELETE CASCADE đến Alarms.id_alarm
        //    hoặc nếu chúng ta chắc chắn rằng Alarm này chỉ thuộc về Note này.
        //    Nếu không, bạn có thể không muốn xóa Alarm ở đây.
        //    Nếu DB đã có ON DELETE SET NULL trên Notes.alarm_id, thì việc xóa Alarm ở bước 5
        //    của updateExistingNote là đủ, ở đây không cần làm gì với alarm.
        //    Nếu bạn muốn xóa alarm khi note bị xóa:
        if (alarmIdToDelete != null && alarmIdToDelete > 0) {
            // Kiểm tra xem có Note nào khác còn dùng Alarm này không trước khi xóa.
            // Hoặc đơn giản là xóa nếu logic nghiệp vụ cho phép.
            // Giả sử: xóa alarm nếu note bị xóa.
            deleteAlarm(alarmIdToDelete);
        }
    }

    // --- Các phương thức quản lý Folder ---
    public List<Folder> getAllFolders() throws SQLException {
        return folderDAO.getAllFolders();
    }

    public Folder getFolderById(long folderId) throws SQLException {
        if (folderId <= 0) return null;
        return folderDAO.getFolderById(folderId);
    }

    public Folder getFolderByName(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) return null;
        return folderDAO.getFolderByName(name);
    }

    public long createNewFolder(Folder folder) throws SQLException {
        if (folder == null || folder.getName() == null || folder.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Folder or folder name cannot be null or empty.");
        }
        long folderId = folderDAO.addFolder(folder); // addFolder của DAO đã set ID vào object folder
        // folder.setId(folderId); // Không cần nữa nếu DAO đã làm
        return folderId;
    }

    public void updateExistingFolder(long folderId, Folder folder) throws SQLException {
        if (folderId <= 0) {
            throw new IllegalArgumentException("Invalid folder ID for update: " + folderId);
        }
        if (folder == null || folder.getName() == null || folder.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Folder or folder name for update cannot be null or empty.");
        }
        folderDAO.updateFolder(folderId, folder);
    }

    public void deleteExistingFolder(long folderId, boolean moveNotesToRoot) throws SQLException {
        if (folderId <= 0) throw new IllegalArgumentException("Invalid folder ID for delete: " + folderId);

        Folder folderToDelete = folderDAO.getFolderById(folderId);
        if (folderToDelete == null) {
            System.out.println("Folder with ID " + folderId + " not found for deletion.");
            return;
        }
        if ("Root".equalsIgnoreCase(folderToDelete.getName())) {
            throw new SQLException("Cannot delete the Root folder.");
        }

        // Lấy danh sách notes trong thư mục này
        // Cần phương thức NoteDAO.getNotesByFolderId(folderId) để hiệu quả hơn
        // Tạm thời getAllNotes rồi lọc:
        List<Note> allNotes = noteDAO.getAllNotes(folderDAO, tagDAO);
        List<Note> notesInFolder = new ArrayList<>();
        for(Note note : allNotes) {
            if(note.getFolderId() == folderId) {
                notesInFolder.add(note);
            }
        }

        if (moveNotesToRoot) {
            Folder rootFolder = folderDAO.getFolderByName("Root");
            if (rootFolder == null || rootFolder.getId() <= 0) {
                throw new SQLException("Root folder not found or invalid. Cannot move notes.");
            }
            for (Note note : notesInFolder) {
                note.setFolderId(rootFolder.getId());
                note.setFolder(rootFolder); // Cập nhật transient
                noteDAO.updateNote(note.getId(), note, folderDAO, tagDAO); // Cập nhật folder_id trong DB
            }
        } else {
            // Xóa các notes trong thư mục này (bao gồm cả tags và alarms liên quan của từng note)
            for (Note note : notesInFolder) {
                deleteExistingNote(note.getId());
            }
        }
        folderDAO.deleteFolder(folderId);
    }

    // --- (Tùy chọn) Các phương thức quản lý Tag ---
    public List<Tag> getAllTags() throws SQLException {
        return tagDAO.getAllTags();
    }

    public Tag getTagByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        return tagDAO.findByName(name.trim());
    }

    public void saveOrUpdateTag(Tag tagToProcess) {
        if (tagToProcess == null) {
            throw new IllegalArgumentException("Tag object cannot be null for save/update.");
        }
        if (tagToProcess.getName() == null || tagToProcess.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty.");
        }
    }
}