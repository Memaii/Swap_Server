package com.dmvlab.swapserver.model;

public class ServerEntry {
    private String name;
    private String ip;
    private int port;

    /**
     * Creates an empty server entry for serialization.
     */
    public ServerEntry() {}

    /**
     * Creates a server entry with the provided values.
     *
     * @param name display name for the server
     * @param ip host or IP address
     * @param port server port
     */
    public ServerEntry(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    /**
     * Returns the server display name.
     *
     * @return the server name
     */
    public String getName() { return name; }

    /**
     * Returns the server host or IP address.
     *
     * @return the server address
     */
    public String getIp() { return ip; }

    /**
     * Returns the server port.
     *
     * @return the server port
     */
    public int getPort() { return port; }

    /**
     * Updates the server display name.
     *
     * @param name new server name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Updates the server host or IP address.
     *
     * @param ip new server address
     */
    public void setIp(String ip) { this.ip = ip; }

    /**
     * Updates the server port.
     *
     * @param port new server port
     */
    public void setPort(int port) { this.port = port; }
}
