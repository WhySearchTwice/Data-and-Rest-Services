package com.whysearchtwice.rexster.extension;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

@ExtensionNaming(name = UserExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class UserExtension extends AbstractParsleyExtension {
    public static final String NAME = "user";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "update a username or attach a device")
    public ExtensionResponse updateVertex(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph) {
        JSONObject attributes = context.getRequestObject();
        try {
            if (attributes.has("ownedDeviceGuid")) {
                linkOwnedDevice(graph, attributes.getString("deviceGuid"), attributes.getString("userGuid"));
            } else if (attributes.has("username")) {
                updateUsername(graph, attributes.getString("userGuid"), attributes.getString("username"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();
        map.put("message", "vertex updated");

        return ExtensionResponse.ok(map);
    }

    public void updateUsername(Graph graph, String userGuid, String username) {
        Vertex user = graph.getVertex(userGuid);
        user.setProperty("username", username);
    }

    public void linkOwnedDevice(Graph graph, String deviceGuid, String userGuid) {
        Vertex device = graph.getVertex(deviceGuid);
        Vertex user = graph.getVertex(userGuid);

        graph.addEdge(null, device, user, "ownedBy");
        graph.addEdge(null, device, user, "owns");
    }
}
