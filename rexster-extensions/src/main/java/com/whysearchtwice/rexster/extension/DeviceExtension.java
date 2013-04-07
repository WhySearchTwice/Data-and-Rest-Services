package com.whysearchtwice.rexster.extension;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
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

@ExtensionNaming(name = DeviceExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class DeviceExtension extends AbstractParsleyExtension {
    public static final String NAME = "device";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "update a username or attach a device")
    public ExtensionResponse updateDevice(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph) {
        JSONObject attributes = context.getRequestObject();

        try {
            if (attributes.has("deviceName")) {
                return updateDeviceName(graph, attributes.getString("deviceGuid"), attributes.getString("deviceName"));
            } else if (attributes.has("newUserGuid")) {
                return updateDeviceOwner(graph, attributes.getString("deviceGuid"), attributes.getString("newUserGuid"));
            } else {
                return ExtensionResponse.error("Missing required field. See documentation for expected properties");
            }
        } catch (JSONException e) {
            return ExtensionResponse.error("Missing required field. See documentation for expected properties");
        }
    }

    private ExtensionResponse updateDeviceOwner(Graph graph, String deviceGuid, String newUserGuid) {
        Vertex newUser = graph.getVertex(newUserGuid);
        if (newUser != null) {
            return ExtensionResponse.error("Invalid userGuid");
        }

        Vertex device = graph.getVertex(deviceGuid);
        if (device == null) {
            return ExtensionResponse.error("Invalid deviceGuid");
        }

        // Delete the old edge
        for (Edge e : device.getEdges(Direction.IN, "owns")) {
            graph.removeEdge(e);
        }

        // Create the new
        graph.addEdge(null, newUser, device, "owns");

        Map<String, String> map = new HashMap<String, String>();
        map.put("message", "device owner updated");
        return ExtensionResponse.ok(map);
    }

    private ExtensionResponse updateDeviceName(Graph graph, String deviceGuid, String deviceName) {
        Vertex user = graph.getVertex(deviceGuid);
        if (user != null) {
            user.setProperty("deviceName", deviceName);
        } else {
            return ExtensionResponse.error("Invalid deviceGuid");
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("message", "device name updated");
        return ExtensionResponse.ok(map);
    }
}
