package com.whysearchtwice.rexster.extension;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.groovy.Gremlin;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.util.iterators.SingleIterator;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;
import com.whysearchtwice.container.PageView;

@ExtensionNaming(name = CleanupExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class CleanupExtension extends AbstractParsleyExtension {
    public static final String NAME = "cleanup";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, path = "openTabs", method = HttpMethod.GET)
    @ExtensionDescriptor(description = "retrieve JSON object of open tabs for user or device")
    public ExtensionResponse retrieveOpenTabs(@RexsterContext Vertex vertex) {
        if (vertex == null) {
            return ExtensionResponse.error("Invalid vertex");
        }

        JSONObject results = new JSONObject();
        try {
            for (Object result : doSearch(vertex)) {
                if (result instanceof Vertex) {
                    results.accumulate("results", new PageView((Vertex) result).exportJson());
                }
            }
        } catch (JSONException e) {
            return ExtensionResponse.error("Failed to create JSON Result");
        } catch (Exception e) {
            return ExtensionResponse.error(e.getMessage());
        }

        return ExtensionResponse.ok(results);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, path = "closeTabs", method = HttpMethod.POST)
    @ExtensionDescriptor(description = "close tabs open on a user or device")
    public ExtensionResponse closeOpenTabs(@RexsterContext RexsterResourceContext context, @RexsterContext Vertex vertex) {
        if (vertex == null) {
            return ExtensionResponse.error("Invalid vertex");
        }

        JSONObject attributes = context.getRequestObject();

        int counter = 0;
        try {
            for (Object result : doSearch(vertex)) {
                if (result instanceof Vertex) {
                    Vertex resultVertex = (Vertex) result;

                    // If vertex is not in the exclude list, close it with -1
                    String tabId = resultVertex.getProperty("tabId").toString();
                    if (attributes.has(tabId) && attributes.getString(tabId).equals((String) resultVertex.getProperty("pageUrl"))) {
                        // Do not close this tab
                    } else {
                        resultVertex.setProperty("pageCloseTime", -1);
                        counter++;
                    }
                }
            }
        } catch (JSONException e) {
            return ExtensionResponse.error("Failed to create JSON Result");
        } catch (Exception e) {
            return ExtensionResponse.error(e.getMessage());
        }

        Map<String, String> response = new HashMap<String, String>();
        response.put("message", counter + " open pages closed");
        return ExtensionResponse.ok(response);
    }

    private Pipe doSearch(Vertex startingVertex) throws Exception {
        String gremlinQuery = null;
        String type = (String) startingVertex.getProperty("type");

        // The search may start from a user or device. Adjust query accordingly.
        if (type.equals("user")) {
            gremlinQuery = "_().out('owns').out('viewed').has('pageCloseTime', null)";
        } else if (type.equals("device")) {
            gremlinQuery = "_().out('viewed').has('pageCloseTime', null)";
        } else {
            throw new Exception("Vertex is not a user or device");
        }

        Pipe pipe = Gremlin.compile(gremlinQuery);
        pipe.setStarts(new SingleIterator<Vertex>(startingVertex));
        return pipe;
    }
}
