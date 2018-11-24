package com.example.yendry.internetchecker2.eventbus;

/**
 * Created by yendry.calana on 3/23/18.
 */

public class ServiceMessage {
    private int interval = -1;
    private boolean ring;
    private boolean default_notification;

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public boolean isRing() {
        return ring;
    }

    public void setRing(boolean ring) {
        this.ring = ring;
    }

    public boolean isDefault_notification() {
        return default_notification;
    }

    public void setDefault_notification(boolean default_notification) {
        this.default_notification = default_notification;
    }
}
