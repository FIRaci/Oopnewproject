import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NoteController {
    private List<Note> notes;
    private String currentTheme;

    public NoteController() {
        this.notes = new ArrayList<>();
        this.currentTheme = "Light"; // default
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void addNote(Note note) {
        notes.add(note);
    }

    public void deleteNote(Note note) {
        notes.remove(note);
    }

    public void editNote(Note oldNote, Note newNote) {
        int index = notes.indexOf(oldNote);
        if (index != -1) {
            notes.set(index, newNote);
        }
    }

    public void markFavorite(Note note) {
        note.setFavorite(!note.isFavorite()); // Toggle trạng thái yêu thích
    }

    public void renameNote(Note note, String newTitle) {
        note.setTitle(newTitle);
    }

    public void sortNotes(Comparator<Note> comparator) {
        notes.sort(comparator);
    }

    public void moveNote(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= notes.size() || toIndex < 0 || toIndex > notes.size()) return;
        Note temp = notes.remove(fromIndex);
        notes.add(toIndex, temp);
    }

    public void changeTheme(String themeName) {
        // Lưu tên theme, phần giao diện sẽ apply riêng
        this.currentTheme = themeName;
    }

    public String getCurrentTheme() {
        return currentTheme;
    }
}
