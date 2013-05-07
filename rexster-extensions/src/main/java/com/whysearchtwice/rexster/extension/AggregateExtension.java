package com.whysearchtwice.rexster.extension;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.groovy.Gremlin;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.util.iterators.SingleIterator;
import com.tinkerpop.rexster.extension.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Arrays;
import java.util.List;

@ExtensionNaming(name = AggregateExtension.NAME, namespace = AbstractParsleyExtension.NAMESPACE)
public class AggregateExtension extends AbstractParsleyExtension {
    public static final String NAME = "aggregate";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, method = HttpMethod.GET)
    @ExtensionDescriptor(description = "find the number of times a user has been to a domain")
    public ExtensionResponse getDomainCount(@RexsterContext Vertex vertex,
                                            @ExtensionRequestParameter(name = "domain", defaultValue = "", description = "Domain to retrieve a count for") String domain) {

        // Catch some errors
        if (vertex == null || !vertex.getProperty("type").equals("user")) {
            return ExtensionResponse.error("Must extend a valid user vertex");
        }

        if (domain.length() == 0) {
            return ExtensionResponse.error("Must include a valid domain name");
        }

        JSONObject results = new JSONObject();
        List<String> ignoredProperties = Arrays.asList(new String[]{"domain", "_id", "_type", "_outV", "inV", "_label"});

        try {
            @SuppressWarnings("unchecked")
            Pipe<Vertex, Edge> pipe = (Pipe<Vertex, Edge>) Gremlin.compile("_().outE('domainVisitCount').has('domain', T.eq, '" + domain + "')");
            pipe.setStarts(new SingleIterator<Vertex>(vertex));
            for (Edge edge : pipe) {
                // Should only get here once, convert to JSON object and return
                for (String propertyKey : edge.getPropertyKeys()) {
                    if (ignoredProperties.contains(propertyKey)) {
                        continue;
                    }
                    results.put(propertyKey, edge.getProperty(propertyKey));
                }

                return ExtensionResponse.ok(results);
            }
        } catch (JSONException e) {
            return ExtensionResponse.error("Failed to create search results");
        }

        return ExtensionResponse.error("Could not find domain for this user");
    }
}
