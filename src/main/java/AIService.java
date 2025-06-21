import javax.swing.SwingWorker;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class AIService {

    private static final String API_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma3:4b";

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onError(String errorMessage);
    }

    public interface SummarizationCallback {
        void onSuccess(String summarizedText);
        void onError(String errorMessage);
    }


    public interface TagGenerationCallback {
        void onSuccess(String generatedTagsString);
        void onError(String errorMessage);
    }

    public static void translate(final String textToTranslate, final String targetLanguage, final TranslationCallback callback) {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String prompt = "translate to " + targetLanguage + " without any comment this text: " + textToTranslate;
                return sendPostRequest(API_URL, MODEL, prompt);
            }

            @Override
            protected void done() {
                try {
                    String rawResponse = get();
                    String processedResponse = processApiResponse(rawResponse);
                    callback.onSuccess(processedResponse);
                } catch (Exception e) {
                    handleException(e, "Lỗi dịch: ", callback::onError);
                }
            }
        };
        worker.execute();
    }

    public static void summarize(final String textToSummarize, final SummarizationCallback callback) {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String prompt = "summarize this text concisely without any comment: " + textToSummarize;
                return sendPostRequest(API_URL, MODEL, prompt);
            }

            @Override
            protected void done() {
                try {
                    String rawResponse = get();
                    String processedResponse = processApiResponse(rawResponse);
                    callback.onSuccess(processedResponse);
                } catch (Exception e) {
                    handleException(e, "Lỗi tóm tắt: ", callback::onError);
                }
            }
        };
        worker.execute();
    }


    public static void generateTags(final String textToAnalyze, final TagGenerationCallback callback) {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {

                String prompt = "Identify the core actions or most critical topics in the following text. " +
                        "Represent these as a few concise, comma-separated tags. " +
                        "Output ONLY the tags (e.g., task-delegation, market-research, urgent-deadline), " +
                        "without any other text, comments, or explanations. " +
                        "Aim for the smallest number of tags that accurately capture the essence. Text:\n\n" + textToAnalyze;
                return sendPostRequest(API_URL, MODEL, prompt);
            }

            @Override
            protected void done() {
                try {
                    String rawResponse = get();
                    String processedResponse = processApiResponse(rawResponse);
                    callback.onSuccess(processedResponse.trim());
                } catch (Exception e) {
                    handleException(e, "Lỗi tạo tag AI: ", callback::onError);
                }
            }
        };
        worker.execute();
    }



    private static String processApiResponse(String rawJsonResponse) throws IOException {
        try {
            JSONObject jsonObject = new JSONObject(rawJsonResponse);
            if (jsonObject.has("response")) {
                return jsonObject.getString("response");
            } else if (jsonObject.has("error")) {
                throw new IOException("Lỗi từ API: " + jsonObject.getString("error"));
            } else {

                System.err.println("Phản hồi không hợp lệ từ API (thiếu 'response' hoặc 'error'): " + rawJsonResponse);
                throw new IOException("Phản hồi không hợp lệ từ API. Chi tiết: " + rawJsonResponse.substring(0, Math.min(rawJsonResponse.length(), 100)));
            }
        } catch (org.json.JSONException e) {
            System.err.println("Lỗi khi phân tích JSON: " + rawJsonResponse + " - " + e.getMessage());
            throw new IOException("Lỗi phân tích phản hồi từ máy chủ: " + e.getMessage(), e);
        }
    }

    private static void handleException(Exception e, String errorPrefix, Consumer<String> onErrorCallback) {

        String errorMessage = e.getMessage();
        if (e.getCause() != null) {
            errorMessage = e.getCause().getMessage();
        }

        if (e instanceof java.util.concurrent.ExecutionException && e.getCause() instanceof IOException) {
            errorMessage = e.getCause().getMessage();
        } else if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            errorMessage = "Tác vụ đã bị gián đoạn.";
        } else if (errorMessage == null && e.getCause() != null) {
            errorMessage = e.getCause().toString();
        } else if (errorMessage == null) {
            errorMessage = e.toString();
        }

        onErrorCallback.accept(errorPrefix + errorMessage);
    }

    private static String sendPostRequest(String url, String model, String prompt) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setConnectTimeout(15000);
        con.setReadTimeout(60000);

        JSONObject jsonInputString = new JSONObject();
        jsonInputString.put("model", model);
        jsonInputString.put("prompt", prompt);
        jsonInputString.put("stream", false);

        con.setDoOutput(true);
        try (DataOutputStream os = new DataOutputStream(con.getOutputStream())) {
            byte[] input = jsonInputString.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        int responseCode = con.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
        } else {
            StringBuilder errorResponse = new StringBuilder();
            if (con.getErrorStream() != null) {
                try (BufferedReader brError = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = brError.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
            } else {
                errorResponse.append("Không có nội dung lỗi từ server.");
            }
            throw new IOException("Lỗi HTTP: " + responseCode + ". Phản hồi từ server: " + errorResponse.toString().trim());
        }

        con.disconnect();
        return response.toString();
    }
}