package com.whysearchtwice.frames;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface User extends VertexFrame {
    @Property("type")
    void setType(String type);

    @Property("type")
    String getType();

    @Property("emailAddress")
    void setEmail(String email);

    @Property("emailAddress")
    String getEmail();

    @Adjacency(label = "owns")
    Iterable<Device> getDevices();

    @Adjacency(label = "owns")
    void addDevice(Device device);
}
