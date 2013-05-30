package com.whysearchtwice.frames;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface Device extends VertexFrame {
    @Property("type")
    void setType(String type);

    @Property("type")
    String getType();

    @Adjacency(label = "owns", direction = Direction.IN)
    Iterable<User> getOwner();

    @Adjacency(label = "viewed")
    Iterable<PageView> getPageViews();

    @Adjacency(label = "viewed")
    void addPageView(PageView pageview);

    @Incidence(label = "viewed")
    Iterable<Viewed> getViewed();
}
