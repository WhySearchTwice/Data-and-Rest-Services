package com.whysearchtwice.frames;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface PageView extends VertexFrame {
    @Property("type")
    void setType(String type);

    @Property("type")
    String getType();

    @Property("pageUrl")
    void setPageUrl(String pageUrl);

    @Property("pageUrl")
    String getPageUrl();

    @Property("pageOpenTime")
    void setPageOpenTime(Long pageOpenTime);

    @Property("pageOpenTime")
    Long getPageOpenTime();

    @Property("pageCloseTime")
    void setPageCloseTime(Long pageOpenTime);

    @Property("pageCloseTime")
    Long getPageCloseTime();

    @Property("tabId")
    void setTabId(int tabId);

    @Property("tabId")
    int getTabId();

    @Property("windowId")
    void setWindowId(int windowId);

    @Property("windowId")
    int getWindowId();

    @Adjacency(label = "viewed", direction = Direction.IN)
    Iterable<Device> getDevice();

    @Adjacency(label = "viewed", direction = Direction.IN)
    void setDevice(Device device);

    @Adjacency(label = "under")
    Iterable<Domain> getDomain();

    @Adjacency(label = "under")
    void setDomain(Domain domain);

    @Adjacency(label = "successorTo")
    Iterable<PageView> getPredecessors();

    @Adjacency(label = "successorTo")
    void addPredecessor(PageView pageview);

    @Adjacency(label = "successorTo", direction = Direction.IN)
    Iterable<PageView> getSuccessors();

    @Adjacency(label = "successorTo", direction = Direction.IN)
    void addSuccessor(PageView pageview);

    @Adjacency(label = "childOf")
    Iterable<PageView> getParents();

    @Adjacency(label = "childOf")
    void addParent(PageView pageview);

    @Adjacency(label = "childOf", direction = Direction.IN)
    Iterable<PageView> getChildren();

    @Adjacency(label = "childOf", direction = Direction.IN)
    void addChild(PageView pageview);

    @Incidence(label = "viewed", direction = Direction.IN)
    Iterable<Viewed> getViewed();
}
