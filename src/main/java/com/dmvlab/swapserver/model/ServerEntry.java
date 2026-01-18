package com.dmvlab.swapserver.model;

public class ServerEntry {
    private String name;
    private String ip;
    private int port;
    private boolean isMain;

    public ServerEntry() {}
    public ServerEntry(String name, String ip, int port, boolean isMain) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.isMain = isMain;
    }

    public String getName() { return name; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public boolean isMain() { return isMain; }

    public void setName(String name) { this.name = name; }
    public void setIp(String ip) { this.ip = ip; }
    public void setPort(int port) { this.port = port; }
    public void setMain(boolean main) { isMain = main; }
}
