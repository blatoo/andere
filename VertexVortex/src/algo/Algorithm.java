/*  Copyright (c) 2012  Andreas Spitz, spitz@stud.uni-heidelberg.de
 *
 *  This file is part of VertexVortex
 *
 *  VertexVortex is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  VertexVortex is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package algo;

import graph.Graph;

import java.util.concurrent.CyclicBarrier;


/**
 * Implements the FDSM algorithm using multiple threads for computation
 */
public class Algorithm {
    private Graph g;    // graph object this computation is done for

    /**
     * Default Constructor
     * @param graph graph object containing network data
     * @param settings object containing program settings
     */
    public Algorithm(Graph graph) {
        g = graph;
    }

    /**
     * Compute p-values based on FDSM method
     */
    public void compute() {
        SynchronizedLock synLock = new SynchronizedLock();          // Lock this thread will wait on until computation finishes
        Synchronizer sync = new Synchronizer(g);                    // synchronizer to assign work to individual threads
        CyclicBarrier cybar = new CyclicBarrier(g.threads, sync);   // cyclicBarrier used to synchronize threads
        g.initBeforeSampling();
        g.projection.doOnceBeforeSampling();
        
        // create the requested number of workthreads and run them
        AlgoThread[] t = new AlgoThread[g.threads];
        for (int i=0; i<g.threads; i++) {
            t[i] = new AlgoThread(g, sync, cybar, synLock);
            t[i].start();
        }
 
        if (g.threads > 0) synLock.await();    // sleep while waiting for workthreads to finish
        else System.out.println("Unable to compute with 0 threads. Aborting.");
    }
    
}