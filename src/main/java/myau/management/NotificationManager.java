package myau.management;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationManager {

    public static class NotificationEntry {
        public final String message;
        public final long startMillis;
        public final long durationMillis;
        public final int color;

        public NotificationEntry(String message, long durationMillis, int color) {
            this.message = message;
            this.durationMillis = durationMillis;
            this.color = color;
            this.startMillis = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return this.durationMillis >= 0 && System.currentTimeMillis() - this.startMillis >= this.durationMillis;
        }

        public long getAge() {
            return System.currentTimeMillis() - this.startMillis;
        }
    }

    private final List<NotificationEntry> entries = new ArrayList<>();
    
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    public synchronized void add(String message) {
        this.add(message, 3000L);
    }

    public synchronized void add(String message, long durationMillis) {
        this.add(message, durationMillis, 0xFFFFFF);
    }

    public synchronized void add(String message, int color) {
        this.add(message, 3000L, color);
    }

    public synchronized void add(String message, long durationMillis, int color) {
        long currentTime = System.currentTimeMillis();
        
        if (messageCooldowns.containsKey(message) && (currentTime - messageCooldowns.get(message)) < 600) return;

        messageCooldowns.put(message, currentTime);
        this.entries.add(new NotificationEntry(message, durationMillis, color));
    }

    public synchronized List<NotificationEntry> getActive() {
        Iterator<NotificationEntry> it = this.entries.iterator();
        while (it.hasNext()) {
            if (it.next().isExpired()) {
                it.remove();
            }
        }
        return new ArrayList<>(this.entries);
    }

    public synchronized void clear() {
        this.entries.clear();
        this.messageCooldowns.clear();
    }
}