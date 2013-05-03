package com.whysearchtwice.frames;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface Device extends VertexFrame {
    @Property("type")
    public void setType(String type);

    @Property("type")
    public String getType();

    @Adjacency(label = "owns", direction = Direction.IN)
    public Iterable<User> getOwner();

    @Adjacency(label = "viewed")
    public Iterable<PageView> getPageViews();

    @Adjacency(label = "viewed")
    public void addPageView(PageView pageview);
    
    @Incidence(label = "viewed")
    public Iterable<Viewed> getViewed();
}
