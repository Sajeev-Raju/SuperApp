package com.example.NearMeBKND.Business.Util;

import java.net.HttpURLConnection;
import java.net.URL;

public class GoogleMapsUtil {
    /**
     * Extracts latitude and longitude from a Google Maps URL of the form:
     * https://www.google.com/maps/place/.../@12.9715987,77.5945627,17z
     * Returns a Double array: [latitude, longitude], or null if not found.
     */
    public static Double[] extractLatLong(String url) {
        if (url == null) return null;

        // If it's a short link, resolve it
        if (url.contains("maps.app.goo.gl")) {
            url = resolveShortLink(url);
        }

        // Handle @lat,lng
        int atIdx = url.indexOf("@");
        if (atIdx != -1) {
            String coords = url.substring(atIdx + 1);
            String[] parts = coords.split(",");
            if (parts.length >= 2) {
                try {
                    double lat = Double.parseDouble(parts[0]);
                    double lng = Double.parseDouble(parts[1]);
                    return new Double[]{lat, lng};
                } catch (NumberFormatException ignored) {}
            }
        }

        // Handle ?q=lat,lng
        int qIdx = url.indexOf("?q=");
        if (qIdx != -1) {
            String coords = url.substring(qIdx + 3);
            String[] parts = coords.split(",");
            if (parts.length >= 2) {
                try {
                    double lat = Double.parseDouble(parts[0]);
                    double lng = Double.parseDouble(parts[1]);
                    return new Double[]{lat, lng};
                } catch (NumberFormatException ignored) {}
            }
        }

        // Add more formats as needed
        return null;
    }

    private static String resolveShortLink(String shortUrl) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(shortUrl).openConnection();
            con.setInstanceFollowRedirects(false);
            con.setRequestMethod("GET");
            con.connect();
            String location = con.getHeaderField("Location");
            if (location != null) {
                return location;
            }
        } catch (Exception e) {
            // Optionally log error
        }
        return shortUrl; // fallback to original if not resolved
    }
} 