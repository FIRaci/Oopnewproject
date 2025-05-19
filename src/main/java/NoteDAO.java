import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object interface for Note entities.
 */
public interface NoteDAO {
    /**
     * Adds a new note to the database.
     * @param note The note to add.
     * @param folderDAO The FolderDAO for folder-related operations.
     * @param tagDAO The TagDAO for tag-related operations.
     * @return The generated ID of the new note.
     * @throws SQLException If a database error occurs.
     */
    long addNote(Note note, FolderDAO folderDAO, TagDAO tagDAO) throws SQLException;

    /**
     * Retrieves a note by its ID.
     * @param noteId The ID of the note.
     * @param folderDAO The FolderDAO for folder-related operations.
     * @param tagDAO The TagDAO for tag-related operations.
     * @return The Note object, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    Note getNoteById(long noteId, FolderDAO folderDAO, TagDAO tagDAO) throws SQLException;

    /**
     * Retrieves all notes from the database.
     * @param folderDAO The FolderDAO for folder-related operations.
     * @param tagDAO The TagDAO for tag-related operations.
     * @return A list of all notes.
     * @throws SQLException If a database error occurs.
     */
    List<Note> getAllNotes(FolderDAO folderDAO, TagDAO tagDAO) throws SQLException;

    /**
     * Updates an existing note.
     * @param noteId The ID of the note to update.
     * @param note The note with updated data.
     * @param folderDAO The FolderDAO for folder-related operations.
     * @param tagDAO The TagDAO for tag-related operations.
     * @throws SQLException If a database error occurs.
     */
    void updateNote(long noteId, Note note, FolderDAO folderDAO, TagDAO tagDAO) throws SQLException;

    /**
     * Deletes a note by its ID.
     * @param noteId The ID of the note to delete.
     * @throws SQLException If a database error occurs.
     */
    void deleteNote(long noteId) throws SQLException;
}