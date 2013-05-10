package com.whysearchtwice.extensions;

import java.util.Iterator;

import com.tinkerpop.blueprints.Vertex;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.tinkerpop.rexster.RexsterResourceContext;
import com.whysearchtwice.frames.Device;
import com.whysearchtwice.frames.User;
import com.whysearchtwice.rexster.extension.UserExtension;

@RunWith(JUnit4.class)
public class UserExtensionTest extends ExtensionTest {
    private static UserExtension userExtension;

    @BeforeClass
    public static void setupClass() {
        userExtension = new UserExtension();
    }

    @Test
    public void createUser() throws JSONException {
        System.out.println("Testing create user");

        JSONObject body = new JSONObject();
        body.put("emailAddress", "testUser@example.com");

        createUser(body, userExtension);
    }

    @Test
    public void createUserWithBadGuid() throws JSONException {
        System.out.println("Testing create user with a bad guid");

        // Create a random vertex
        Vertex v = graph.addVertex(null);
        v.setProperty("type", "random");
        System.out.println(v.getId());

        // Create a user
        JSONObject body = new JSONObject();
        body.put("userGuid", v.getId());
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        JSONObject response = (JSONObject) userExtension.createDevice(ctx, graph).getJerseyResponse().getEntity();

        // Check that it threw an error
        Assert.assertEquals("Invalid userGuid", response.getString("message"));
    }

    @Test
    public void createDeviceForExistingUser() throws JSONException {
        System.out.println("Testing create new device for existing user");

        // Create a user
        JSONObject body = new JSONObject().put("emailAddress", "testUser@example.com");
        JSONObject response = createUser(body, userExtension);

        // Add the user to the body and issue a create request again
        User user = manager.frame(graph.getVertex(response.getString("userGuid")), User.class);
        body.put("userGuid", user.asVertex().getId());
        createUser(body, userExtension);

        // Check that there are now two devices
        int count = 0;
        Iterator<Device> iter = user.getDevices().iterator();
        for (; iter.hasNext(); ++count)
            iter.next();

        Assert.assertEquals(2, count);
    }

    @Test
    public void renewUserTest() throws JSONException {
        System.out.println("Testing renew user");

        JSONObject body = new JSONObject()
                .put("userGuid", "an invalid userGuid")
                .put("deviceGuid", "an invalid deviceGuid");

        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        JSONObject response = (JSONObject) userExtension.renewUserOrDevice(ctx, graph).getJerseyResponse().getEntity();
        String userGuid = response.getString("userGuid");
        String deviceGuid = response.getString("deviceGuid");

        // Check that a new user was created
        Vertex v = graph.getVertex(response.getString("userGuid"));
        Assert.assertNotEquals(null, v);
        Assert.assertEquals("an invalid userGuid", v.getProperty("oldUserGuid"));

        // Renew again with the invalid userGuid and new valid deviceGuid and check that the same info was returned
        body.put("deviceGuid", deviceGuid);
        ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        response = (JSONObject) userExtension.renewUserOrDevice(ctx, graph).getJerseyResponse().getEntity();
        Assert.assertEquals(userGuid, response.getString("userGuid"));
        Assert.assertEquals(deviceGuid, response.getString("deviceGuid"));
    }

    @Test
    public void lookupUserTest() throws JSONException {
        System.out.println("Testing user lookup");

        // Create a valid user to look up
        JSONObject body = new JSONObject();
        body.put("emailAddress", "testUser@example.com");
        JSONObject response = createUser(body, userExtension);
        String userGuid = response.getString("userGuid");

        // Look up valid user
        response = (JSONObject) userExtension.lookupUser(graph, "testUser@example.com").getJerseyResponse().getEntity();
        Assert.assertEquals(userGuid, response.getString("userGuid"));

        // Lookup invalid user
        response = (JSONObject) userExtension.lookupUser(graph, "invaludUser@example.com").getJerseyResponse().getEntity();
        Assert.assertEquals("No user found with that email address", response.getString("message"));

        // Lookup with no email address
        response = (JSONObject) userExtension.lookupUser(graph, null).getJerseyResponse().getEntity();
        Assert.assertEquals("Must include an email address", response.getString("message"));
        response = (JSONObject) userExtension.lookupUser(graph, "").getJerseyResponse().getEntity();
        Assert.assertEquals("Must include an email address", response.getString("message"));
    }

    @Test
    public void updateEmailAddress() throws JSONException {
        System.out.println("Testing update user email address");

        // Create a valid user to update
        JSONObject body = new JSONObject().put("emailAddress", "testUser@example.com");
        JSONObject response = createUser(body, userExtension);
        User user = manager.frame(graph.getVertex(response.getString("userGuid")), User.class);
        Assert.assertEquals("testUser@example.com", user.getEmail());

        body.put("emailAddress", "updatedEmail@example.com");
        body.put("userGuid", user.asVertex().getId());
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        userExtension.updateEmail(ctx, user.asVertex(), graph).getJerseyResponse().getEntity();
        Assert.assertEquals("updatedEmail@example.com", user.getEmail());

        // Test invalid user
        response = (JSONObject) userExtension.updateEmail(ctx, null, graph).getJerseyResponse().getEntity();
        Assert.assertEquals("Invalid User", response.getString("message"));

        // Test without sending email address
        body.remove("emailAddress");
        ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        response = (JSONObject) userExtension.updateEmail(ctx, user.asVertex(), graph).getJerseyResponse().getEntity();
        Assert.assertEquals("Must include emailAddress", response.getString("message"));
    }
}
