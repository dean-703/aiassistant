package com.courtney.aiassistant.service;

import com.courtney.aiassistant.exception.ApiException;
import com.courtney.aiassistant.model.AppSettings;
import com.courtney.aiassistant.model.Conversation;
import com.courtney.aiassistant.model.Message;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApiService {
    private static final String RESPONSES_URL = "https://api.openai.com/v1/responses";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient client;

    public ApiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // streaming
                .retryOnConnectionFailure(true)
                .build();
    }

    public interface StreamCallbacks {
        void onStart();
        void onDelta(String token);
        void onCompletion(String finishReason);
        void onError(Throwable t);
    }

    public void streamChatCompletion(Conversation conv, AppSettings settings,
                                     Runnable onStart,
                                     java.util.function.Consumer<String> onDelta,
                                     java.util.function.Consumer<String> onComplete,
                                     java.util.function.Consumer<Throwable> onError) throws ApiException {

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException("OPENAI_API_KEY environment variable is not set.");
        }

        // Build input (same as your previous messages, now under "input")
        List<Map<String, String>> input = new ArrayList<>();
        String sys = (settings != null) ? settings.getSystemPrompt() : null;
        if (sys != null && !sys.isBlank()) {
            input.add(Map.of("role", "system", "content", sys));
        }
        if (conv != null && conv.getMessages() != null) {
            for (Message m : conv.getMessages()) {
                if (m == null || m.getRole() == null || m.getContent() == null) continue;
                input.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        String model = (settings != null && settings.getModel() != null && !settings.getModel().isBlank())
                ? settings.getModel()
                : DEFAULT_MODEL;

        payload.put("model", model);
        payload.put("input", input);
        payload.put("stream", true);

        // ✅ Enable web search and image generation tools
        payload.put("tools", List.of(
                Map.of("type", "web_search")
        ));

        // Map settings (primitives, so no null-checks)
        if (settings != null) {
            int max = settings.getMaxTokens();
            if (max > 0) {
                // Responses uses max_output_tokens (not max_tokens)
                payload.put("max_output_tokens", max);
            }
            payload.put("temperature", settings.getTemperature());
        }

        String json = JsonUtil.toJson(payload);

        Request request = new Request.Builder()
                .url(RESPONSES_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .post(RequestBody.create(json, JSON))
                .build();

        AtomicBoolean finished = new AtomicBoolean(false);  // set true when we’ve completed normally

        EventSources.createFactory(client).newEventSource(request, new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                if (onStart != null) onStart.run();
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                try {
                    // Expect types:
                    // - response.output_text.delta -> { "delta": "..." }
                    // - response.completed         -> { ... }
                    // - response.error             -> { "error": {...} }
                    if (type == null || type.isBlank()) {
                        // Some intermediates may omit the event name. Try best-effort delta extraction.
                        Map<String, Object> root = JsonUtil.fromJson(data);
                        Object deltaMaybe = root.get("delta");
                        if (deltaMaybe != null && onDelta != null) onDelta.accept(String.valueOf(deltaMaybe));
                        return;
                    }

                    switch (type) {

                        case "response.output_tool_calls": {
                            // If the model explicitly calls tools, you can inspect them here.
                            Map<String, Object> root = JsonUtil.fromJson(data);
                            if (onDelta != null) onDelta.accept("[TOOL CALL] " + root.toString());
                            break;
                        }

                        case "response.output_text.delta": {
                            Map<String, Object> root = JsonUtil.fromJson(data);
                            Object delta = root.get("delta");
                            if (delta != null && onDelta != null) onDelta.accept(String.valueOf(delta));
                            break;
                        }
                        case "response.completed": {
                            finished.set(true);
                            if (onComplete != null) onComplete.accept("stop");
                            // We can either let the server close the stream naturally or cancel.
                            // Cancel tends to trigger a benign "stream was reset" from OkHttp; we'll ignore it via 'finished'.
                            eventSource.cancel();
                            break;
                        }
                        case "response.error": {
                            // Forward the error
                            if (onError != null) onError.accept(new RuntimeException("Stream error: " + data));
                            eventSource.cancel();
                            break;
                        }
                        default:
                            // Ignore other events unless you use tools/reasoning
                            break;
                    }
                } catch (Exception ex) {
                    if (onError != null) onError.accept(ex);
                    eventSource.cancel();
                }
            }

            @Override
            public void onClosed(EventSource eventSource) {
                // No-op
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                // If we already finished normally, ignore benign close/reset errors.
                if (finished.get() && isBenignClose(t, response)) {
                    return;
                }
                // Also ignore common cancellation/close messages even if finished state got missed.
                if (isBenignClose(t, response)) {
                    return;
                }

                String body = null;
                try {
                    if (response != null && response.body() != null) {
                        body = response.body().string();
                    }
                } catch (Exception ignore) { }

                if (onError != null) {
                    String msg = (t != null ? t.getMessage() : "unknown failure");
                    onError.accept(new IOException("SSE failure: " + msg + (body != null ? " | " + body : ""), t));
                }
            }
        });
    }

    private boolean isBenignClose(Throwable t, Response response) {
        // These are common when the server or we cancel/close a finished HTTP/2 stream.
        String msg = (t != null && t.getMessage() != null) ? t.getMessage().toLowerCase() : "";
        if (msg.contains("stream was reset") || msg.contains("rst_stream") || msg.contains("canceled")
                || msg.contains("cancelled") || msg.contains("socket closed") || msg.contains("forcibly closed")) {
            return true;
        }
        // If server responded 200 OK and then closed, treat as benign when there's no body detail.
        if (response != null && response.code() == 200) {
            return true;
        }
        return false;
    }

    // Minimal JSON util using Jackson
    static class JsonUtil {
        private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
                new com.fasterxml.jackson.databind.ObjectMapper();

        static String toJson(Object o) {
            try { return MAPPER.writeValueAsString(o); }
            catch (Exception e) { throw new RuntimeException(e); }
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> fromJson(String json) {
            try { return MAPPER.readValue(json, Map.class); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
    }
}
