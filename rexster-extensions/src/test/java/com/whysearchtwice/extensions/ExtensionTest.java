package com.whysearchtwice.extensions;

import java.io.File;
import java.io.IOException;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.whysearchtwice.rexster.extension.CreateIndices;

public class ExtensionTest {
    protected TitanGraph graph;
    protected FramedGraph<TitanGraph> manager;

    @Before
    public void createGraph() {
        graph = TitanFactory.open("/tmp/titan");
        manager = new FramedGraph<TitanGraph>(graph);

        CreateIndices indicesExtension = new CreateIndices();
        RexsterResourceContext ctx = new RexsterResourceContext(null, null, null, new JSONObject(), null, null, null);
        indicesExtension.createIndices(ctx, graph);
    }

    @After
    public void deleteGraph() throws IOException {
        TitanGraph graph = manager.getBaseGraph();
        manager.shutdown();
        manager = null;
        if (graph.isOpen()) {
            graph.shutdown();
        }
        graph = null;

        File directory = new File("/tmp/titan");
        if (directory.exists()) {
            delete(directory);
        }
    }

    protected void delete(File file) throws IOException {
        if (file.isDirectory()) {
            if (file.list().length == 0) {
                file.delete();
            } else {
                for (String temp : file.list()) {
                    File fileDelete = new File(file, temp);
                    delete(fileDelete);
                }

                if (file.list().length == 0) {
                    file.delete();
                }
            }
        } else {
            file.delete();
        }
    }
}
