package com.whysearchtwice.extensions;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.whysearchtwice.frames.Device;
import com.whysearchtwice.frames.User;
import com.whysearchtwice.rexster.extension.CreateIndices;
import com.whysearchtwice.rexster.extension.UserExtension;

@RunWith(JUnit4.class)
public class UserExtensionTest {
    private TitanGraph graph;
    private FramedGraph<TitanGraph> manager;

    @Test
    public void createUser() throws JSONException {
        System.out.println("Testing create user");

        JSONObject body = new JSONObject();
        body.put("emailAddress", "testUser@example.com");

        UserExtension userExtension = new UserExtension();
        createUser(body, userExtension);
    }

    @Test
    public void createDeviceForExistingUser() throws JSONException {
        System.out.println("Testing create new device for existing user");

        JSONObject body = new JSONObject();
        body.put("emailAddress", "testUser@example.com");

        UserExtension userExtension = new UserExtension();
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

    @Before
    public void setupGraph() {
        graph = TitanFactory.open("/tmp/titan");
        manager = new FramedGraph<TitanGraph>(graph);

        CreateIndices indicesExtension = new CreateIndices();
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, new JSONObject(), null, null, null);
        indicesExtension.createIndices(ctx, graph);
    }

    @After
    public void deleteGraph() throws IOException {
        manager.shutdown();
        manager = null;
        if (graph.isOpen()) {
            graph.shutdown();
        }
        graph = null;

        File directory = new File("/tmp/titan");
        if (directory.exists()) {
            delete(directory);
        }
    }

    private static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            if (file.list().length == 0) {
                file.delete();
            } else {
                for (String temp : file.list()) {
                    File fileDelete = new File(file, temp);
                    delete(fileDelete);
                }

                if (file.list().length == 0) {
                    file.delete();
                }
            }
        } else {
            file.delete();
        }
    }

    private JSONObject createUser(JSONObject body, UserExtension userExtension) throws JSONException {
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        JSONObject responseContent = (JSONObject) userExtension.createDevice(ctx, graph).getJerseyResponse().getEntity();

        Device device = manager.frame(graph.getVertex(responseContent.getString("deviceGuid")), Device.class);
        Assert.assertTrue(device.getOwner().iterator().hasNext());

        if (responseContent.has("userGuid")) {
            User user = manager.frame(graph.getVertex(responseContent.getString("userGuid")), User.class);
            Assert.assertEquals("testUser@example.com", user.asVertex().getProperty("emailAddress"));
            Assert.assertEquals("testUser@example.com", user.getEmail());

            Assert.assertEquals(user.asVertex().getId(), device.getOwner().iterator().next().asVertex().getId());
        }

        return responseContent;
    }
}
