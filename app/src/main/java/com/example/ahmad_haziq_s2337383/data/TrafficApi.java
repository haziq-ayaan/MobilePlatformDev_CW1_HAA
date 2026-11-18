// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.data;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Downloads a feed from Traffic Scotland and returns the XML as a String.
 * This is the starter code's download, moved into its own class and made more robust:
 * timeouts, a StringBuilder instead of repeated String "+", and proper HTTP error handling.
 *
 * It does blocking network work, so it must only be called from a background thread.
 */
public class TrafficApi {

    /** Thrown when Traffic Scotland rejects the Client ID / Key (HTTP 401 or 403). */
    public static class AuthException extends IOException {
        public AuthException(String message) {
            super(message);
        }
    }

    private static final String TAG = "TrafficApi";
    private final ApiKeys keys;

    public TrafficApi(ApiKeys keys) {
        this.keys = keys;
    }

    public String download(String feedUrl) throws IOException {
        if (!feedUrl.startsWith("https://")) {
            throw new IOException("Feed URL not set: " + feedUrl);
        }
        if (!keys.hasRealKeys()) {
            throw new AuthException("DATEX II Client ID / Key not set in local.properties");
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(feedUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(FeedConfig.CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(FeedConfig.READ_TIMEOUT_MS);

            // Same authorisation as the starter code: Base64 of "clientId:clientKey"
            String credentials = keys.getClientId() + ":" + keys.getClientKey();
            connection.setRequestProperty("Authorization",
                    Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new AuthException("Traffic Scotland refused the request (HTTP " + code
                        + "). Check the Client ID and Key.");
            }
            if (code != HttpURLConnection.HTTP_OK) {
                String detail = readErrorBody(connection);
                Log.e(TAG, "HTTP " + code + " from " + feedUrl + ": " + detail);
                throw new IOException("Traffic Scotland returned HTTP " + code
                        + (detail.isEmpty() ? "" : " – " + detail));
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line).append('\n');
                }
            }
            return result.toString();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Reads the start of the server's error message (if any), to explain a failed request. */
    private String readErrorBody(HttpURLConnection connection) {
        InputStream errorStream = connection.getErrorStream();
        if (errorStream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null && sb.length() < 300) {
                sb.append(line.trim()).append(' ');
            }
        } catch (IOException e) {
            return "";
        }
        // Strip any HTML/XML tags so only the readable text remains
        String text = sb.toString().replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }
}
