package com.whysearchtwice.frames;

import com.tinkerpop.frames.EdgeFrame;
import com.tinkerpop.frames.Property;

public interface Viewed extends EdgeFrame {
    @Property("pageOpenTime")
    void setPageOpenTime(long pageOpenTime);

    @Property("pageOpenTime")
    long getPageOpenTime();
}
