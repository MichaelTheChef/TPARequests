package me.mio.tpatest;

import java.util.HashMap;
import java.util.Map;

public class CooldownMap<K> {
    private final Map<K, Long> cooldowns = new HashMap<>();
    // As a generic is not needed since the only type which is use is the Player type, I will probably just rewrite this.

    public boolean hasCooldown(K key) {
        if (!cooldowns.containsKey(key))
            return false;

        long currentTime = System.currentTimeMillis();
        long cooldownTime = cooldowns.get(key);
        return currentTime < cooldownTime;
    }

    public void setCooldown(K key, long cooldownMillis) {
        long currentTime = System.currentTimeMillis();
        long cooldownTime = currentTime + cooldownMillis;
        cooldowns.put(key, cooldownTime);
    }
}
