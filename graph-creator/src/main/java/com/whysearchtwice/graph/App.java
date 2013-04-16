package com.whysearchtwice.graph;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanLabel;
import com.tinkerpop.blueprints.Direction;

/**
 * Create the indices that are needed on the WhySearchTwice graph database
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Getting Graph");

        Configuration conf = new BaseConfiguration();
        conf.setProperty("storage.backend", "hbase");
        TitanGraph graph = TitanFactory.open(conf);

        System.out.println("Creating Vertex Indices");
        TitanKey username = graph.makeType().name("username").dataType(String.class).makePropertyKey();
        TitanKey domain = graph.makeType().name("domain").dataType(String.class).makePropertyKey();
        TitanKey pageOpenTime = graph.makeType().name("pageOpenTime").dataType(Long.class).unique(Direction.OUT).makePropertyKey();

        System.out.println("Creating Edge Indices");
        TitanLabel pageViewEdge = graph.makeType().name("viewed").primaryKey(pageOpenTime).makeEdgeLabel();
        
        System.out.println("Done");
    }
}
