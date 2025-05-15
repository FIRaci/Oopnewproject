import javax.swing.SwingWorker;
import org.json.JSONObject; // Bạn cần thư viện org.json. Thêm dependency nếu dùng Maven/Gradle, hoặc add JAR.
// Ví dụ Maven:
// <dependency>
//     <groupId>org.json</groupId>
//     <artifactId>json</artifactId>
//     <version>20231013</version>
// </dependency>

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TranslationService {

    private static final String API_URL = "http://203.113.132.109:11434/api/generate";
    private static final String MODEL = "llama3.1"; // Hoặc mô hình bạn muốn sử dụng

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onError(String errorMessage);
    }

    public static void translate(final String textToTranslate, final String targetLanguage, final TranslationCallback callback) {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Xây dựng prompt tương tự như trong TranslationApp
                String prompt = "translate to " + targetLanguage + ": " + textToTranslate;
                return sendPostRequest(API_URL, MODEL, prompt);
            }

            @Override
            protected void done() {
                try {
                    String rawResponse = get(); // Lấy kết quả từ doInBackground
                    // Xử lý response để trích xuất văn bản dịch
                    StringBuilder finalResponse = new StringBuilder();
                    String[] subResponses = rawResponse.trim().split("\n");
                    for (String subResponse : subResponses) {
                        if (subResponse.trim().isEmpty()) continue;
                        try {
                            JSONObject jsonObject = new JSONObject(subResponse);
                            if (jsonObject.has("response")) {
                                finalResponse.append(jsonObject.getString("response"));
                            }
                        } catch (org.json.JSONException e) {
                            System.err.println("Lỗi khi phân tích JSON: " + subResponse + " - " + e.getMessage());
                            // Quyết định cách xử lý lỗi này, ví dụ: throw exception để bắt ở catch dưới
                            throw new IOException("Lỗi phân tích phản hồi từ máy chủ: " + e.getMessage(), e);
                        }
                    }
                    callback.onSuccess(finalResponse.toString());
                } catch (Exception e) {
                    // e.printStackTrace(); // In lỗi ra console để debug
                    String errorMessage = e.getMessage();
                    if (e.getCause() != null) {
                        errorMessage += " (Cause: " + e.getCause().getMessage() + ")";
                    }
                    callback.onError("Lỗi dịch: " + errorMessage);
                }
            }
        };
        worker.execute();
    }

    private static String sendPostRequest(String url, String model, String prompt) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setConnectTimeout(15000); // 15 giây timeout kết nối
        con.setReadTimeout(30000);    // 30 giây timeout đọc

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("model", model);
        jsonInputString.put("prompt", prompt);
        // Thêm "stream": false để nhận toàn bộ phản hồi một lần, thay vì từng phần.
        // API của bạn có thể hỗ trợ hoặc không hỗ trợ điều này.
        // Nếu API trả về từng dòng JSON (stream), thì logic hiện tại là đúng.
        // Nếu bạn muốn nhận 1 JSON object lớn duy nhất, bạn có thể cần "stream": false
        // jsonInputString.put("stream", false);

        con.setDoOutput(true);
        try (DataOutputStream os = new DataOutputStream(con.getOutputStream())) {
            byte[] input = jsonInputString.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        int responseCode = con.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }
        } else { // error
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }
            throw new IOException("Lỗi HTTP: " + responseCode + " - " + response.toString());
        }

        con.disconnect();
        return response.toString();
    }
}
