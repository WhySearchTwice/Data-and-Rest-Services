package com.whysearchtwice.rexster.extension;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.*;

import com.whysearchtwice.frames.User;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

@ExtensionNaming(name = UserExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class UserExtension extends AbstractParsleyExtension {
    public static final String NAME = "user";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, path = "email", method = HttpMethod.POST)
    @ExtensionDescriptor(description = "update an email or attach a device")
    public ExtensionResponse updateEmail(@RexsterContext RexsterResourceContext context, @RexsterContext Vertex vertex, @RexsterContext Graph graph) {
        if(vertex == null) {
            return ExtensionResponse.error("Invalid User");
        }

        JSONObject attributes = context.getRequestObject();
        try {
            if (attributes.has("emailAddress")) {
                FramedGraph<TitanGraph> manager = new FramedGraph<TitanGraph>((TitanGraph) graph);
                User user = manager.frame(vertex, User.class);
                user.setEmail(attributes.getString("emailAddress"));
            } else {
                return ExtensionResponse.error("Must include emailAddress");
            }
        } catch (JSONException e) {
            return ExtensionResponse.error("JSONException, please include emailAddress as string");
        }

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();
        map.put("message", "vertex updated");

        return ExtensionResponse.ok(map);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "lookup", method = HttpMethod.GET)
    @ExtensionDescriptor(description = "find a user based on their email")
    public ExtensionResponse lookupUser(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph,
            @ExtensionRequestParameter(name = "emailAddress", defaultValue = "", description = "An email address to look up") String emailAddress) {

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();

        if (emailAddress != null && emailAddress.length() > 0) {
            for (Vertex v : graph.getVertices("type", "user")) {
                String potentialEmail = v.getProperty("emailAddress");
                if (potentialEmail != null && potentialEmail.equals(emailAddress)) {
                    map.put("userGuid", v.getId().toString());
                    return ExtensionResponse.ok(map);
                }
            }
            map.put("message", "No user found with that email address");
        } else {
            return ExtensionResponse.error("Must include an email address");
        }

        return ExtensionResponse.ok(map);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "createDevice", method = HttpMethod.POST)
    @ExtensionDescriptor(description = "create a new device (and optionally user)")
    public ExtensionResponse createDevice(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph) {

        JSONObject attributes = context.getRequestObject();
        String existingUserGuid = attributes.optString("userGuid", null);
        String emailAddress = attributes.optString("emailAddress", null);

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();

        if (existingUserGuid != null) {
            // Check that this is a valid user
            Vertex user = graph.getVertex(existingUserGuid);
            if (user == null || !user.getProperty("type").equals("user")) {
                return ExtensionResponse.error("Invalid userGuid");
            }

            // Create a device and connect to this user
            Vertex device = createDevice(graph, user);
            map.put("deviceGuid", device.getId().toString());
        } else {
            // Create a user and device
            Vertex user = createUser(graph, emailAddress);
            Vertex device = createDevice(graph, user);

            map.put("deviceGuid", device.getId().toString());
            map.put("userGuid", user.getId().toString());
        }

        return ExtensionResponse.ok(map);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "renew", method = HttpMethod.POST)
    @ExtensionDescriptor(description = "get a new user or device or look up existing")
    public ExtensionResponse renewUserOrDevice(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph) {

        JSONObject attributes = context.getRequestObject();
        String oldUserGuid = attributes.optString("userGuid", "");
        String oldDeviceGuid = attributes.optString("deviceGuid", "");

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();

        // Check to see if there is an existing user
        Vertex user = graph.getVertex(oldUserGuid);
        if (user == null || !user.getProperty("type").equals("user")) {
            // Check if there is an "oldUserGuid" on any user nodes
            for (Vertex v : graph.getVertices("type", "user")) {
                String foundUserGuid = (String) v.getProperty("oldUserGuid");
                if (foundUserGuid != null && oldUserGuid.equals(foundUserGuid)) {
                    user = v;
                }
            }

            // If we still didn't find anything, create a new user
            if (user == null || !user.getProperty("type").equals("user")) {
                user = createUser(graph, "", oldUserGuid);
            }
        }

        // Check to see if there is an existing device
        Vertex device = graph.getVertex(oldDeviceGuid);
        if (device == null || !device.getProperty("type").equals("device")) {
            // If not, create a new device
            device = createDevice(graph, user);
        }

        map.put("userGuid", user.getId().toString());
        map.put("deviceGuid", device.getId().toString());

        return ExtensionResponse.ok(map);
    }

    private Vertex createUser(Graph graph, String emailAddress) {
        Vertex user = graph.addVertex(null);
        user.setProperty("type", "user");
        user.setProperty("timeCreated", System.currentTimeMillis());

        if (emailAddress != null && emailAddress.length() > 0) {
            user.setProperty("emailAddress", emailAddress);
        }

        return user;
    }

    private Vertex createUser(Graph graph, String emailAddress, String oldUserGuid) {
        Vertex user = createUser(graph, emailAddress);
        user.setProperty("oldUserGuid", oldUserGuid);
        return user;
    }

    private Vertex createDevice(Graph graph, Vertex user) {
        Vertex device = graph.addVertex(null);
        device.setProperty("type", "device");
        device.setProperty("timeCreated", System.currentTimeMillis());

        // Connect new device to user
        graph.addEdge(null, user, device, "owns");

        return device;
    }
}
