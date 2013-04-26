package com.whysearchtwice.frames;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface PageView extends VertexFrame {
    @Property("type")
    public void setType(String type);

    @Property("type")
    public String getType();

    @Property("pageUrl")
    public void setPageUrl(String pageUrl);

    @Property("pageUrl")
    public String getPageUrl();

    @Property("pageOpenTime")
    public void setPageOpenTime(Long pageOpenTime);

    @Property("pageOpenTime")
    public Long getPageOpenTime();

    @Property("pageCloseTime")
    public void setPageCloseTime(Long pageOpenTime);

    @Property("pageCloseTime")
    public Long getPageCloseTime();

    @Property("tabId")
    public void setTabId(int tabId);

    @Property("tabId")
    public int getTabId();

    @Property("windowId")
    public void setWindowId(int windowId);

    @Property("windowId")
    public int getWindowId();

    @Adjacency(label = "viewed")
    public Iterable<Device> getViewingDevice();

    @Adjacency(label = "under")
    public Iterable<Domain> getDomain();

    @Adjacency(label = "under")
    public void setDomain(Domain domain);

    @Adjacency(label = "successorTo")
    public Iterable<PageView> getPredecessors();

    @Adjacency(label = "successorTo")
    public void addPredecessor(PageView pageview);

    @Adjacency(label = "childOf")
    public Iterable<PageView> getParents();

    @Adjacency(label = "childOf")
    public void addParent(PageView pageview);
}
