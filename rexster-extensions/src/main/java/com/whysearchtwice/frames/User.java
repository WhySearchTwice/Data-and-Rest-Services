package com.whysearchtwice.frames;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface User extends VertexFrame {
    @Property("type")
    public void setType(String type);
    
    @Property("type")
    public String getType();
    
    @Property("emailAddress")
    public void setEmail(String email);
    
    @Property("emailAddress")
    public String getEmail();
    
    @Adjacency(label="owns")
    public Iterable<Device> getDevices();
    
    @Adjacency(label="owns")
    public void addDevice(Device device);
}
