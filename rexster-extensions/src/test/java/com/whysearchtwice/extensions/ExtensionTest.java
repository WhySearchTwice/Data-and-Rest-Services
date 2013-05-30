package com.whysearchtwice.extensions;

import java.io.File;
import java.io.IOException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.whysearchtwice.frames.Device;
import com.whysearchtwice.frames.User;
import com.whysearchtwice.rexster.extension.CreateIndices;
import com.whysearchtwice.rexster.extension.UserExtension;

public class ExtensionTest {
    protected TitanGraph graph;
    protected FramedGraph<TitanGraph> manager;

    @Before
    public void createGraph() {
        graph = TitanFactory.open("/tmp/titan");
        manager = new FramedGraph<TitanGraph>(graph);

        CreateIndices indicesExtension = new CreateIndices();
        indicesExtension.createIndices(graph);
    }

    @After
    public void deleteGraph() throws IOException {
        TitanGraph graph = manager.getBaseGraph();
        manager.shutdown();
        manager = null;
        if (graph.isOpen()) {
            graph.shutdown();
        }

        File directory = new File("/tmp/titan");
        if (directory.exists()) {
            delete(directory);
        }
    }

    protected void delete(File file) throws IOException {
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

    protected JSONObject createUser(JSONObject body, UserExtension userExtension) throws JSONException {
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
