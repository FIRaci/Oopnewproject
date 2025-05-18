import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object interface for Tag entities.
 */
public interface TagDAO {
    /**
     * Adds a new tag to the database.
     * @param tag The tag to add.
     * @return The generated ID of the new tag.
     * @throws SQLException If a database error occurs.
     */
    long addTag(Tag tag) throws SQLException;

    /**
     * Retrieves a tag by its ID.
     * @param id_tag The ID of the tag.
     * @return The Tag object, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    Tag getTagById(long id_tag) throws SQLException;

    /**
     * Retrieves a tag by its name.
     * @param name The name of the tag.
     * @return The Tag object, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    Tag getTagByName(String name) throws SQLException;

    /**
     * Retrieves all tags from the database.
     * @return A list of all tags.
     * @throws SQLException If a database error occurs.
     */
    List<Tag> getAllTags() throws SQLException;

    /**
     * Updates an existing tag.
     * @param id_tag The ID of the tag to update.
     * @param tag The tag with updated data.
     * @throws SQLException If a database error occurs.
     */
    void updateTag(long id_tag, Tag tag) throws SQLException;

    /**
     * Deletes a tag by its ID.
     * @param id_tag The ID of the tag to delete.
     * @throws SQLException If a database error occurs.
     */
    void deleteTag(long id_tag) throws SQLException;

    /**
     * Retrieves the ID of a tag by its name.
     * @param name The name of the tag.
     * @return The ID of the tag, or -1 if not found.
     * @throws SQLException If a database error occurs.
     */
    long getTagIdByName(String name) throws SQLException;

    Tag findByName(String trim);
}