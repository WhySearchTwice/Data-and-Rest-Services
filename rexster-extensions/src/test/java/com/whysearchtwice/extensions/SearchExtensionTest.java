package com.whysearchtwice.extensions;

import com.tinkerpop.rexster.RexsterResourceContext;
import com.whysearchtwice.frames.Device;
import com.whysearchtwice.frames.PageView;
import com.whysearchtwice.frames.User;
import com.whysearchtwice.rexster.extension.CleanupExtension;
import com.whysearchtwice.rexster.extension.PageViewExtension;
import com.whysearchtwice.rexster.extension.SearchExtension;
import com.whysearchtwice.rexster.extension.UserExtension;
import org.junit.Assert;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

public class SearchExtensionTest extends ExtensionTest {
    private static UserExtension userExtension;
    private static SearchExtension searchExtension;
    private static CleanupExtension cleanupExtension;
    private static PageViewExtension pageViewExtension;

    @BeforeClass
    public static void setupClass() {
        userExtension = new UserExtension();
        searchExtension = new SearchExtension();
        cleanupExtension = new CleanupExtension();
        pageViewExtension = new PageViewExtension();
    }

    @Test
    public void basicSearch() throws JSONException {
        System.out.println("Testing retrieve open tabs");

        // Get the current time to use as a search starting point
        String openRange = Long.toString(System.currentTimeMillis());

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
                .put("pageOpenTime", System.currentTimeMillis())
                .put("clientVersion", "0.0.13")
                .put("userGuid", userGuid)
                .put("deviceGuid", deviceGuid);
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        JSONObject pageViewResponse = (JSONObject) pageViewExtension.createNewVertex(ctx, graph).getJerseyResponse().getEntity();
        PageView pv1 = manager.frame(graph.getVertex(pageViewResponse.getString("id")), PageView.class);

        body.put("tabId", 1234)
                .put("pageUrl", "http://github.com")
                .put("pageOpenTime", System.currentTimeMillis());
        ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        pageViewResponse = (JSONObject) pageViewExtension.createNewVertex(ctx, graph).getJerseyResponse().getEntity();
        PageView pv2 = manager.frame(graph.getVertex(pageViewResponse.getString("id")), PageView.class);

        // Close these tabs so they show up in the search
        body = new JSONObject();
        ctx = new RexsterResourceContext(null, null, null, body, null, null, null);
        cleanupExtension.closeOpenTabs(ctx, graph, user.asVertex());

        // Get the time again to use as the search ending point
        String closeRange = Long.toString(System.currentTimeMillis());

        // Perform basic search and validate results
        response = (JSONObject) searchExtension.searchVertices(graph, userGuid, "", openRange, closeRange, false, false).getJerseyResponse().getEntity();
        Assert.assertEquals(2, response.getJSONArray("results").length());
    }
}
