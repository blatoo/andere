package projection.abstr;

import graph.DuplexGraph;

/**
 * Projection for a duplex graph
 */
public abstract class DuplexProjection extends Projection {
    protected DuplexGraph g;
    
    public DuplexProjection(DuplexGraph graph) {
        this.g = graph;
    }
    
}
