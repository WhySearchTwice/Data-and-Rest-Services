package com.whysearchtwice.extensions;

import com.tinkerpop.rexster.RexsterResourceContext;
import com.whysearchtwice.frames.Device;
import com.whysearchtwice.frames.PageView;
import com.whysearchtwice.frames.User;
import com.whysearchtwice.rexster.extension.CleanupExtension;
import com.whysearchtwice.rexster.extension.PageViewExtension;
import com.whysearchtwice.rexster.extension.UserExtension;
import org.junit.Assert;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class CleanupExtensionTest extends ExtensionTest {
    private static UserExtension userExtension;
    private static CleanupExtension cleanupExtension;
    private static PageViewExtension pageViewExtension;

    @BeforeClass
    public static void setupClass() {
        userExtension = new UserExtension();
        cleanupExtension = new CleanupExtension();
        pageViewExtension = new PageViewExtension();
    }

    @Test
    public void retrieveOpenTabsTest() throws JSONException {
        System.out.println("Testing retrieve open tabs");

        // Create a user
        JSONObject body = new JSONObject().put("emailAddress", "testUser@example.com");
        JSONObject response = createUser(body, userExtension);
        User user = manager.frame(graph.getVertex(response.getString("userGuid")), User.class);
        Device device = manager.frame(graph.getVertex(response.getString("deviceGuid")), Device.class);

        String deviceGuid = Long.toString((Long) device.asVertex().getId());
        String userGuid = Long.toString((Long) user.asVertex().getId());

        // Open some tabs for this user
        body = new JSONObject()
                .put("type", "pageView")
                .put("tabId", 1761)
                .put("pageUrl", "http://google.com")
                .put("windowId", 515)
                .put("pageOpenTime", 1367460869100L)
                .put("clientVersion", "0.0.13")
                .put("userGuid", userGuid)
                .put("deviceGuid", deviceGuid);
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        JSONObject pageViewResponse = (JSONObject) pageViewExtension.createNewVertex(ctx, graph).getJerseyResponse().getEntity();
        PageView pv1 = manager.frame(graph.getVertex(pageViewResponse.getString("id")), PageView.class);

        body.put("tabId", 1234)
                .put("pageUrl", "http://github.com")
                .put("pageOpenTime", 1367460870100L);
        ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        pageViewResponse = (JSONObject) pageViewExtension.createNewVertex(ctx, graph).getJerseyResponse().getEntity();
        PageView pv2 = manager.frame(graph.getVertex(pageViewResponse.getString("id")), PageView.class);

        // Get the open tabs
        response = (JSONObject) cleanupExtension.retrieveOpenTabs(graph, user.asVertex()).getJerseyResponse().getEntity();
        Assert.assertEquals(2, response.getJSONArray("results").length());
    }

    @Test
    public void retrieveOpenTabsOfNullUser() throws JSONException {
        System.out.println("Testing retrieve open tabs of a null user");

        JSONObject response = (JSONObject) cleanupExtension.retrieveOpenTabs(graph, null).getJerseyResponse().getEntity();
        Assert.assertEquals("Invalid vertex", response.getString("message"));
    }

    @Test
    public void closeOpenTabs() throws JSONException {
        System.out.println("Testing close open tabs");

        // Create a user
        JSONObject body = new JSONObject().put("emailAddress", "testUser@example.com");
        JSONObject response = createUser(body, userExtension);
        User user = manager.frame(graph.getVertex(response.getString("userGuid")), User.class);
        Device device = manager.frame(graph.getVertex(response.getString("deviceGuid")), Device.class);

        String deviceGuid = Long.toString((Long) device.asVertex().getId());
        String userGuid = Long.toString((Long) user.asVertex().getId());

        // Open some tabs for this user
        body = new JSONObject()
                .put("type", "pageView")
                .put("tabId", 1761)
                .put("pageUrl", "http://google.com")
                .put("windowId", 515)
                .put("pageOpenTime", 1367460869100L)
                .put("clientVersion", "0.0.13")
                .put("userGuid", userGuid)
                .put("deviceGuid", deviceGuid);
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        JSONObject pageViewResponse = (JSONObject) pageViewExtension.createNewVertex(ctx, graph).getJerseyResponse().getEntity();
        PageView pv1 = manager.frame(graph.getVertex(pageViewResponse.getString("id")), PageView.class);

        body.put("tabId", 1234)
                .put("pageUrl", "http://github.com")
                .put("pageOpenTime", 1367460870100L);
        ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        pageViewResponse = (JSONObject) pageViewExtension.createNewVertex(ctx, graph).getJerseyResponse().getEntity();
        PageView pv2 = manager.frame(graph.getVertex(pageViewResponse.getString("id")), PageView.class);

        // Ensure the tabs are open
        Assert.assertNull(pv1.getPageCloseTime());
        Assert.assertNull(pv2.getPageCloseTime());

        // Get the open tabs
        body = new JSONObject();
        ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        cleanupExtension.closeOpenTabs(ctx, graph, user.asVertex());

        // Ensure the tabs are closed
        Assert.assertNotNull(pv1.getPageCloseTime());
        Assert.assertNotNull(pv2.getPageCloseTime());
    }
}
