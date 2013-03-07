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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;

public class App {
    public static void main(String[] args) {
        try {
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

            // System.out.println(graphWrapper.toString());

            InputStream in = new ByteArrayInputStream(graphWrapper.toString().getBytes());
            OutputStream out = new FileOutputStream("titan.graphML");
            Graph graph = new TinkerGraph();

            GraphSONReader.inputGraph(graph, in);
            GraphMLWriter.outputGraph(graph, out);

            System.out.println("Graph exported to titan.graphML");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
