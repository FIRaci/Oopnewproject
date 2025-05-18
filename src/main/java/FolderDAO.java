import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object interface for Folder entities.
 */
public interface FolderDAO {
    /**
     * Adds a new folder to the database.
     * @param folder The folder to add.
     * @return The generated ID of the new folder.
     * @throws SQLException If a database error occurs.
     */
    long addFolder(Folder folder) throws SQLException;

    /**
     * Retrieves a folder by its ID.
     * @param id_folder The ID of the folder.
     * @return The Folder object, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    Folder getFolderById(long id_folder) throws SQLException;

    /**
     * Retrieves a folder by its name.
     * @param name The name of the folder.
     * @return The Folder object, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    Folder getFolderByName(String name) throws SQLException;

    /**
     * Retrieves all folders from the database.
     * @return A list of all folders.
     * @throws SQLException If a database error occurs.
     */
    List<Folder> getAllFolders() throws SQLException;

    /**
     * Updates an existing folder.
     * @param id_folder The ID of the folder to update.
     * @param folder The folder with updated data.
     * @throws SQLException If a database error occurs.
     */
    void updateFolder(long id_folder, Folder folder) throws SQLException;

    /**
     * Deletes a folder by its ID.
     * @param id_folder The ID of the folder to delete.
     * @throws SQLException If a database error occurs.
     */
    void deleteFolder(long id_folder) throws SQLException;

    /**
     * Checks if a folder exists by its ID.
     * @param id_folder The ID of the folder.
     * @return true if the folder exists, false otherwise.
     * @throws SQLException If a database error occurs.
     */
    boolean folderExists(long id_folder) throws SQLException;

    /**
     * Retrieves the ID of a folder by its name.
     * @param name The name of the folder.
     * @return The ID of the folder, or -1 if not found.
     * @throws SQLException If a database error occurs.
     */
    long getFolderIdByName(String name) throws SQLException;
}