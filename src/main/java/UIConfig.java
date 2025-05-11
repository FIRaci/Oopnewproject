import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONTokener;

public class UIConfig {
    private final JSONObject config;

    public UIConfig() {
        try (InputStream is = getClass().getResourceAsStream("/config/ui_config.json")) {
            if (is == null) throw new IllegalStateException("UI config file not found");
            JSONTokener tokener = new JSONTokener(new InputStreamReader(is, StandardCharsets.UTF_8));
            config = new JSONObject(tokener);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load UI config: " + e.getMessage());
        }
    }

    public Color getColor(String theme, String key) {
        String colorHex = config.getJSONObject("theme").getJSONObject(theme).getString(key);
        return Color.decode(colorHex);
    }

    public Font getFont(String key) {
        JSONObject fontObj = config.getJSONObject("fonts").getJSONObject(key);
        String family = fontObj.getString("family");
        int size = fontObj.getInt("size");
        String styleStr = fontObj.getString("style");
        int style = styleStr.equalsIgnoreCase("bold") ? Font.BOLD :
                styleStr.equalsIgnoreCase("italic") ? Font.ITALIC : Font.PLAIN;
        return new Font(family, style, size);
    }

    public int getComponentInt(String component, String key) {
        return config.getJSONObject("components").getJSONObject(component).getInt(key);
    }

    public int[] getTableColumnWidths() {
        return config.getJSONObject("components")
                .getJSONObject("table")
                .getJSONArray("columnWidths")
                .toList()
                .stream()
                .mapToInt(obj -> (int) obj)
                .toArray();
    }

    public String getIconPath(String key) {
        return config.getJSONObject("icons").getString(key);
    }

    public Insets getButtonPadding() {
        JSONObject padding = config.getJSONObject("components")
                .getJSONObject("button")
                .getJSONObject("padding");
        return new Insets(
                padding.getInt("top"),
                padding.getInt("left"),
                padding.getInt("bottom"),
                padding.getInt("right")
        );
    }
}
