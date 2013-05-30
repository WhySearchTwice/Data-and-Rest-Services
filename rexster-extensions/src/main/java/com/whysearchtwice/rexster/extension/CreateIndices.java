package com.whysearchtwice.rexster.extension;

import java.util.HashMap;
import java.util.Map;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanKey;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;

@ExtensionNaming(name = CreateIndices.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class CreateIndices extends AbstractParsleyExtension {
    public static final String NAME = "setup";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "create the required indices on the graph")
    public ExtensionResponse createIndices(@RexsterContext Graph graph) {
        if (graph instanceof TitanGraph) {
            TitanGraph tg = (TitanGraph) graph;

            tg.makeType().name("username").dataType(String.class).unique(Direction.OUT).indexed(Vertex.class).makePropertyKey();
            tg.makeType().name("domain").dataType(String.class).unique(Direction.OUT).indexed(Vertex.class).makePropertyKey();
            TitanKey pageOpenTime = tg.makeType().name("pageOpenTime").dataType(Long.class).unique(Direction.OUT).indexed(Vertex.class).makePropertyKey();

            tg.makeType().name("viewed").primaryKey(pageOpenTime).makeEdgeLabel();

            Map<String, String> result = new HashMap<String, String>();
            result.put("message", "Indices created");
            return ExtensionResponse.ok(result);
        } else {
            return ExtensionResponse.error("Invalid graph format");
        }
    }
}
