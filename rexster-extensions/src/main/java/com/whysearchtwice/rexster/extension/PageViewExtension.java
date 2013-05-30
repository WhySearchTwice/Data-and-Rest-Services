package com.whysearchtwice.rexster.extension;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.http.HttpStatus;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;
import com.whysearchtwice.frames.Device;
import com.whysearchtwice.frames.Domain;
import com.whysearchtwice.frames.PageView;
import com.whysearchtwice.utils.PageViewUtils;

@ExtensionNaming(name = PageViewExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class PageViewExtension extends AbstractParsleyExtension {
    public static final String NAME = "pageView";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "create a new vertex in the graph")
    public ExtensionResponse createNewVertex(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph) {
        FramedGraph<TitanGraph> manager = new FramedGraph<TitanGraph>((TitanGraph) graph);

        JSONObject attributes = context.getRequestObject();
        System.out.println("--- A NEW REQUEST ---");
        System.out.println(attributes.toString());

        // Check that the device is valid before doing anything else
        Device device;
        try {
            device = getDevice(attributes, manager);
        } catch (IllegalArgumentException e) {
            return ExtensionResponse.error("Invalid or missing deviceGuid", new IllegalArgumentException(), HttpStatus.SC_BAD_REQUEST);
        }

        // Create the new Vertex
        PageView newPageView = manager.addVertex(null, PageView.class);
        PageViewUtils.populatePageView(newPageView, manager, attributes);
        device.addPageView(newPageView);
        
        newPageView.getViewed().iterator().next().setPageOpenTime(newPageView.getPageOpenTime());

        // Return the id of the new Vertex
        Map<String, String> map = new HashMap<String, String>();
        map.put("id", newPageView.asVertex().getId().toString());

        // Link to the domain of the page URL
        try {
            if (attributes.has("pageUrl")) {
                newPageView.setDomain(findDomain(manager, extractDomain(attributes.getString("pageUrl"))));
            }
        } catch (URISyntaxException e) {
            return ExtensionResponse.error("URI Syntax Exception");
        } catch (JSONException e) {
            return ExtensionResponse.error("JSON Exception");
        }

        return ExtensionResponse.ok(map);
        // resp.setHeader("X-Chrome-Exponential-Throttling", disable)
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "update an existing vertex in the graph")
    public ExtensionResponse updateVertex(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph, @RexsterContext Vertex vertex) {
        if (vertex == null) {
            return ExtensionResponse.error("Invalid vertex, can not update");
        }

        FramedGraph<TitanGraph> manager = new FramedGraph<TitanGraph>((TitanGraph) graph);
        PageView pv = manager.frame(vertex, PageView.class);

        PageViewUtils.populatePageView(pv, manager, context.getRequestObject());

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();
        map.put("message", "vertex updated");

        return ExtensionResponse.ok(map);
    }

    private Device getDevice(JSONObject attributes, FramedGraph<TitanGraph> manager) {
        Device device = manager.frame(manager.getVertex(attributes.optString("deviceGuid")), Device.class);
        if (device.getType().equals("device")) {
            return device;
        } else {
            throw new NoSuchElementException();
        }
    }

    private String extractDomain(String pageUrl) throws URISyntaxException {
        try {
            URI uri = new URI(pageUrl);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            return "special";
        }

    }

    private Domain findDomain(FramedGraph<TitanGraph> manager, String domain) {
        Iterator<Vertex> iter = manager.getBaseGraph().getVertices("domain", domain).iterator();
        if (iter.hasNext()) {
            return manager.frame(iter.next(), Domain.class);
        } else {
            Domain d = manager.addVertex(null, Domain.class);
            d.setDomain(domain);
            d.setType("domain");
            return d;
        }
    }
}
