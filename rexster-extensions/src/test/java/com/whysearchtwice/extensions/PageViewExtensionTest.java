package com.whysearchtwice.extensions;

import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.whysearchtwice.frames.Device;
import com.whysearchtwice.frames.PageView;
import com.whysearchtwice.frames.User;
import com.whysearchtwice.rexster.extension.PageViewExtension;
import com.whysearchtwice.rexster.extension.UserExtension;

@RunWith(JUnit4.class)
public class PageViewExtensionTest extends ExtensionTest {
    private static PageViewExtension pageViewExtension;
    private static UserExtension userExtension;

    @BeforeClass
    public static void setupClass() {
        pageViewExtension = new PageViewExtension();
        userExtension = new UserExtension();
    }

    @Test
    public void createPageView() throws JSONException {
        System.out.println("Testing create new page view for existing user");

        // Create a user to own the pageView
        JSONObject body = new JSONObject();
        body.put("emailAddress", "testUser@example.com");
        JSONObject userResponse = createUser(body, userExtension);
        User user = manager.frame(graph.getVertex(userResponse.getString("userGuid")), User.class);
        Device device = manager.frame(graph.getVertex(userResponse.getString("deviceGuid")), Device.class);

        String deviceGuid = Long.toString((Long) device.asVertex().getId());
        String userGuid = Long.toString((Long) user.asVertex().getId());

        // Send the PageView request
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, createBody(userGuid, deviceGuid), null, null, null);
        Response pageViewResponse = pageViewExtension.createNewVertex(ctx, graph).getJerseyResponse();
        JSONObject pageViewResponseBody = (JSONObject) pageViewResponse.getEntity();

        // Check that the request was successful
        Assert.assertEquals(200, pageViewResponse.getStatus());

        // Check a few details about the object created
        PageView pageview = manager.frame(graph.getVertex(pageViewResponseBody.getString("id")), PageView.class);
        Assert.assertEquals("http://google.com", pageview.getPageUrl());
        Assert.assertEquals(1761, pageview.getTabId());
        Assert.assertEquals(515, pageview.getWindowId());
        Assert.assertEquals((Long) 1367460869100L, pageview.getPageOpenTime());

        // Check that the device has a pageview and the pageview has a device
        Assert.assertEquals(true, device.getPageViews().iterator().hasNext());
        Assert.assertEquals(true, pageview.getDevice().iterator().hasNext());
        Assert.assertEquals(deviceGuid, Long.toString((Long) pageview.getDevice().iterator().next().asVertex().getId()));
    }

    @Test
    public void updatePageView() throws JSONException {
        System.out.println("Testing update existing page view");

        // Create a user to own the pageView
        JSONObject body = new JSONObject();
        body.put("emailAddress", "testUser@example.com");
        JSONObject userResponse = createUser(body, userExtension);
        User user = manager.frame(graph.getVertex(userResponse.getString("userGuid")), User.class);
        Device device = manager.frame(graph.getVertex(userResponse.getString("deviceGuid")), Device.class);

        String deviceGuid = Long.toString((Long) device.asVertex().getId());
        String userGuid = Long.toString((Long) user.asVertex().getId());

        // Send the PageView request
        body = createBody(userGuid, deviceGuid);
        body.put("pageUrl", "http://www.google.com");
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        Response pageViewResponse = pageViewExtension.createNewVertex(ctx, graph).getJerseyResponse();
        JSONObject pageViewResponseBody = (JSONObject) pageViewResponse.getEntity();
        Vertex vertex = graph.getVertex(pageViewResponseBody.getString("id"));

        // Send the update request
        body = new JSONObject();
        body.put("pageCloseTime", 1367460909100L);

        ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        pageViewResponse = pageViewExtension.updateVertex(ctx, graph, vertex).getJerseyResponse();
        pageViewResponseBody = (JSONObject) pageViewResponse.getEntity();

        // Check that the request was successful
        Assert.assertEquals(200, pageViewResponse.getStatus());

        // Check that the page close time was successful and nothing was removed
        PageView pageview = manager.frame(vertex, PageView.class);
        Assert.assertEquals((Long) 1367460869100L, pageview.getPageOpenTime());
        Assert.assertEquals((Long) 1367460909100L, pageview.getPageCloseTime());
    }

    @Test
    public void updateNullVertex() {
        System.out.println("Testing update page view with null vertex");

        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, null, null, null, null);
        Response pageViewResponse = pageViewExtension.updateVertex(ctx, graph, null).getJerseyResponse();

        Assert.assertEquals(500, pageViewResponse.getStatus());
    }

    @Test
    public void createPageViewInvalidUser() throws JSONException {
        System.out.println("Testing create new page view for invalid user");

        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, createBody("invalid", "invalid"), null, null, null);
        Response response = pageViewExtension.createNewVertex(ctx, graph).getJerseyResponse();

        Assert.assertEquals(400, response.getStatus());
    }

    private JSONObject createBody(String userGuid, String deviceGuid) throws JSONException {
        return new JSONObject()
                .put("type", "pageView")
                .put("tabId", 1761)
                .put("pageUrl", "http://google.com")
                .put("windowId", 515)
                .put("pageOpenTime", 1367460869100L)
                .put("clientVersion", "0.0.13")
                .put("userGuid", userGuid)
                .put("deviceGuid", deviceGuid);
    }
}
