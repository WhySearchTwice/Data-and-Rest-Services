package com.whysearchtwice.extensions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.AbstractRexsterExtension;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;

@ExtensionNaming(name = "vertex", namespace = "parsley")
public class ParsleyExtension extends AbstractRexsterExtension {

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "createVertex", method = HttpMethod.POST)
    @ExtensionDescriptor(description = "create a new vertex in the graph")
    public ExtensionResponse doSomeWorkOnGraph(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph,
            @ExtensionRequestParameter(name = "predecessor", defaultValue = "-1", description = "id of the vertex preceeding this vertex") Integer predecessor,
            @ExtensionRequestParameter(name = "parent", defaultValue = "-1", description = "id of the parent vertex to this vertex") Integer parent) {

        JSONObject attributes = context.getRequestObject();
        Iterator keysIter = attributes.keys();

        // Create the new Vertex
        Vertex newVertex = graph.addVertex(null);
        while (keysIter.hasNext()) {
            try {
                String key = (String) keysIter.next();

                if (key.equals("type") || key.equals("pageUrl") || key.equals("userId") || key.equals("deviceId")) {
                    String value = (String) attributes.get(key);
                    newVertex.setProperty(key, value);
                } else if (key.equals("pageOpenTime") || key.equals("pageCloseTime")) {
                    Long value = (Long) attributes.get(key);
                    newVertex.setProperty(key, value);
                } else if (key.equals("tabId") || key.equals("windowId")) {
                    int value = (Integer) attributes.get(key);
                    newVertex.setProperty(key, value);
                } else {
                    // Ignore the property for now
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        // Create an edge to the Predecessor if needed
        if (predecessor != -1) {

        }

        // Create an edge to the parent if needed
        if (parent != -1) {

        }

        // Return the id of the new Vertex
        Map<String, String> map = new HashMap<String, String>();
        map.put("id", newVertex.getId().toString());

        return ExtensionResponse.ok(map);
    }
}
