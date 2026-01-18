package com.dmvlab.swapserver.model;

public class PlayerPosition {
    public String world;
    public double x, y, z;
    public float yaw, pitch;

    public PlayerPosition() {}
    public PlayerPosition(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
