package com.example.NearMeBKND.Business.Util;

public class bsns_BusinessGeocodeUtil {
    public static Double[] extractLatLongFromGoogleMapsURL(String url) {
        // Example: https://maps.google.com/?q=12.9716,77.5946
        try {
            if (url == null) return new Double[]{null, null};
            String[] parts = url.split("q=");
            if (parts.length < 2) return new Double[]{null, null};
            String[] coords = parts[1].split(",");
            if (coords.length < 2) return new Double[]{null, null};
            Double lat = Double.parseDouble(coords[0]);
            Double lng = Double.parseDouble(coords[1]);
            return new Double[]{lng, lat};
        } catch (Exception e) {
            return new Double[]{null, null};
        }
    }
} 