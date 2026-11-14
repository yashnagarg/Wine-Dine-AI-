package com.winedine.llm;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.winedine.data.ConstraintsJson;
import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter: makes the OpenAI chat-completions HTTP API look like {@link LLMClient}.
 * The API key is passed in from {@code AppConfig} (read from the local .env file) and is never printed.
 */
public class OpenAILLMClient implements LLMClient {
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    /** Sends a JSON request body and returns the JSON response body. Replaceable so tests need no network. */
    public interface Sender {
        String send(String jsonBody);
    }

    private static final String FIELDS = "Reply with ONLY a JSON object with these keys: "
            + "groupSize (integer), maxBudgetPerPerson (number, CAD), maxDrinkPrice (number, CAD), "
            + "day (MONDAY..SUNDAY), startTime and endTime (24-hour \"HH:mm\"), location (text), "
            + "maxDistanceKm (number), foodPreferences, drinkPreferences, venuePreferences (arrays of short lowercase words), "
            + "happyHourRequired (true/false), rankingProfile (BALANCED, BUDGET_FIRST or DISTANCE_FIRST). "
            + "Use null for anything the user did not say. Never guess or invent values.";

    private final String model;
    private final Sender sender;
    private final Clock clock;
    private final Gson gson = new Gson();

    public OpenAILLMClient(String apiKey, String model) {
        this(model, httpSender(apiKey), Clock.systemDefaultZone());
    }

    public OpenAILLMClient(String model, Sender sender, Clock clock) {
        this.model = model;
        this.sender = sender;
        this.clock = clock;
    }

    @Override
    public SearchConstraints extractConstraints(String userText) {
        String system = "You extract restaurant happy-hour search constraints for Toronto from the user's message. "
                + today() + " " + FIELDS;
        return parseConstraints(chat(system, userText));
    }

    @Override
    public SearchConstraints interpretRefinement(String userText, SearchConstraints current) {
        String system = "The user is refining a restaurant search. Current constraints: "
                + gson.toJson(ConstraintsJson.from(current)) + ". " + today()
                + " Return ONLY the values the user's new message changes or adds; use null for everything else. "
                + FIELDS;
        return parseConstraints(chat(system, userText));
    }

    @Override
    public String askClarification(List<String> missingFields, SearchConstraints known) {
        String system = "You help find happy-hour deals. Ask the user ONE short, friendly question that asks for "
                + "all of the missing details. Do not recommend anything.";
        String user = "Missing: " + String.join(", ", missingFields) + ". Already known: "
                + gson.toJson(ConstraintsJson.from(known));
        return chat(system, user).trim();
    }

    @Override
    public String explain(SearchResult result, SearchConstraints constraints) {
        String system = "Explain in two or three sentences why this restaurant fits the request. Use ONLY the "
                + "facts below. Do not add prices, hours, addresses or claims that are not listed. "
                + "If warnings are listed, mention them.";
        String user = "Request: " + gson.toJson(ConstraintsJson.from(constraints)) + "\nFacts:\n"
                + result.getMatchSummary();
        return chat(system, user).trim();
    }

    @Override
    public String describe() { return "OpenAI " + model; }

    private String today() {
        LocalDate d = LocalDate.now(clock);
        return "Today is " + d + " (" + d.getDayOfWeek() + ").";
    }

    private SearchConstraints parseConstraints(String reply) {
        try {
            return ConstraintsJson.parse(reply).toConstraints();
        } catch (JsonParseException e) {
            throw new LLMException("The model's answer could not be understood: " + e.getMessage(), e);
        }
    }

    private String chat(String system, String user) {
        ChatRequest request = new ChatRequest();
        request.model = model;
        request.temperature = 0.0;
        request.messages = new ArrayList<>();
        request.messages.add(new Message("system", system));
        request.messages.add(new Message("user", user));
        String body = sender.send(gson.toJson(request));
        try {
            ChatResponse response = gson.fromJson(new StringReader(body), ChatResponse.class);
            if (response == null || response.choices == null || response.choices.isEmpty()
                    || response.choices.get(0).message == null || response.choices.get(0).message.content == null) {
                throw new LLMException("The model returned an empty answer.");
            }
            return response.choices.get(0).message.content;
        } catch (JsonParseException e) {
            throw new LLMException("The model's response could not be read.", e);
        }
    }

    private static Sender httpSender(String apiKey) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return body -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    // Only the status code is reported: never echo headers, which contain the key.
                    throw new LLMException("The language model service returned HTTP " + response.statusCode()
                            + (response.statusCode() == 401 ? " (check LLM_API_KEY in your .env file)." : "."));
                }
                return response.body();
            } catch (IOException e) {
                throw new LLMException("Could not reach the language model service: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LLMException("The request was interrupted.", e);
            }
        };
    }

    // JSON shapes of the chat-completions API (only the parts we use).
    static class ChatRequest {
        String model;
        Double temperature;
        List<Message> messages;
    }

    static class Message {
        String role;
        String content;

        Message() { }

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    static class ChatResponse {
        List<Choice> choices;
    }

    static class Choice {
        Message message;
    }
}
