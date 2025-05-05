import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
            data.notes = noteManager.getAllNotes();
            data.folders = noteManager.getAllFolders();
            data.tags = new ArrayList<>(new HashSet<>(noteManager.getAllTags())); // Loại bỏ tag trùng lặp
            System.out.println("Saving data to notes.json: " + data.notes.size() + " notes, " + data.folders.size() + " folders, " + data.tags.size() + " tags");
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save data: " + e.getMessage(), e);
        }
    }

    public void load(NoteManager noteManager) {
        if (!file.exists()) {
            System.out.println("No notes.json file found, using default data.");
            return;
        }
        try (Reader reader = new FileReader(file)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            if (!jsonElement.isJsonObject()) {
                System.err.println("Invalid JSON format in notes.json, using default data.");
                return;
            }
            Data data = gson.fromJson(jsonElement, Data.class);
            if (data != null) {
                System.out.println("Loaded data from notes.json: " + data.notes.size() + " notes, " + data.folders.size() + " folders, " + data.tags.size() + " tags");
                noteManager.getAllNotes().clear();
                noteManager.getAllFolders().clear();
                noteManager.getAllTags().clear();
                noteManager.getAllFolders().addAll(data.folders);
                noteManager.getAllTags().addAll(data.tags);
                Map<String, Folder> folderMap = new HashMap<>();
                for (Folder folder : data.folders) {
                    folderMap.put(folder.getName(), folder);
                }
                Map<String, Tag> tagMap = new HashMap<>();
                for (Tag tag : data.tags) {
                    tagMap.put(tag.getName(), tag);
                }
                for (Note note : data.notes) {
                    if (note.folderName != null) {
                        Folder folder = folderMap.get(note.folderName);
                        if (folder != null) {
                            note.setFolder(folder);
                            folder.addNote(note);
                        } else {
                            note.setFolder(noteManager.getRootFolder());
                            noteManager.getRootFolder().addNote(note);
                        }
                    } else {
                        note.setFolder(noteManager.getRootFolder());
                        noteManager.getRootFolder().addNote(note);
                    }
                    if (note.tagNames != null) {
                        note.tagNames.forEach(tagName -> {
                            Tag tag = tagMap.get(tagName);
                            if (tag != null) {
                                note.addTag(tag);
                            }
                        });
                    }
                    noteManager.getAllNotes().add(note);
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Failed to load data from notes.json: " + e.getMessage());
            System.err.println("Using default data instead.");
            if (file.exists()) {
                System.out.println("Deleting invalid notes.json file.");
                file.delete();
            }
        }
    }

    private static class Data {
        List<Note> notes = new ArrayList<>();
        List<Folder> folders = new ArrayList<>();
        List<Tag> tags = new ArrayList<>();
    }

    private static class NoteAdapter implements JsonSerializer<Note>, JsonDeserializer<Note> {
        @Override
        public JsonElement serialize(Note src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("title", src.getTitle());
            json.addProperty("content", src.getContent());
            json.addProperty("favorite", src.isFavorite());
            json.addProperty("mission", src.isMission());
            json.addProperty("missionCompleted", src.isMissionCompleted());
            json.addProperty("missionContent", src.getMissionContent());
            json.add("creationDate", context.serialize(src.getCreationDate()));
            json.add("modificationDate", context.serialize(src.getModificationDate()));
            json.add("alarm", context.serialize(src.getAlarm()));
            if (src.getFolder() != null) {
                json.addProperty("folderName", src.getFolder().getName());
            }
            JsonArray tagNames = new JsonArray();
            src.getTags().forEach(tag -> tagNames.add(tag.getName()));
            json.add("tagNames", tagNames);
            return json;
        }

        @Override
        public Note deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String title = obj.get("title").getAsString();
            String content = obj.get("content").getAsString();
            boolean favorite = obj.get("favorite").getAsBoolean();
            Note note = new Note(title, content, favorite);
            note.setMission(obj.get("mission").getAsBoolean());
            note.setMissionCompleted(obj.get("missionCompleted").getAsBoolean());
            if (obj.has("missionContent")) {
                note.setMissionContent(obj.get("missionContent").getAsString());
            }
            note.setCreationDate(context.deserialize(obj.get("creationDate"), LocalDateTime.class));
            note.setModificationDate(context.deserialize(obj.get("modificationDate"), LocalDateTime.class));
            if (obj.has("alarm") && !obj.get("alarm").isJsonNull()) {
                note.setAlarm(context.deserialize(obj.get("alarm"), Alarm.class));
            }
            if (obj.has("folderName")) {
                note.folderName = obj.get("folderName").getAsString();
            }
            if (obj.has("tagNames")) {
                JsonArray tagNames = obj.get("tagNames").getAsJsonArray();
                note.tagNames = new ArrayList<>();
                tagNames.forEach(tagName -> note.tagNames.add(tagName.getAsString()));
            }
            return note;
        }
    }

    private static class FolderAdapter implements JsonSerializer<Folder>, JsonDeserializer<Folder> {
        @Override
        public JsonElement serialize(Folder src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("name", src.getName());
            JsonArray subFolders = new JsonArray();
            src.getSubFolders().forEach(subFolder -> subFolders.add(subFolder.getName()));
            json.add("subFolders", subFolders);
            json.addProperty("favorite", src.isFavorite());
            return json;
        }

        @Override
        public Folder deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String name = obj.get("name").getAsString();
            Folder folder = new Folder(name);
            if (obj.has("subFolders")) {
                JsonArray subFolders = obj.get("subFolders").getAsJsonArray();
                subFolders.forEach(subFolder -> folder.subFolderNames.add(subFolder.getAsString()));
            }
            if (obj.has("favorite")) {
                folder.setFavorite(obj.get("favorite").getAsBoolean());
            }
            return folder;
        }
    }

    private static class TagAdapter implements JsonSerializer<Tag>, JsonDeserializer<Tag> {
        @Override
        public JsonElement serialize(Tag src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("name", src.getName());
            return json;
        }

        @Override
        public Tag deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            return new Tag(obj.get("name").getAsString());
        }
    }

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), FORMATTER);
        }
    }
}