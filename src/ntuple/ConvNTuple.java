package ntuple;

// a convolutional version of the N-Tuple

// it actually uses a standard N-Tuple, but via a convolutional expansion of the input


import evodef.SearchSpace;
import evodef.SolutionEvaluator;
import utilities.ElapsedTimer;
import utilities.Picker;
import utilities.StatSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class ConvNTuple implements BanditLandscapeModel {

    double epsilon = 0.1;

    double defaultMeanEstimate = 0;

    public static void main(String[] args) {
        int w = 20, h = 20;
        int filterWidth = 6, filterHeight = 6;
        ConvNTuple convNTuple = new ConvNTuple().setImageDimensions(w, h);
        convNTuple.setFilterDimensions(filterWidth, filterHeight);
        convNTuple.setMValues(2).setStride(2);

        convNTuple.makeIndicies();

        System.out.println("Address space size: " + convNTuple.addressSpaceSize());
        // System.out.println("Mean of empty summary: " + new StatSummary().mean());

        // now put some random data in to it
        ElapsedTimer timer = new ElapsedTimer();

        int nPoints = 100;
        int nDims = w * h;
        Random random = new Random();
        for (int i=0; i<nPoints; i++) {
            double p = random.nextDouble();
            // now make a random array with this P(x==1)
            int[] x = new int[nDims];
            int tot = 0;
            for (int j=0; j<nDims; j++) {
                int z = random.nextDouble() < p ? 1 : 0;
                x[j] = z;
                tot += z;
            }
            convNTuple.addPoint(x, tot);
        }

        // now iterate over all the values in there
        System.out.println("Training finished: ");
        System.out.println(timer);

        convNTuple.report();
        System.out.println(timer);

        // now make some random points

    }

    boolean verbose = false;
    public void report(SolutionEvaluator evaluator) {
        System.out.println();
        System.out.println("Indexes used: " + ntMap.size());
        System.out.println();
        if (!verbose) return;
        StatSummary errorStats = new StatSummary();
        RankCorrelation rankCorrelation = new RankCorrelation();
        int index = 0;
        for (int[] p : solutions) {
            // System.out.println(countOnes(p) + " : " + getMeanEstimate(p) + " : " + Arrays.toString(p));
            double fitness = evaluator.evaluate(p);
            double estimate = getMeanEstimate(p);
            errorStats.add(Math.abs(fitness - estimate));
            rankCorrelation.add(index++, fitness, estimate);
        }
        System.out.println("Error Stats");
        System.out.println(errorStats);
        System.out.println();
        rankCorrelation.rankCorrelation();
    }

    public void report() {
        StatSummary errorStats = new StatSummary();
        for (int[] p : solutions) {
            // System.out.println(countOnes(p) + " : " + getMeanEstimate(p) + " : " + Arrays.toString(p));
            errorStats.add(Math.abs(countOnes(p) - getMeanEstimate(p)));
        }
        System.out.println("Error Stats");
        System.out.println(errorStats);
        System.out.println("Indexes used: " + ntMap.size());
    }

    private int countOnes(int[] a) {
        int tot = 0;
        for (int x : a) tot += x;
        return tot;
    }

    // self explanatory variables
    int imageWidth, imageHeight;

    int filterWidth, filterHeight;

    // vecLength should be equal to imageWidth * imageHeight
    // images will often be passed as 1-d vectors, which is faster
    int vecLength;

    //
    int stride;

    int mValues; // number of values possible in each image pixel / tile

    HashMap<Double, StatSummary> ntMap;

    // store every solution ever sampled, ready to return the best one when ready
    // since the fitness estimate is always being updated, best to do all these at the end

    ArrayList<int[]> solutions;
    private Picker<int[]> picker;



    int nSamples;
    SearchSpace searchSpace;

    public ConvNTuple reset() {
        // avoid taking log of zero
        nSamples = 1;
        ntMap = new HashMap<>();
        solutions = new ArrayList<>();
        picker = new Picker<>();
        return this;
    }


    public BanditLandscapeModel setSearchSpace(SearchSpace searchSpace) {
        this.searchSpace = searchSpace;
        return this;
    }

    @Override
    public SearchSpace getSearchSpace() {
        return searchSpace;
    }

    public ConvNTuple setImageDimensions(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        return this;
    }

    public ConvNTuple setFilterDimensions(int filterWidth, int filterHeight) {
        this.filterWidth = filterWidth;
        this.filterHeight = filterHeight;
        return this;
    }

    public ConvNTuple setMValues(int mValues) {
        this.mValues = mValues;
        return this;
    }

    public ConvNTuple setStride(int stride) {
        this.stride = stride;
        return this;
    }

    private double address(int[] image, int[] index) {
        // System.out.println(image.length);
        double prod = 1;
        double addr = 0;
        for (int i : index) { //  i<tuple.length; i++) {
            // System.out.println(i);
            addr += prod * image[i];
            prod *= mValues;
        }
        return addr;
    }

    ArrayList<int[]> indices;

    public ConvNTuple makeIndicies() {
        indices = new ArrayList<>();
        for (int i = 0; i <= imageWidth - filterWidth; i += stride) {
            for (int j = 0; j <= imageHeight - filterHeight; j += stride) {
                // i and j are the start points in the image
                // but we need to iterate over all the filter positions
                // for each start point we create an array of filter sample points in image vector coordinates
                // first calculate the image x,y points, then map them to the one-d vector coords
                int[] a = new int[filterWidth * filterHeight];
                int filterIndex = 0;
                for (int k = 0; k < filterWidth; k++) {
                    for (int l = 0; l < filterHeight; l++) {
                        int x = i + k;
                        int y = j + l;
                        int ix = x + imageWidth * y;
                        a[filterIndex] = ix;
                        filterIndex++;
                    }
                }
                System.out.println(Arrays.toString(a));
                indices.add(a);
            }
        }
        // reset the stats after making new indices
        reset();
        System.out.println("Made index vectors: " + indices.size());
        return this;
    }

    public double addressSpaceSize() {
        // return SearchSpaceUtil.size(searchSpace);
        double size = 1;
        for (int i = 0; i < filterWidth * filterHeight; i++) {
            size *= mValues;
        }
        return size;
    }


    @Override
    public void addPoint(int[] p, double value) {
        // iterate over all the indices
        // calculate an address for each one

        // System.out.println(" ADDING A POINT !!!!!!!!!!!!!!!!!!!!!!");
        for (int[] index : indices) {
            double address = address(p, index);
            StatSummary ss = getStatsForceCreate(address);
            ss.add(value);
        }
        solutions.add(p);
        picker.add(value, p);
        nSamples++;
    }

    public int nSamples() {
        return nSamples;
    }

    public int nEntries() {
        return ntMap.keySet().size();
    }

    public StatSummary getStatsForceCreate(double address) {
        StatSummary ss = ntMap.get(address);
        if (ss == null) {
            ss = new StatSummary();

            ntMap.put(address, ss);
        }
        return ss;
    }


    @Override
    public int[] getBestSolution() {
        throw new RuntimeException("Not yet implemented");
    }

    public int[] getBestOfAllSampled() {
        Picker<int[]> picker = new Picker<>();
        for (int[] a : solutions) {
            // System.out.println(getMeanEstimate(a) + " : " + Arrays.toString(a));
            picker.add(getMeanEstimate(a), a);
        }
        // System.out.println();
        return picker.getBest();
    }

    @Override
    public int[] getBestOfSampled() {
        return picker.getBest();
    }

    @Override
    public int[] getBestOfSampledPlusNeighbours(int nNeighbours) {
        throw new RuntimeException("Not yet implemented");
    }

    public boolean useWeightedMean = false;

    @Override
    public Double getMeanEstimate(int[] x) {
        StatSummary ssTot = new StatSummary("Summary stats exploit");
        for (int[] index : indices) {
            double address = address(x, index);
            StatSummary ss = ntMap.get(address);
            if (ss != null && ss.n() > 0) {
                if (useWeightedMean) {
                    ssTot.add(ss);
                } else {
                    ssTot.add(ss.mean());
                }
            }
        }
        // need to cope with an empty summary
        if (ssTot.n() == 0) ssTot.add(defaultMeanEstimate);
//        System.out.println(ssTot);
//        System.out.println();
        return ssTot.mean();
    }

    public StatSummary getNoveltyStats(int[] x) {
        StatSummary ssTot = new StatSummary();
        for (int[] index : indices) {
            double address = address(x, index);
            StatSummary ss = ntMap.get(address);
            if (ss != null) {
                ssTot.add(ss.n());
            } else {
                ssTot.add(0);
            }
        }
        return ssTot;
    }

    @Override
    public double getExplorationEstimate(int[] x) {
        // StatSummary ssTot = new StatSummary();
        StatSummary exploreStats = new StatSummary();
        for (int[] index : indices) {
            double address = address(x, index);
            StatSummary ss = ntMap.get(address);
            if (ss != null) {
                exploreStats.add(explore(ss.n()));
            } else {
                exploreStats.add(explore(0));
            }
        }
        // System.out.println("Explore stats n() " + exploreStats.n() + " : " + exploreStats.mean());
        return exploreStats.mean();
    }

    double k = 2.0;

    public double explore(int n_i) {
        return k * Math.sqrt(Math.log(nSamples) / (epsilon + n_i));
    }

}

