package projection.abstr;

/**
 * Abstract projection template.
 * Projections are assigned to a graph object before computation and serve two purposes:
 * 1. They implement the methods that are used prior to the sampling and after each sampling step
 *    for the computation.
 * 2. They Store the computed data
 * 
 * Due to this structure, the computation is independent of the used graph, so in order to implement
 * a new computation method for projection weights, only a new child class of projection has to be
 * created and assigned to the graph.
 */
public abstract class Projection {
    public String weightType;
    
    /**
     * This method must implement any initialization of the object that has to
     * occurr before the computation begins, such as reserving memory for data
     * structures. It is called ONCE before computation begins.
     */
    public abstract void doOnceBeforeSampling();
    
    /**
     * This method must implement any computation that needs to be done
     * for each actor before the sampling beings, e.g. computing initial
     * co-occurrence
     * @param a ID of the actor the computation is done for
     */
    public abstract void doPerActorBeforeSampling(int a);
    
    /**
     * This method must implement any computation that needs to be done
     * for each actor after each sampling step is complete.
     * @param ac1 ID of the actor the computation is done for
     */
    public abstract void doPerActorDuringSampling(int ac1);
   
    /**
     * Computes the co-occurrence of 2 actors by comparing their adjacency lists
     * Input arrays are REQUIRED to be sorted in ascending order
     * 
     * @param adj1 sorted integer array containing all events connected to actor1
     * @param adj2 sorted integer array containing all events connected to actor2
     * @return co-occurrence of actor1 and actor2
     */
    protected static int computeCooc(int[] adj1, int[] adj2) {
        int cooc = 0;
        int pos1 = 0;
        int pos2 = 0;
        while ( (pos1 < adj1.length) && (pos2 < adj2.length) ) {  // while neither list is empty
            if (adj1[pos1] < adj2[pos2]) pos1++;                  // if one list has a smaller element at the first
            else if (adj1[pos1] > adj2[pos2]) pos2++;             // position: advance in that list
            else {              // otherwise, both lists have an identical element
                cooc++;         // so increase cooc
                pos1++;         // and advance in both lists
                pos2++;
            }
        }
        return cooc;
    }
    
}
