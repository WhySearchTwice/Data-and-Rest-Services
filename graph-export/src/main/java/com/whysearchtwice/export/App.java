package com.whysearchtwice.export;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;

public class App {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Expected two arguments -- inFilename outFilename");
            return;
        }

        try {
            JSONObject json = loadAndTransformJson(args[0]);
            Graph graph = createTinkerGraph(json);
            writeGraphToFile(args[1], graph);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Graph exported to titan.graphML");
    }

    private static JSONObject loadAndTransformJson(String filename) throws Exception {
        File inFile = new File("vertices.json");

        JSONArray edges = new JSONArray();
        JSONArray vertices = new JSONArray();

        BufferedReader br = new BufferedReader(new FileReader(inFile));
        String line;
        while ((line = br.readLine()) != null) {
            JSONObject json = new JSONObject(line);

            if (json.has("_inE")) {
                JSONArray inE = json.getJSONArray("_inE");

                for (int i = 0; i < inE.length(); i++) {
                    JSONObject edge = inE.getJSONObject(i);
                    edge.put("_inV", json.get("_id"));
                    edges.put(edge);
                }

                json.remove("_inE");
            }

            if (json.has("_outE")) {
                // JSONArray outE = json.getJSONArray("_outE");
                //
                // for (int i = 0; i < outE.length(); i++) {
                // JSONObject edge = outE.getJSONObject(i);
                // edge.put("_outV", json.get("_id"));
                // edges.put(edge);
                // }

                json.remove("_outE");
            }

            vertices.put(json);
        }
        br.close();

        JSONObject graphJSON = new JSONObject();
        graphJSON.put("mode", "NORMAL");
        graphJSON.put("vertices", vertices);
        graphJSON.put("edges", edges);
        JSONObject graphWrapper = new JSONObject();
        graphWrapper.put("graph", graphJSON);

        return graphWrapper;
    }

    private static Graph createTinkerGraph(JSONObject graphWrapper) throws IOException {
        Graph graph = new TinkerGraph();

        InputStream in = new ByteArrayInputStream(graphWrapper.toString().getBytes());
        GraphSONReader.inputGraph(graph, in);

        return graph;
    }

    private static void writeGraphToFile(String outputFileName, Graph graph) throws Exception {
        OutputStream out = new FileOutputStream(outputFileName);
        GraphMLWriter.outputGraph(graph, out);
    }
}
