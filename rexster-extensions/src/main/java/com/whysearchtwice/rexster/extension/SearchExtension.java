package com.whysearchtwice.rexster.extension;

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
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;
import com.whysearchtwice.frames.PageView;
import com.whysearchtwice.utils.PageViewUtils;

@ExtensionNaming(name = SearchExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class SearchExtension extends AbstractParsleyExtension {
    public static final String NAME = "search";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET)
    @ExtensionDescriptor(description = "Get the results of a search")
    public ExtensionResponse searchVertices(
            @RexsterContext RexsterResourceContext context,
            @RexsterContext Graph graph,
            @ExtensionRequestParameter(name = "userGuid", defaultValue = "", description = "The user to retrieve information for") String userGuid,
            @ExtensionRequestParameter(name = "domain", defaultValue = "", description = "Retrieve pages with this domain") String domain,
            @ExtensionRequestParameter(name = "openTime", defaultValue = "", description = "The middle of a time based query") String openTime,
            @ExtensionRequestParameter(name = "timeRange", defaultValue = "30", description = "The range of time to search around openTime (openTime +- timeRange/2)") Integer timeRange,
            @ExtensionRequestParameter(name = "timeRangeUnits", defaultValue = "minutes", description = "hours, minutes, seconds") String units,
            @ExtensionRequestParameter(name = "includeSuccessors", defaultValue = "false", description = "Whether or not to include all successors to a search result") Boolean successors,
            @ExtensionRequestParameter(name = "includeChildren", defaultValue = "false", description = "Whether or not to include all children of a search result") Boolean children) {

        // Catch some errors
        if (openTime.equals("")) {
            return ExtensionResponse.error("You should specify an openTime");
        } else if (userGuid.equals("")) {
            return ExtensionResponse.error("You should specify a userGuid");
        }

        Vertex user = graph.getVertex(userGuid);
        if (user == null) {
            return ExtensionResponse.error("Invalid userGuid");
        }

        // Create the framed graph'
        FramedGraph<TitanGraph> manager = new FramedGraph<TitanGraph>((TitanGraph) graph);

        // Manipulate parameters
        Long openTimeL = Long.parseLong(openTime);
        timeRange = adjustTimeRange(timeRange / 2, units);

        JSONObject results = new JSONObject();

        // Build the search
        String gremlinQuery = "_().out('owns').out('viewed')";
        gremlinQuery += ".has('pageOpenTime', T.gte, " + (openTimeL - timeRange) + ")";
        gremlinQuery += ".has('pageOpenTime', T.lte, " + (openTimeL + timeRange) + ")";
        if (!domain.equals("")) {
            gremlinQuery += ".out('under').has('domain', T.eq, '" + domain + "').back(2)";
        }

        // Perform search
        try {
            @SuppressWarnings("unchecked")
            Pipe<Vertex, Vertex> pipe = (Pipe<Vertex, Vertex>) Gremlin.compile(gremlinQuery);
            pipe.setStarts(new SingleIterator<Vertex>(user));
            for (PageView pv : manager.frameVertices(pipe, PageView.class)) {
                addVertexToList(results, pv, successors, children, openTimeL, timeRange);
            }
        } catch (JSONException e) {
            return ExtensionResponse.error("Failed to create search results");
        }

        return ExtensionResponse.ok(results);
    }

    /**
     * Adds a vertex to the list of search results. Will recurse on children or
     * successors based on parameters.
     * 
     * @param pages
     * @param pv
     * @param successors
     * @param children
     * @throws JSONException
     */
    private void addVertexToList(JSONObject results, PageView pv, boolean successors, boolean children, long searchTime, int timeRange) throws JSONException {
        // Check that this PageView is within the time range. If not return
        if (!PageViewUtils.inTimeRange(pv, searchTime, timeRange)) {
            return;
        }

        // Add this vertex to the results list
        results.accumulate("results", PageViewUtils.asJSON(pv));

        // Recursively search if children or successors should be included
        if (successors) {
            for (PageView successor : pv.getSuccessors()) {
                addVertexToList(results, successor, successors, children, searchTime, timeRange);
            }

            for (PageView predecessor : pv.getPredecessors()) {
                addVertexToList(results, predecessor, successors, children, searchTime, timeRange);
            }
        }

        if (children) {
            for (PageView child : pv.getChildren()) {
                addVertexToList(results, child, successors, children, searchTime, timeRange);
            }

            for (PageView parent : pv.getParents()) {
                addVertexToList(results, parent, successors, children, searchTime, timeRange);
            }
        }
    }

    /**
     * Converts the timeRange to seconds from the given units
     * 
     * @param timeRange
     * @param units
     * @return int timeRange Timestamp converted to milliseconds
     */
    private int adjustTimeRange(int timeRange, String units) {
        // Convert to milliseconds
        timeRange *= 1000;

        if (units.equals("seconds")) {
            return timeRange * 1;
        } else if (units.equals("minutes")) {
            return timeRange * 1 * 60;
        } else if (units.equals("hours")) {
            return timeRange * 1 * 60 * 60;
        } else {
            return timeRange;
        }
    }
}
