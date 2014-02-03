package tool;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;

public class Hypergeometric {
    private JDKRandomGenerator r;
    
    public Hypergeometric(int randomseed) {
        this.r = new JDKRandomGenerator();      // create new pseudo random number generator
        this.r.setSeed(randomseed);             // and seed it
    }
    
    public double getHyperGeomSimilarity(int deg1, int deg2, int numbercommonneighs, int numberrightsidenodes) {
        double similarity = 0;
        int mindeg = Math.min(deg1, deg2);
        HypergeometricDistribution hyp;
        
        for(int i = numbercommonneighs; i <= mindeg; i++) {
             hyp = new HypergeometricDistribution(r, numberrightsidenodes, deg1, deg2);
             similarity += hyp.probability(i);
        }
        
        similarity = -Math.log(similarity); // Goldberg and Roth "Assessing experimentally derived interactions in a small world" 
        return similarity;
    }
}