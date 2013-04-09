package com.whysearchtwice.rexster.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<PageView> results = new ArrayList<PageView>();

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
                results.add(new PageView((Vertex) result));
            }
        }

        // Turn list into JSON to return
        String listAsJSON = "[]";
        if (results.size() > 0) {
            listAsJSON = "[";
            for (PageView pv : results) {
                listAsJSON += pv.toString() + ", ";
            }
            listAsJSON = listAsJSON.substring(0, listAsJSON.length() - 2);
            listAsJSON += "]";
        }

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();
        map.put("results", listAsJSON);

        return ExtensionResponse.ok(map);
    }
}
