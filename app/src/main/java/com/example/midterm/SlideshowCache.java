    package com.example.midterm;

    import java.util.ArrayList;
    import java.util.Map;
    import java.util.UUID;
    import java.util.concurrent.ConcurrentHashMap;

    public final class SlideshowCache {
        private SlideshowCache() {}

        private static final Map<String, ArrayList<String>> CACHE = new ConcurrentHashMap<>();

        public static String put(ArrayList<String> list) {
            String key = UUID.randomUUID().toString();
            CACHE.put(key, list);
            return key;
        }

        // Non-destructive get for configuration changes
        public static ArrayList<String> get(String key) {
            if (key == null) return null;
            return CACHE.get(key);
        }

        public static ArrayList<String> getAndRemove(String key) {
            if (key == null) return null;
            return CACHE.remove(key);
        }

        public static void remove(String key) {
            if (key != null) {
                CACHE.remove(key);
            }
        }
    }
