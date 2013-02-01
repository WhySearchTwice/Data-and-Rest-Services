package com.whysearchtwice.rexster.extension;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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

@ExtensionNaming(name = PageViewExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class PageViewExtension extends AbstractParsleyExtension {
    public static final String NAME = "pageView";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "create a new vertex in the graph")
    public ExtensionResponse createNewVertex(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph) {
        JSONObject attributes = context.getRequestObject();

        // Create the new Vertex
        Vertex newVertex = graph.addVertex(null);
        updateVertexProperties(newVertex, attributes);

        // Return the id of the new Vertex
        Map<String, String> map = new HashMap<String, String>();
        map.put("id", newVertex.getId().toString());

        // Create an edge to the Predecessor or Parent if needed
        try {
            if (attributes.has("predecessor")) {
                boolean result = createEdge(graph, newVertex, attributes.getInt("predecessor"), "successorTo", "predecessorTo");
                map.put("predecessor", (result) ? "predecessor created successfully" : "predecessor could not be created");
            }
            if (attributes.has("parent")) {
                boolean result = createEdge(graph, newVertex, attributes.getInt("parent"), "childOf", "parentOf");
                map.put("parent", (result) ? "parent created successfully" : "parent could not be created");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Connect to the device or create if necessary
        try {
            if (attributes.has("deviceGuid")) {
                // We already have a device, just attach the page
                Vertex device = graph.getVertex(attributes.get("deviceGuid"));
            } else {
                // Must create a device and tie to user
                // TODO: Do not have to create a function to retrieve by ID
                Vertex user = attributes.has("userGuid") ? getUser(attributes.get("userGuid")) : getUser();
                Vertex device = createDevice(user);
            }
        } catch (JSONException e) {

        }

        return ExtensionResponse.ok(map);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "update an existing vertex in the graph")
    public ExtensionResponse updateVertex(@RexsterContext RexsterResourceContext context, @RexsterContext Vertex vertex) {
        updateVertexProperties(vertex, context.getRequestObject());

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();
        map.put("message", "vertex updated");

        return ExtensionResponse.ok(map);
    }

    private Vertex getUser() {
        // TODO: Implement getUser()
        return null;
    }

    private Vertex getUser(Object userGuid) {
        // TODO: Implement getUser(userGuid)
        return null;
    }

    private Vertex createDevice(Vertex user) {
        // TODO: Implement createDeveice(user)
        return null;
    }

    private boolean createEdge(Graph graph, Vertex v1, int v2id, String message1, String message2) {
        Vertex v2 = graph.getVertex(v2id);
        if (v2 != null) {
            graph.addEdge(null, v1, v2, message1);
            graph.addEdge(null, v2, v1, message2);
            return true;
        } else {
            return false;
        }
    }

    private void updateVertexProperties(Vertex v, JSONObject attributes) {
        Iterator keysIter = attributes.keys();

        // For any property that exists in the map, update it
        while (keysIter.hasNext()) {
            try {
                String key = (String) keysIter.next();

                if (key.equals("type") || key.equals("pageUrl") || key.equals("userId") || key.equals("deviceId")) {
                    String value = (String) attributes.get(key);
                    v.setProperty(key, value);
                } else if (key.equals("pageOpenTime") || key.equals("pageCloseTime")) {
                    Long value = (Long) attributes.get(key);
                    v.setProperty(key, value);
                } else if (key.equals("tabId") || key.equals("windowId")) {
                    int value = (Integer) attributes.get(key);
                    v.setProperty(key, value);
                } else {
                    // Ignore the property for now
                }
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
    }
}
