package com.whysearchtwice.extensions;

import java.util.Iterator;

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
    public void createDeviceForExistingUser() throws JSONException {
        System.out.println("Testing create new device for existing user");

        JSONObject body = new JSONObject();
        body.put("emailAddress", "testUser@example.com");

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
}
