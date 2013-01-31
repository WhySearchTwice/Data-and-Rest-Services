package com.whysearchtwice.extensions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.AbstractRexsterExtension;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;

@ExtensionNaming(name = "vertex", namespace = "parsley")
public class ParsleyExtension extends AbstractRexsterExtension {

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "createVertex", method = HttpMethod.POST)
    @ExtensionDescriptor(description = "create a new vertex in the graph")
    public ExtensionResponse doSomeWorkOnGraph(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph) {
        JSONObject attributes = context.getRequestObject();
        Iterator keysIter = attributes.keys();

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();

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

        // Return the id of the new Vertex
        map.put("id", newVertex.getId().toString());

        // Create an edge to the Predecessor if needed
        if (attributes.has("predecessor")) {
            try {
                Vertex predecessorVertex = graph.getVertex(attributes.get("predecessor"));
                if (predecessorVertex != null) {
                    System.out.println("Creating the predecessor edges");
                    Edge e1 = graph.addEdge(null, newVertex, predecessorVertex, "successorTo");
                    Edge e2 = graph.addEdge(null, predecessorVertex, newVertex, "predecessorTo");
                    System.out.println(e1);
                    System.out.println(e2);
                } else {
                    map.put("error", "could not find predecessor");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Create an edge to the parent if needed
        if (attributes.has("parent")) {
            try {
                Vertex parentVertex = graph.getVertex(attributes.get("parent"));
                if (parentVertex != null) {
                    graph.addEdge(null, newVertex, parentVertex, "childOf");
                    graph.addEdge(null, parentVertex, newVertex, "parentOf");
                } else {
                    map.put("error", "could not find parent");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return ExtensionResponse.ok(map);
    }
}
