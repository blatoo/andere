package projection.abstr;

import graph.SimplexGraph;

/**
 * Projection for a simplex graph
 */
public abstract class SimplexProjection extends Projection {
    protected SimplexGraph g;
    
    public SimplexProjection(SimplexGraph graph) {
        this.g = graph;
    }
    
}
