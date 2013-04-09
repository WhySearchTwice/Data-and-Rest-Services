package com.whysearchtwice.rexster.extension;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.groovy.Gremlin;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.util.iterators.SingleIterator;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;
import com.whysearchtwice.container.PageView;

@ExtensionNaming(name = CleanupExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class CleanupExtension extends AbstractParsleyExtension {
    public static final String NAME = "cleanup";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, path = "openTabs", method = HttpMethod.GET)
    @ExtensionDescriptor(description = "update a username or attach a device")
    public ExtensionResponse updateDevice(@RexsterContext Vertex vertex) {
        if (vertex == null) {
            return ExtensionResponse.error("Invalid vertex");
        }

        String gremlinQuery = null;
        String type = (String) vertex.getProperty("type");
        JSONObject results = new JSONObject();

        // The search may be starting from a user or a device. Adjust the query
        // accordingly.

        if (type.equals("user")) {
            gremlinQuery = "_().out('owns').out('viewed').has('pageCloseTime', null)";
        } else if (type.equals("device")) {
            gremlinQuery = "_().out('viewed').has('pageCloseTime', null)";
        } else {
            return ExtensionResponse.error("Vertex is not a user or device");
        }
        Pipe pipe = Gremlin.compile(gremlinQuery);
        pipe.setStarts(new SingleIterator<Vertex>(vertex));
        for (Object result : pipe) {
            if (result instanceof Vertex) {
                try {
                    results.accumulate("results", new PageView((Vertex) result).exportJson());
                } catch (JSONException e) {
                    return ExtensionResponse.error("Failed to create JSON Result");
                }
            }
        }

        return ExtensionResponse.ok(results);
    }
}
