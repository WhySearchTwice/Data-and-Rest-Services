package com.whysearchtwice.rexster.extension;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.groovy.Gremlin;
import com.tinkerpop.gremlin.java.GremlinPipeline;
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
import com.whysearchtwice.container.PageView;

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
            @ExtensionRequestParameter(name = "includeChildren", defaultValue = "false", description = "Whether or not to include all children of a search result") Boolean children){

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

        // Manipulate parameters
        Calendar pageOpenTime = Calendar.getInstance();
        pageOpenTime.setTimeInMillis(Long.parseLong(openTime));
        timeRange = adjustTimeRange(timeRange, units);

        List<PageView> pages = new ArrayList<PageView>();

        // Build the search
        Pipe pipe;
        if(!domain.equals("")) {
            pipe = Gremlin.compile("_().out('owns').out('viewed').out('under').has('domain', T.eq, '" + domain + "').back(2)");
        } else {
            pipe = Gremlin.compile("_().out('owns').out('viewed')");
        }
        
        // Perform search
        pipe.setStarts(new SingleIterator<Vertex>(user));
        for (Object result : pipe) {
            if (result instanceof Vertex) {
                Vertex v = (Vertex) result;
                addVertexToList(pages, v, successors, children);
            }
        }
        
        // Turn list into JSON to return
        String listAsJSON = "[";
        for(PageView pv : pages) {
            listAsJSON += pv.toString() + ", ";
        }
        listAsJSON = listAsJSON.substring(0, listAsJSON.length()-2);
        listAsJSON += "]";

        // Map to store the results
        Map<String, String> map = new HashMap<String, String>();
        map.put("results", listAsJSON);

        return ExtensionResponse.ok(map);
    }
    
    private void addVertexToList(List<PageView> pages, Vertex v, boolean successors, boolean children) {
        PageView pv = new PageView(v);
        pages.add(pv);

        // Add a reference to the parent and successors if edges exist
        for (Vertex neighbor : v.getVertices(Direction.OUT, "childOf")) {
            pv.setProperty("parentId", neighbor.getId().toString());
        }
        for (Vertex neighbor : v.getVertices(Direction.OUT, "successorTo")) {
            pv.setProperty("predecessorId", neighbor.getId().toString());
        }
        
        // Recursively search if children or successors should be included
        if(successors) {
            for(Vertex successor : v.getVertices(Direction.OUT, "predecessorTo")) {
                addVertexToList(pages, successor, successors, children);
            }
        }
        
        if(children) {
            for(Vertex successor : v.getVertices(Direction.OUT, "parentOf")) {
                addVertexToList(pages, successor, successors, children);
            }
        }
    }

    private int adjustTimeRange(int timeRange, String units) {
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
