package com.moondust.antiflowerpottheft;

import org.bukkit.Location;

public final class RegionSelection {
    private Location first;
    private Location second;

    public Location getFirst() {
        return first;
    }

    public void setFirst(Location first) {
        this.first = first;
    }

    public Location getSecond() {
        return second;
    }

    public void setSecond(Location second) {
        this.second = second;
    }

    public boolean isComplete() {
        return first != null && second != null;
    }

    public boolean isSameWorld() {
        return isComplete()
                && first.getWorld() != null
                && second.getWorld() != null
                && first.getWorld().equals(second.getWorld());
    }
}
