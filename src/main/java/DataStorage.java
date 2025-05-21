import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;


public class DataStorage {
    private final Gson gson;
    private final File file;

    public DataStorage(String filePath) {
        this.file = new File(filePath);
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        builder.registerTypeAdapter(Note.class, new NoteAdapter());
        builder.registerTypeAdapter(Folder.class, new FolderAdapter());
        builder.registerTypeAdapter(Tag.class, new TagAdapter());
        this.gson = builder.setPrettyPrinting().create();
    }

    public void save(NoteManager noteManager) {
        try (Writer writer = new FileWriter(file)) {
            Data data = new Data();
            data.notes = new ArrayList<>(noteManager.getAllNotes());
            data.folders = new ArrayList<>(noteManager.getAllFolders());
            data.tags = new ArrayList<>(new HashSet<>(noteManager.getAllTags()));

            System.out.println("Đang lưu dữ liệu vào " + file.getName() + ": " +
                    data.notes.size() + " notes, " +
                    data.folders.size() + " folders, " +
                    data.tags.size() + " tags.");
            gson.toJson(data, writer);
            System.out.println("Lưu dữ liệu thành công.");
        } catch (IOException e) {
            System.err.println("Lỗi nghiêm trọng: Không thể lưu dữ liệu vào file " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void load(NoteManager noteManager) {
        if (!file.exists() || file.length() == 0) {
            System.out.println("File " + file.getName() + " không tồn tại hoặc rỗng. Bỏ qua việc tải, NoteManager sẽ dùng dữ liệu mặc định.");
            return;
        }

        try (Reader reader = new FileReader(file)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            if (!jsonElement.isJsonObject()) {
                System.err.println("Lỗi: Định dạng JSON trong " + file.getName() + " không hợp lệ. Sử dụng dữ liệu mặc định.");
                handleCorruptedFile();
                return;
            }

            Data data = gson.fromJson(jsonElement, Data.class);

            if (data != null) {
                noteManager.getModifiableNotesList().clear();
                noteManager.getModifiableFoldersList().clear();
                noteManager.getModifiableTagsList().clear();

                if (data.folders != null) {
                    noteManager.getModifiableFoldersList().addAll(data.folders);
                }
                if (data.tags != null) {
                    noteManager.getModifiableTagsList().addAll(data.tags);
                }
                if (data.notes != null) {
                    noteManager.getModifiableNotesList().addAll(data.notes);
                }

                System.out.println("Đã tải dữ liệu từ " + file.getName() + ": " +
                        (data.notes != null ? data.notes.size() : 0) + " notes, " +
                        (data.folders != null ? data.folders.size() : 0) + " folders, " +
                        (data.tags != null ? data.tags.size() : 0) + " tags.");
            } else {
                System.err.println("Lỗi: Không thể deserialize dữ liệu từ " + file.getName() + ". File có thể bị hỏng. Sử dụng dữ liệu mặc định.");
                handleCorruptedFile();
            }

        } catch (IOException e) {
            System.err.println("Lỗi I/O khi tải dữ liệu từ " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            handleCorruptedFile();
        } catch (JsonSyntaxException e) {
            System.err.println("Lỗi cú pháp JSON khi tải dữ liệu từ " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            handleCorruptedFile();
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi tải dữ liệu từ " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            handleCorruptedFile();
        }
    }

    private void handleCorruptedFile() {
        System.out.println("Đang cố gắng tạo file dữ liệu mới do file cũ bị lỗi hoặc không tồn tại.");
    }

    private static class Data {
        List<Note> notes = new ArrayList<>();
        List<Folder> folders = new ArrayList<>();
        List<Tag> tags = new ArrayList<>();
    }

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? null : new JsonPrimitive(src.format(FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null ? null : LocalDateTime.parse(json.getAsString(), FORMATTER);
        }
    }

    private static class NoteAdapter implements JsonSerializer<Note>, JsonDeserializer<Note> {
        @Override
        public JsonElement serialize(Note src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("id", src.getId());
            json.addProperty("title", src.getTitle());
            json.addProperty("content", src.getContent());
            json.add("createdAt", context.serialize(src.getCreatedAt()));
            json.add("updatedAt", context.serialize(src.getUpdatedAt()));
            json.addProperty("isFavorite", src.isFavorite());
            json.addProperty("isMission", src.isMission());
            json.addProperty("isMissionCompleted", src.isMissionCompleted());
            json.addProperty("missionContent", src.getMissionContent());
            json.addProperty("folderId", src.getFolderId());

            if (src.getTags() != null) {
                json.add("tags", context.serialize(src.getTags()));
            }

            if (src.getAlarm() != null) {
                json.add("alarm", context.serialize(src.getAlarm()));
            } else {
                json.add("alarm", JsonNull.INSTANCE);
            }
            return json;
        }

        @Override
        public Note deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();

            long id = obj.has("id") ? obj.get("id").getAsLong() : 0;
            String title = obj.has("title") ? obj.get("title").getAsString() : "Untitled"; // Default title
            String content = obj.has("content") ? obj.get("content").getAsString() : ""; // Default content

            LocalDateTime createdAt = null;
            if (obj.has("createdAt") && !obj.get("createdAt").isJsonNull()) {
                createdAt = context.deserialize(obj.get("createdAt"), LocalDateTime.class);
            } else {
                createdAt = LocalDateTime.now(); // Default if missing
            }

            LocalDateTime updatedAt = null;
            if (obj.has("updatedAt") && !obj.get("updatedAt").isJsonNull()) {
                updatedAt = context.deserialize(obj.get("updatedAt"), LocalDateTime.class);
            } else {
                updatedAt = createdAt; // Default to createdAt if missing
            }

            // SỬA LỖI NullPointerException ở đây
            boolean isFavorite = false;
            if (obj.has("isFavorite") && !obj.get("isFavorite").isJsonNull()) {
                isFavorite = obj.get("isFavorite").getAsBoolean();
            }

            Note note = new Note(title, content, isFavorite);
            note.setId(id);
            note.setCreatedAt(createdAt);
            note.setUpdatedAt(updatedAt);

            boolean isMission = false;
            if (obj.has("isMission") && !obj.get("isMission").isJsonNull()) {
                isMission = obj.get("isMission").getAsBoolean();
            }
            note.setMission(isMission);

            boolean isMissionCompleted = false;
            if (obj.has("isMissionCompleted") && !obj.get("isMissionCompleted").isJsonNull()) {
                isMissionCompleted = obj.get("isMissionCompleted").getAsBoolean();
            }
            note.setMissionCompleted(isMissionCompleted);

            if (obj.has("missionContent") && !obj.get("missionContent").isJsonNull()) {
                note.setMissionContent(obj.get("missionContent").getAsString());
            } else {
                note.setMissionContent(""); // Default empty string
            }

            if (obj.has("folderId") && !obj.get("folderId").isJsonNull()) {
                note.setFolderId(obj.get("folderId").getAsLong());
            } else {
                note.setFolderId(0); // Default or indicate no specific folder
            }

            if (obj.has("tags") && !obj.get("tags").isJsonNull()) {
                Type listType = new TypeToken<ArrayList<Tag>>() {}.getType();
                List<Tag> tags = context.deserialize(obj.get("tags"), listType);
                note.setTags(tags);
            } else {
                note.setTags(new ArrayList<>()); // Default empty list
            }

            if (obj.has("alarm") && !obj.get("alarm").isJsonNull()) {
                Alarm alarm = context.deserialize(obj.get("alarm"), Alarm.class);
                note.setAlarm(alarm);
                if (alarm != null) {
                    note.setAlarmId(alarm.getId());
                }
            }
            return note;
        }
    }

    private static class FolderAdapter implements JsonSerializer<Folder>, JsonDeserializer<Folder> {
        @Override
        public JsonElement serialize(Folder src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("id", src.getId());
            json.addProperty("name", src.getName());
            json.addProperty("isFavorite", src.isFavorite());

            if (src.getSubFolderNames() != null && !src.getSubFolderNames().isEmpty()) {
                json.add("subFolderNames", context.serialize(src.getSubFolderNames()));
            }
            return json;
        }

        @Override
        public Folder deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            long id = obj.has("id") ? obj.get("id").getAsLong() : 0;
            String name = obj.has("name") ? obj.get("name").getAsString() : "Unnamed Folder"; // Default name

            Folder folder = new Folder(name); // Use constructor that doesn't require ID initially
            folder.setId(id);

            if (obj.has("isFavorite") && !obj.get("isFavorite").isJsonNull()) {
                folder.setFavorite(obj.get("isFavorite").getAsBoolean());
            } else {
                folder.setFavorite(false); // Default
            }

            if (obj.has("subFolderNames") && !obj.get("subFolderNames").isJsonNull()) {
                Type listType = new TypeToken<ArrayList<String>>() {}.getType();
                List<String> subFolderNames = context.deserialize(obj.get("subFolderNames"), listType);
                folder.setSubFolderNames(subFolderNames);
            } else {
                folder.setSubFolderNames(new ArrayList<>()); // Default
            }
            return folder;
        }
    }

    private static class TagAdapter implements JsonSerializer<Tag>, JsonDeserializer<Tag> {
        @Override
        public JsonElement serialize(Tag src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("id", src.getId());
            json.addProperty("name", src.getName());
            return json;
        }

        @Override
        public Tag deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            long id = obj.has("id") ? obj.get("id").getAsLong() : 0;
            String name = obj.has("name") ? obj.get("name").getAsString() : "Unnamed Tag"; // Default name

            Tag tag = new Tag(name); // Use constructor that doesn't require ID initially
            tag.setId(id);
            return tag;
        }
    }
}
