package com.whysearchtwice.rexster.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
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
import com.whysearchtwice.frames.PageView;
import com.whysearchtwice.utils.PageViewUtils;

@ExtensionNaming(name = CleanupExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class CleanupExtension extends AbstractParsleyExtension {
    public static final String NAME = "cleanup";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, path = "openTabs", method = HttpMethod.GET)
    @ExtensionDescriptor(description = "retrieve JSON object of open tabs for user or device")
    public ExtensionResponse retrieveOpenTabs(@RexsterContext Graph graph, @RexsterContext Vertex vertex) {
        if (vertex == null) {
            return ExtensionResponse.error("Invalid vertex");
        }

        FramedGraph<TitanGraph> manager = new FramedGraph<TitanGraph>((TitanGraph) graph);

        JSONObject results = new JSONObject();
        try {
            for (PageView pv : manager.frameVertices(doSearch(vertex), PageView.class)) {
                results.accumulate("results", PageViewUtils.asJSON(pv));
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
    public ExtensionResponse closeOpenTabs(@RexsterContext RexsterResourceContext context, @RexsterContext Graph graph, @RexsterContext Vertex vertex) {
        if (vertex == null) {
            return ExtensionResponse.error("Invalid vertex");
        }

        JSONObject attributes = context.getRequestObject();
        FramedGraph<TitanGraph> manager = new FramedGraph<TitanGraph>((TitanGraph) graph);

        int counter = 0;
        try {
            // Create a Set containing all the Guids to leave open
            JSONArray jsonGuidsArray = attributes.getJSONArray("sessionGuids");
            Set<String> guidsToLeaveOpen = new TreeSet<String>();
            for(int i = 0; i < jsonGuidsArray.length(); i++) {
                guidsToLeaveOpen.add(jsonGuidsArray.getString(i));
            }

            for (PageView pv : manager.frameVertices(doSearch(vertex), PageView.class)) {
                // If vertex is not in the exclude list, close it with -1
                if (guidsToLeaveOpen.contains(pv.getTabId())) {
                    // Do not close this tab
                } else {
                    pv.setPageCloseTime(-1L);
                    counter++;
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

    private Pipe<Vertex, Vertex> doSearch(Vertex startingVertex) throws Exception {
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

        @SuppressWarnings("unchecked")
        Pipe<Vertex, Vertex> pipe = (Pipe<Vertex, Vertex>) Gremlin.compile(gremlinQuery);
        pipe.setStarts(new SingleIterator<Vertex>(startingVertex));
        return pipe;
    }
}
