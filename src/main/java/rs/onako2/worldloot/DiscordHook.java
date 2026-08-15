package rs.onako2.worldloot;

import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static rs.onako2.worldloot.WorldLoot.LOGGER;

// IDK. where I have this class from or when I made it. Was for my Anti Cheat a long time ago...
public class DiscordHook {

    private DiscordHook() {}

    public static void sendMessage(final String message, @Nullable String webhookURL) {
        if (webhookURL == null || (!webhookURL.contains("http"))) {
            LOGGER.info("No webhook URL provided. Skipping webhook.");
            return;
        }
        try {
            final HttpsURLConnection connection = (HttpsURLConnection) new URI(webhookURL).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "WorldLoot (+https://github.com/Onako2/WorldLoot)");
            connection.setDoOutput(true);
            try (final OutputStream outputStream = connection.getOutputStream()) {
                // Handle backslashes.
                String preparedMessage = message.replaceAll("\\\\", "");
                if (preparedMessage.endsWith(" *")) {
                    LOGGER.info("Command ends with a space, removing it.");
                    preparedMessage = preparedMessage.substring(0, preparedMessage.length() - 2) + "*";
                }
                // Create JSON payload
                JsonObject jsonPayload = new JsonObject();
                jsonPayload.addProperty("content", preparedMessage);

                outputStream.write(jsonPayload.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                LOGGER.error("Lol, some exception", e);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200 && responseCode != 204) {
                String responseMessage = readResponse(connection);
                LOGGER.error("Failed to send command to Discord. Response code: {}, Response message: {}", responseCode, responseMessage);
            } else {
                connection.getInputStream();
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't connect to Discord's servers or webhookURL is invalid!");
        }
    }

    private static String readResponse(HttpsURLConnection connection) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            return content.toString();
        } catch (Exception e) {
            return "<NO RESPONSE>";
        }
    }
}