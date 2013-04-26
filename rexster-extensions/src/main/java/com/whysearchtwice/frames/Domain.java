package com.whysearchtwice.frames;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface Domain extends VertexFrame {
    @Property("type")
    public void setType(String type);

    @Property("type")
    public String getType();
    
    @Property("domain")
    public void setDomain(String domain);
    
    @Property("domain")
    public String getDomain();
}
