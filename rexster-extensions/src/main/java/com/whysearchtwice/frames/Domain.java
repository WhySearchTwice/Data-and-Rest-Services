package com.whysearchtwice.frames;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface Domain extends VertexFrame {
    @Property("type")
    void setType(String type);

    @Property("type")
    String getType();

    @Property("domain")
    void setDomain(String domain);

    @Property("domain")
    String getDomain();
}
