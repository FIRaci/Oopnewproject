import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class NoteManager {
    private final List<Note> notes;
    private final List<Folder> folders;
    private final List<Tag> tags;
    private final DataStorage dataStorage;

    private AtomicLong nextNoteId = new AtomicLong(1);
    private AtomicLong nextFolderId = new AtomicLong(1);
    private AtomicLong nextTagId = new AtomicLong(1);
    private AtomicLong nextAlarmId = new AtomicLong(1);

    public NoteManager() {
        notes = new ArrayList<>();
        folders = new ArrayList<>();
        tags = new ArrayList<>();
        dataStorage = new DataStorage("notes.json");

        System.out.println("[NoteManager Constructor] Đang tải dữ liệu từ DataStorage...");
        dataStorage.load(this);

        initializeAndSanitizeIds();
        ensureRootFolderExists();
        relinkObjects();

        System.out.println("[NoteManager Constructor] Khởi tạo hoàn tất. Notes: " + notes.size() +
                ", Folders: " + folders.size() +
                ", Tags: " + tags.size());
        System.out.println("[NoteManager Constructor] ID Generators: nextNoteId=" + nextNoteId +
                ", nextFolderId=" + nextFolderId +
                ", nextTagId=" + nextTagId +
                ", nextAlarmId=" + nextAlarmId);
    }

    private void initializeAndSanitizeIds() {
        System.out.println("[NoteManager] Bắt đầu initializeAndSanitizeIds...");
        boolean dataModified = false;

        // Tìm ID lớn nhất từ dữ liệu đã tải
        long maxLoadedNoteId = notes.stream().mapToLong(Note::getId).filter(id -> id > 0).max().orElse(0);
        long maxLoadedFolderId = folders.stream().mapToLong(Folder::getId).filter(id -> id > 0).max().orElse(0);
        long maxLoadedTagId = tags.stream().mapToLong(Tag::getId).filter(id -> id > 0).max().orElse(0);
        long maxLoadedAlarmId = notes.stream()
                .filter(n -> n.getAlarm() != null && n.getAlarm().getId() > 0)
                .mapToLong(n -> n.getAlarm().getId())
                .max().orElse(0);

        // Thiết lập bộ đếm ID
        nextNoteId.set(maxLoadedNoteId + 1);
        nextFolderId.set(maxLoadedFolderId + 1);
        nextTagId.set(maxLoadedTagId + 1);
        nextAlarmId.set(maxLoadedAlarmId + 1);
        System.out.println("[NoteManager] ID generators được thiết lập từ dữ liệu tải: Note=" + nextNoteId +
                ", Folder=" + nextFolderId + ", Tag=" + nextTagId + ", Alarm=" + nextAlarmId);

        // Gán ID cho các đối tượng chưa có ID hợp lệ (ID = 0)
        for (Folder folder : new ArrayList<>(folders)) {
            if (folder.getId() == 0) {
                System.out.println("[NoteManager] Folder '" + folder.getName() + "' có ID 0. Đang gán ID mới...");
                folder.setId(generateNewFolderId());
                System.out.println("[NoteManager] ID mới cho '" + folder.getName() + "': " + folder.getId());
                dataModified = true;
            }
        }

        for (Tag tag : new ArrayList<>(tags)) {
            if (tag.getId() == 0) {
                System.out.println("[NoteManager] Tag '" + tag.getName() + "' có ID 0. Đang gán ID mới...");
                tag.setId(generateNewTagId());
                System.out.println("[NoteManager] ID mới cho '" + tag.getName() + "': " + tag.getId());
                dataModified = true;
            }
        }

        for (Note note : new ArrayList<>(notes)) {
            if (note.getId() == 0) {
                System.out.println("[NoteManager] Note '" + note.getTitle() + "' có ID 0. Đang gán ID mới...");
                note.setId(generateNewNoteId());
                System.out.println("[NoteManager] ID mới cho '" + note.getTitle() + "': " + note.getId());
                dataModified = true;
            }
            if (note.getAlarm() != null && note.getAlarm().getId() == 0) {
                System.out.println("[NoteManager] Alarm cho note '" + note.getTitle() + "' có ID 0. Đang gán ID mới...");
                note.getAlarm().setId(generateNewAlarmId());
                System.out.println("[NoteManager] ID Alarm mới: " + note.getAlarm().getId());
                dataModified = true;
            }
        }

        if (dataModified) {
            System.out.println("[NoteManager] Dữ liệu đã được sửa đổi trong quá trình chuẩn hóa ID, đang lưu lại...");
            saveData();
        }
        System.out.println("[NoteManager] Hoàn tất initializeAndSanitizeIds.");
    }

    private void ensureRootFolderExists() {
        System.out.println("[NoteManager] Đang kiểm tra thư mục Root...");
        Folder root = folders.stream()
                .filter(f -> "Root".equalsIgnoreCase(f.getName()))
                .findFirst()
                .orElse(null);
        boolean rootModifiedOrCreated = false;
        if (root == null) {
            System.out.println("[NoteManager] Thư mục Root không tồn tại, đang tạo mới...");
            root = new Folder("Root");
            root.setId(generateNewFolderId());
            folders.add(0, root);
            System.out.println("[NoteManager] Đã tạo thư mục Root với ID: " + root.getId());
            rootModifiedOrCreated = true;
        } else {
            if (root.getId() == 0) {
                System.out.println("[NoteManager] Thư mục Root có ID 0, đang gán ID mới: " + root.getName());
                root.setId(generateNewFolderId());
                System.out.println("[NoteManager] ID mới cho Root: " + root.getId());
                rootModifiedOrCreated = true;
            }
            if (folders.indexOf(root) != 0) {
                folders.remove(root);
                folders.add(0, root);

            }
            System.out.println("[NoteManager] Thư mục Root đã tồn tại với ID: " + root.getId());
        }
        if (rootModifiedOrCreated) {
            saveData();
        }
    }

    private void relinkObjects() {
        System.out.println("[NoteManager] Đang tái liên kết các đối tượng...");
        Map<Long, Folder> folderMapById = folders.stream()
                .filter(f -> f.getId() != 0)
                .collect(Collectors.toMap(Folder::getId, f -> f, (f1, f2) -> f1));
        Map<Long, Tag> tagMapById = tags.stream()
                .filter(t -> t.getId() != 0)
                .collect(Collectors.toMap(Tag::getId, t -> t, (t1, t2) -> t1));
        Folder rootFolder = getRootFolder();

        for (Note note : notes) {
            note.setDisableAutoUpdate(true);
            Folder associatedFolder = null;
            if (note.getFolderId() != 0) {
                associatedFolder = folderMapById.get(note.getFolderId());
            }

            if (associatedFolder != null) {
                note.setFolder(associatedFolder);
                if (!associatedFolder.getNotes().contains(note)) {
                    associatedFolder.addNote(note);
                }
            } else {
                if (note.getFolderId() != 0) {
                    System.err.println("[NoteManager relink] Cảnh báo: Note '" + note.getTitle() + "' có folderId " + note.getFolderId() + " nhưng không tìm thấy folder. Gán vào Root.");
                }
                note.setFolder(rootFolder);
                if (rootFolder != null && !rootFolder.getNotes().contains(note)) {
                    rootFolder.addNote(note);
                }
            }

            // Liên kết Tags cho Note
            List<Tag> resolvedTags = new ArrayList<>();
            if (note.getTags() != null) {
                for (Tag tagStub : note.getTags()) {
                    Tag resolvedTag = null;
                    if (tagStub.getId() != 0) {
                        resolvedTag = tagMapById.get(tagStub.getId());
                    }
                    if (resolvedTag == null && tagStub.getName() != null && !tagStub.getName().isEmpty()) {
                        resolvedTag = tags.stream()
                                .filter(t -> t.getName().equalsIgnoreCase(tagStub.getName()))
                                .findFirst().orElse(null);
                    }
                    if (resolvedTag != null) {
                        resolvedTags.add(resolvedTag);
                    } else {
                        System.err.println("[NoteManager relink] Cảnh báo: Note '" + note.getTitle() + "' có tag '" + tagStub.getName() + "' (ID: " + tagStub.getId() + ") không thể resolve.");
                    }
                }
            }
            note.setTags(resolvedTags);
            note.setDisableAutoUpdate(false);
        }

        // Liên kết subFolders cho Folders (nếu dùng subFolderNames)
        for (Folder folder : folders) {
            if (folder.getSubFolderNames() != null && !folder.getSubFolderNames().isEmpty()) {
                folder.getSubFolders().clear();
                for (String subFolderName : folder.getSubFolderNames()) {
                    Folder subFolder = folders.stream()
                            .filter(f -> f.getName().equalsIgnoreCase(subFolderName))
                            .findFirst().orElse(null);
                    if (subFolder != null && subFolder.getId() != folder.getId()) {
                        folder.addSubFolder(subFolder);
                    } else if (subFolder != null && subFolder.getId() == folder.getId()){
                        System.err.println("[NoteManager relink] Cảnh báo: Folder '" + folder.getName() + "' không thể là subfolder của chính nó.");
                    }
                }
            }
        }
        System.out.println("[NoteManager] Hoàn tất tái liên kết đối tượng.");
    }



    public long generateNewNoteId() { return nextNoteId.getAndIncrement(); }
    public long generateNewFolderId() { return nextFolderId.getAndIncrement(); }
    public long generateNewTagId() { return nextTagId.getAndIncrement(); }
    public long generateNewAlarmId() { return nextAlarmId.getAndIncrement(); }

    public void addFolder(Folder folder) {
        if (folder == null) {
            throw new IllegalArgumentException("Folder cannot be null");
        }
        if (folder.getId() == 0) {

            Optional<Folder> existingByName = folders.stream()
                    .filter(f -> f.getName().equalsIgnoreCase(folder.getName()))
                    .findFirst();
            if (existingByName.isPresent()) {
                System.out.println("[NoteManager addFolder] Thư mục '" + folder.getName() + "' đã tồn tại với ID " + existingByName.get().getId() + ". Không thêm mới.");
               return;
            }
            folder.setId(generateNewFolderId());
        }


        final long folderIdToAdd = folder.getId();
        boolean alreadyExistsById = folders.stream().anyMatch(f -> f.getId() == folderIdToAdd);

        if (!alreadyExistsById) {
            folders.add(folder);
            System.out.println("[NoteManager addFolder] Đã thêm thư mục: " + folder.getName() + " với ID: " + folder.getId());
            saveData();
        } else if (!folders.contains(folder)) {

            folders.removeIf(f -> f.getId() == folderIdToAdd);
            folders.add(folder);
            System.out.println("[NoteManager addFolder] Đã cập nhật instance cho thư mục: " + folder.getName() + " với ID: " + folder.getId());
            saveData();
        }
    }

    // --- Note Management ---
    public void addNote(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note cannot be null");
        }
        if (note.getId() == 0) {
            note.setId(generateNewNoteId());
        }
        if (note.getAlarm() != null && note.getAlarm().getId() == 0) {
            note.getAlarm().setId(generateNewAlarmId());
        }

        List<Tag> resolvedTags = new ArrayList<>();
        if (note.getTags() != null) {
            for (Tag tag : note.getTags()) {
                resolvedTags.add(getOrCreateTag(tag.getName()));
            }
        }
        note.setTags(resolvedTags);

        Folder parentFolder = note.getFolder();
        if (parentFolder == null || parentFolder.getId() == 0) {
            parentFolder = getRootFolder();
            note.setFolder(parentFolder);
        } else {
            Folder managedParentFolder = getFolderById(parentFolder.getId());
            if (managedParentFolder != null) {
                parentFolder = managedParentFolder;
                note.setFolder(parentFolder);
            } else {
                System.err.println("Cảnh báo: Folder ID " + parentFolder.getId() + " cho note '" + note.getTitle() + "' không tìm thấy. Gán vào Root.");
                parentFolder = getRootFolder();
                note.setFolder(parentFolder);
            }
        }
        note.setFolderId(parentFolder.getId());


        if (!notes.contains(note)) {
            notes.add(note);
            if (!parentFolder.getNotes().contains(note)) {
                parentFolder.addNote(note);
            }
        } else {
            updateNote(note);
            return;
        }
        System.out.println("[NoteManager addNote] Đã thêm/cập nhật note: " + note.getTitle() + " với ID: " + note.getId());
        saveData();
    }

    public void updateNote(Note noteToUpdate) {
        if (noteToUpdate == null || noteToUpdate.getId() == 0) {
            throw new IllegalArgumentException("Note to update must not be null and must have a valid ID.");
        }
        if (noteToUpdate.getAlarm() != null && noteToUpdate.getAlarm().getId() == 0) {
            noteToUpdate.getAlarm().setId(generateNewAlarmId());
        }

        List<Tag> resolvedTags = new ArrayList<>();
        if (noteToUpdate.getTags() != null) {
            for (Tag tag : noteToUpdate.getTags()) {
                resolvedTags.add(getOrCreateTag(tag.getName()));
            }
        }
        noteToUpdate.setTags(resolvedTags);

        Folder parentFolder = noteToUpdate.getFolder();
        if (parentFolder == null || parentFolder.getId() == 0) {
            parentFolder = getRootFolder();
            noteToUpdate.setFolder(parentFolder);
        } else {
            Folder managedParentFolder = getFolderById(parentFolder.getId());
            if (managedParentFolder != null) {
                parentFolder = managedParentFolder;
                noteToUpdate.setFolder(parentFolder);
            } else {
                System.err.println("Cảnh báo: Folder ID " + parentFolder.getId() + " cho note update '" + noteToUpdate.getTitle() + "' không tìm thấy. Gán vào Root.");
                parentFolder = getRootFolder();
                noteToUpdate.setFolder(parentFolder);
            }
        }
        noteToUpdate.setFolderId(parentFolder.getId());


        int index = -1;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId() == noteToUpdate.getId()) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            Note oldNoteVersion = notes.get(index);
            if (oldNoteVersion.getFolder() != null && oldNoteVersion.getFolder().getId() != noteToUpdate.getFolderId()) {
                Folder oldActualFolder = getFolderById(oldNoteVersion.getFolder().getId());
                if(oldActualFolder != null) oldActualFolder.removeNote(oldNoteVersion);
            }
            notes.set(index, noteToUpdate);
            if (parentFolder != null && !parentFolder.getNotes().contains(noteToUpdate)) {
                parentFolder.addNote(noteToUpdate);
            }
            System.out.println("[NoteManager updateNote] Đã cập nhật note: " + noteToUpdate.getTitle() + " với ID: " + noteToUpdate.getId());
        } else {
            System.err.println("[NoteManager updateNote] Cảnh báo: updateNote được gọi cho note không có trong danh sách. ID: " + noteToUpdate.getId() + ". Thêm như note mới.");
            notes.add(noteToUpdate);
            if (parentFolder != null && !parentFolder.getNotes().contains(noteToUpdate)) {
                parentFolder.addNote(noteToUpdate);
            }
        }
        saveData();
    }

    public void deleteNote(long noteId) {
        Note noteToRemove = getNoteById(noteId);
        if (noteToRemove != null) {
            if (noteToRemove.getFolder() != null) {
                Folder parent = getFolderById(noteToRemove.getFolder().getId());
                if(parent != null) parent.removeNote(noteToRemove);
            }
            notes.remove(noteToRemove);
            System.out.println("[NoteManager deleteNote] Đã xóa note với ID: " + noteId);
            saveData();
        } else {
            System.err.println("[NoteManager deleteNote] Note với ID " + noteId + " không tìm thấy để xóa.");
        }
    }

    public Note getNoteById(long noteId) {
        return notes.stream().filter(note -> note.getId() == noteId).findFirst().orElse(null);
    }

    public List<Note> getAllNotes() {
        return new ArrayList<>(notes);
    }

    public List<Note> getNotesInFolder(Folder folder) {
        if (folder == null || folder.getId() == 0) {
            System.err.println("[NoteManager getNotesInFolder] Nhận folder null hoặc ID 0. Trả về danh sách rỗng.");
            return new ArrayList<>();
        }
        Folder managedFolder = getFolderById(folder.getId());
        if (managedFolder == null) {
            System.err.println("[NoteManager getNotesInFolder] Không tìm thấy folder được quản lý với ID: " + folder.getId() + ". Trả về danh sách rỗng.");
            return new ArrayList<>();
        }
        return new ArrayList<>(managedFolder.getNotes());
    }

    public void updateFolder(Folder folderToUpdate) {
        if (folderToUpdate == null || folderToUpdate.getId() == 0) {
            throw new IllegalArgumentException("Folder to update must not be null and must have a valid ID.");
        }
        if ("Root".equalsIgnoreCase(folderToUpdate.getName()) && getRootFolder().getId() != folderToUpdate.getId()) {
            throw new IllegalArgumentException("Cannot rename another folder to 'Root'.");
        }

        int index = -1;
        for (int i = 0; i < folders.size(); i++) {
            if (folders.get(i).getId() == folderToUpdate.getId()) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            final long currentId = folderToUpdate.getId();
            Optional<Folder> conflictingFolder = folders.stream()
                    .filter(f -> f.getName().equalsIgnoreCase(folderToUpdate.getName()) && f.getId() != currentId)
                    .findFirst();

            if (conflictingFolder.isPresent()) {
                throw new IllegalArgumentException("Another folder with the name '" + folderToUpdate.getName() + "' already exists.");
            }
            folders.set(index, folderToUpdate);
            System.out.println("[NoteManager updateFolder] Đã cập nhật folder: " + folderToUpdate.getName() + " với ID: " + folderToUpdate.getId());
            saveData();
        } else {
            throw new IllegalArgumentException("Folder with ID " + folderToUpdate.getId() + " not found for update.");
        }
    }

    public void deleteFolder(long folderId, boolean deleteAssociatedNotes) {
        Folder folderToRemove = getFolderById(folderId);
        if (folderToRemove == null) {
            System.err.println("[NoteManager deleteFolder] Folder với ID " + folderId + " không tìm thấy để xóa.");
            return;
        }
        if ("Root".equalsIgnoreCase(folderToRemove.getName())) {
            throw new IllegalStateException("Cannot delete the Root folder.");
        }

        if (deleteAssociatedNotes) {
            List<Note> notesInFolderCopy = new ArrayList<>(folderToRemove.getNotes());
            for (Note note : notesInFolderCopy) {
                deleteNote(note.getId());
            }
        } else {
            Folder root = getRootFolder();
            if (root == null) throw new IllegalStateException("Root folder not found, cannot move notes.");
            List<Note> notesToMoveCopy = new ArrayList<>(folderToRemove.getNotes());
            for (Note note : notesToMoveCopy) {
                note.setFolder(root);
                note.setFolderId(root.getId());
                updateNote(note);
            }
        }
        folders.remove(folderToRemove);
        System.out.println("[NoteManager deleteFolder] Đã xóa folder: " + folderToRemove.getName() + " với ID: " + folderId);
        saveData();
    }

    public Folder getRootFolder() {
        if (folders.isEmpty() || !"Root".equalsIgnoreCase(folders.get(0).getName()) || folders.get(0).getId() == 0) {
            System.err.println("[NoteManager getRootFolder] Root folder không hợp lệ hoặc không ở vị trí đầu. Đang thử đảm bảo lại...");
            ensureRootFolderExists();
            if (folders.isEmpty() || !"Root".equalsIgnoreCase(folders.get(0).getName()) || folders.get(0).getId() == 0) {
                System.err.println("[NoteManager getRootFolder] LỖI NGHIÊM TRỌNG: Không thể đảm bảo Root folder hợp lệ!");
                if (folders.isEmpty()) {
                    Folder tempRoot = new Folder("Root (Fallback Cấp Cứu)");
                    tempRoot.setId(generateNewFolderId());
                    folders.add(0, tempRoot);
                    saveData();
                    return tempRoot;
                }
            }
        }
        return folders.get(0);
    }

    public List<Folder> getAllFolders() {
        return new ArrayList<>(folders);
    }

    public Folder getFolderById(long folderId) {
        if (folderId == 0) return null;
        return folders.stream().filter(folder -> folder.getId() == folderId).findFirst().orElse(null);
    }

    public Optional<Folder> getFolderByName(String name) {
        if (name == null || name.trim().isEmpty()) return Optional.empty();
        return folders.stream().filter(f -> f.getName().equalsIgnoreCase(name.trim())).findFirst();
    }

    public Tag getOrCreateTag(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty.");
        }
        String trimmedName = tagName.trim();
        Optional<Tag> existingTag = tags.stream()
                .filter(t -> t.getName().equalsIgnoreCase(trimmedName))
                .findFirst();
        if (existingTag.isPresent()) {
            return existingTag.get();
        } else {
            Tag newTag = new Tag(trimmedName);
            newTag.setId(generateNewTagId());
            tags.add(newTag);
            System.out.println("[NoteManager getOrCreateTag] Đã tạo tag mới: " + newTag.getName() + " với ID: " + newTag.getId());
            saveData();
            return newTag;
        }
    }

    public void addTagToNote(Note note, String tagName) {
        if (note == null || note.getId() == 0) throw new IllegalArgumentException("Note cannot be null or unsaved.");
        Note managedNote = getNoteById(note.getId());
        if(managedNote == null) throw new IllegalArgumentException("Note not found in manager.");

        Tag tag = getOrCreateTag(tagName);

        boolean alreadyHasTag = managedNote.getTags().stream().anyMatch(t -> t.getId() == tag.getId());
        if (!alreadyHasTag) {
            managedNote.addTag(tag);
            updateNote(managedNote);
            System.out.println("[NoteManager addTagToNote] Đã thêm tag '" + tag.getName() + "' vào note '" + managedNote.getTitle() + "'");
        } else {
            System.out.println("[NoteManager addTagToNote] Note '" + managedNote.getTitle() + "' đã có tag '" + tag.getName() + "'");
        }
    }

    public void removeTagFromNote(Note note, Tag tagToRemove) {
        if (note == null || note.getId() == 0 || tagToRemove == null || tagToRemove.getId() == 0) {
            throw new IllegalArgumentException("Note or Tag to remove cannot be null or unsaved");
        }
        Note managedNote = getNoteById(note.getId());
        if(managedNote == null) throw new IllegalArgumentException("Note not found in manager.");

        boolean removed = managedNote.getTags().removeIf(t -> t.getId() == tagToRemove.getId());
        if (removed) {
            updateNote(managedNote);
            System.out.println("[NoteManager removeTagFromNote] Đã xóa tag '" + tagToRemove.getName() + "' khỏi note '" + managedNote.getTitle() + "'");
        }
    }

    public List<Tag> getAllTags() {
        return new ArrayList<>(tags);
    }

    public Tag getTagById(long tagId) {
        if (tagId == 0) return null;
        return tags.stream().filter(tag -> tag.getId() == tagId).findFirst().orElse(null);
    }

    public Tag getTagByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return tags.stream().filter(t -> t.getName().equalsIgnoreCase(name.trim())).findFirst().orElse(null);
    }

    public void updateTag(Tag tagToUpdate) {
        if (tagToUpdate == null || tagToUpdate.getId() == 0) {
            throw new IllegalArgumentException("Tag to update must not be null and must have a valid ID.");
        }
        int index = -1;
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).getId() == tagToUpdate.getId()) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            final long currentId = tagToUpdate.getId();
            Optional<Tag> conflictingTag = tags.stream()
                    .filter(t -> t.getName().equalsIgnoreCase(tagToUpdate.getName()) && t.getId() != currentId)
                    .findFirst();
            if (conflictingTag.isPresent()) {
                throw new IllegalArgumentException("Another tag with the name '" + tagToUpdate.getName() + "' already exists.");
            }
            tags.set(index, tagToUpdate);
            System.out.println("[NoteManager updateTag] Đã cập nhật tag: " + tagToUpdate.getName() + " với ID: " + tagToUpdate.getId());
            saveData();
        } else {
            throw new IllegalArgumentException("Tag with ID " + tagToUpdate.getId() + " not found for update.");
        }
    }

    public void deleteTag(long tagId) {
        Tag tagToDelete = getTagById(tagId);
        if (tagToDelete == null) {
            System.err.println("[NoteManager deleteTag] Tag với ID " + tagId + " không tìm thấy để xóa.");
            return;
        }
        for (Note note : notes) {
            boolean modified = note.getTags().removeIf(t -> t.getId() == tagId);
            if (modified) {
            }
        }
        tags.remove(tagToDelete);
        System.out.println("[NoteManager deleteTag] Đã xóa tag: " + tagToDelete.getName() + " với ID: " + tagId + " và xóa khỏi tất cả các notes.");
        saveData();
    }

    public List<Note> searchNotes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllNotes();
        }
        String lowerQuery = query.trim().toLowerCase();
        return notes.stream()
                .filter(note -> (note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) ||
                        (note.getContent() != null && note.getContent().toLowerCase().contains(lowerQuery)) ||
                        (note.getTags().stream().anyMatch(tag -> tag.getName().toLowerCase().contains(lowerQuery)))
                )
                .collect(Collectors.toList());
    }

    public List<Note> searchNotesByTag(Tag tag) {
        if (tag == null || tag.getId() == 0) {
            return new ArrayList<>();
        }
        return notes.stream()
                .filter(note -> note.getTags().stream().anyMatch(t -> t.getId() == tag.getId()))
                .collect(Collectors.toList());
    }

    public void moveNoteToFolder(Note note, Folder newFolder) {
        if (note == null || newFolder == null || note.getId() == 0 || newFolder.getId() == 0) {
            throw new IllegalArgumentException("Note or new Folder cannot be null or unsaved.");
        }
        Note noteInManager = getNoteById(note.getId());
        Folder folderInManager = getFolderById(newFolder.getId());

        if (noteInManager == null || folderInManager == null) {
            throw new IllegalArgumentException("Note or Folder not found in manager for move operation.");
        }

        Folder oldFolder = noteInManager.getFolder();

        if (oldFolder != null && oldFolder.getId() == folderInManager.getId()) {
            System.out.println("[NoteManager moveNoteToFolder] Note đã ở trong thư mục đích.");
            return;
        }

        if (oldFolder != null) {
            oldFolder.removeNote(noteInManager);
        }

        noteInManager.setFolder(folderInManager);
        noteInManager.setFolderId(folderInManager.getId());
        if (!folderInManager.getNotes().contains(noteInManager)) {
            folderInManager.addNote(noteInManager);
        }
        System.out.println("[NoteManager moveNoteToFolder] Đã chuyển note '" + noteInManager.getTitle() + "' sang thư mục '" + folderInManager.getName() + "'.");
        saveData();
    }

    public List<Note> getSortedNotes() {
        return notes.stream()
                .sorted(Comparator.comparing(Note::isFavorite, Comparator.reverseOrder())
                        .thenComparing(note -> !note.isMission() || note.isMissionCompleted())
                        .thenComparing(Note::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    void saveData() {
        try {
            dataStorage.save(this);
        } catch (Exception e) {
            System.err.println("Nghiêm trọng: Không thể lưu dữ liệu vào " + "notes.json" + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    List<Note> getModifiableNotesList() { return notes; }
    List<Folder> getModifiableFoldersList() { return folders; }
    List<Tag> getModifiableTagsList() { return tags; }
}
