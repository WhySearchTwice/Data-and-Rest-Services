package com.whysearchtwice.rexster.extension;

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

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "updateUser", method = HttpMethod.POST)
    @ExtensionDescriptor(description = "update an email or attach a device")
    public ExtensionResponse updateUser(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph) {
        JSONObject attributes = context.getRequestObject();
        try {
            if (attributes.has("ownedDeviceGuid")) {
                linkOwnedDevice(graph, attributes.getString("ownedDeviceGuid"), attributes.getString("userGuid"));
            } else if (attributes.has("emailAddress")) {
                updateEmailAddress(graph, attributes.getString("userGuid"), attributes.getString("emailAddress"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();
        map.put("message", "vertex updated");

        return ExtensionResponse.ok(map);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "lookupUser", method = HttpMethod.GET)
    @ExtensionDescriptor(description = "find a user based on their email")
    public ExtensionResponse lookupUser(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph,
            @ExtensionRequestParameter(name = "emailAddress", defaultValue = "", description = "An email address to look up") String emailAddress) {

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();

        if (emailAddress.length() > 0) {
            for (Vertex v : graph.getVertices("type", "user")) {
                String potentialEmail = (String) v.getProperty("emailAddress");
                if (potentialEmail != null && potentialEmail.equals(emailAddress)) {
                    map.put("userGuid", v.getId().toString());
                }
            }
        } else {
            return ExtensionResponse.error("Must include an email address");
        }

        return ExtensionResponse.ok(map);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "createDevice", method = HttpMethod.GET)
    @ExtensionDescriptor(description = "create a new device (and optionally user)")
    public ExtensionResponse createDevice(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph,
            @ExtensionRequestParameter(name = "userGuid", defaultValue = "", description = "userGuid which the device should be bound to") String existingUserGuid,
            @ExtensionRequestParameter(name = "emailAddress", defaultValue = "", description = "An optional email address to attach to the user") String emailAddress) {

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();

        if (existingUserGuid.length() > 0) {
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

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "renewDevice", method = HttpMethod.GET)
    @ExtensionDescriptor(description = "get a new user or device or look up existing")
    public ExtensionResponse getNewUser(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph,
            @ExtensionRequestParameter(name = "userGuid", defaultValue = "", description = "An old userGuid that is no longer valid") String oldUserGuid,
            @ExtensionRequestParameter(name = "deviceGuid", defaultValue = "", description = "An old deviceGuid that is no longer valid") String oldDeviceGuid) {

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
            // Check if there is an "oldDeviceGuid" on any device nodes
            for (Vertex v : graph.getVertices("type", "device")) {
                String foundDeviceGuid = (String) v.getProperty("oldDeviceGuid");
                if (foundDeviceGuid != null && foundDeviceGuid.equals(oldDeviceGuid)) {
                    device = v;
                }
            }

            // If we still didn't find anything, create a new user
            if (device == null || !device.getProperty("type").equals("device")) {
                device = (oldDeviceGuid.length() > 0) ? createDevice(graph, user, oldDeviceGuid) : createDevice(graph, user);
            }
        }

        map.put("userGuid", user.getId().toString());
        map.put("deviceGuid", device.getId().toString());

        return ExtensionResponse.ok(map);
    }

    private Vertex createUser(Graph graph, String emailAddress) {
        Vertex user = graph.addVertex(null);
        user.setProperty("type", "user");
        user.setProperty("timeCreated", System.currentTimeMillis());

        if (emailAddress.length() > 0) {
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

    private Vertex createDevice(Graph graph, Vertex user, String oldDeviceGuid) {
        Vertex device = createDevice(graph, user);
        device.setProperty("oldDeviceGuid", oldDeviceGuid);
        return device;
    }

    private void updateEmailAddress(Graph graph, String userGuid, String username) {
        Vertex user = graph.getVertex(userGuid);
        user.setProperty("username", username);
    }

    private void linkOwnedDevice(Graph graph, String deviceGuid, String userGuid) {
        Vertex device = graph.getVertex(deviceGuid);
        Vertex user = graph.getVertex(userGuid);

        graph.addEdge(null, device, user, "owns");
    }
}
