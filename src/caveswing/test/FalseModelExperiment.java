package caveswing.test;

import agents.evo.EvoAgent;
import caveswing.core.CaveGameState;
import caveswing.core.CaveSwingParams;
import caveswing.util.ViewUtil;
import caveswing.view.CaveView;
import evodef.DefaultMutator;
import evodef.EvoAlg;
import ga.SimpleRMHC;
import ggi.core.SimplePlayerInterface;
import plot.LineChart;
import plot.LineChartAxis;
import plot.LineGroup;
import utilities.JEasyFrame;
import utilities.StatSummary;

import java.awt.*;
import java.util.ArrayList;

public class FalseModelExperiment {

    public static void main(String[] args) throws Exception {
        ArrayList<StatSummary> stats = new ArrayList<>();
        int nTrials = 20;
        double[] hooke = {0, 0.2, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0};
        for (double h : hooke) {
            StatSummary ss = runTrial(1.0, h, nTrials);
            System.out.println(h);
            System.out.println(ss);
            stats.add(ss);
        }
        LineGroup lineGroup = new LineGroup();
        lineGroup.stats = stats;
        LineChart chart = new LineChart();
        chart.addLineGroup(lineGroup);
        chart.yAxis = new LineChartAxis(new double[]{-5000, 0, 5000, 10000, 15000});
        chart.xAxis = new LineChartAxis(hooke);
        // chart.autoScale();
        new JEasyFrame(chart, "Score versus hooke");
    }

    public static StatSummary runTrial(double gravityFactor, double hookesLaw, int nTrials) {
        StatSummary ss = new StatSummary("False Model");
        for (int i=0; i<nTrials; i++) {
            SimplePlayerInterface player = getEvoAgent();

            CaveSwingParams params = CoolTestParams.getParams();
            params.nAnchors *= 2;

            CaveSwingParams falseParams = params.copy();
            falseParams.gravity.y *= gravityFactor;
            falseParams.hooke *= hookesLaw;

            falseParams.lossFactor = 1.0;

//            System.out.println(params.gravity);
//            System.out.println(falseParams.gravity);

            CaveGameState gameState = new CaveGameState().setParams(params).setup();
            boolean useFalseModel = true;

            while (!gameState.isTerminal()) {
                // get the action from the player, update the game state, and show a view


                CaveGameState falseState = (CaveGameState) gameState.copy();
                falseState.setParams(falseParams);

                int action = useFalseModel ?
                        player.getAction(falseState, 0) :
                        player.getAction(gameState.copy(), 0);
                // recall the action array is needed for generality for n-player games
                int[] actions = new int[]{action};
                gameState.next(actions);

            }
            ss.add(gameState.getScore());
        }

        return ss;
    }

    public static SimplePlayerInterface getEvoAgent() {
        int nResamples = 1;
        DefaultMutator mutator = new DefaultMutator(null);
        // setting to true may give best performance
        // mutator.totalRandomChaosMutation = true;
        mutator.flipAtLeastOneValue = true;
        mutator.pointProb = 5;
        SimpleRMHC simpleRMHC = new SimpleRMHC();
        simpleRMHC.setSamplingRate(nResamples);
        simpleRMHC.setMutator(mutator);
        EvoAlg evoAlg = simpleRMHC;
        // evoAlg = new SlidingMeanEDA();
        int nEvals = 20;
        int seqLength = 20;
        EvoAgent evoAgent = new EvoAgent().setEvoAlg(evoAlg, nEvals).setSequenceLength(seqLength);
        evoAgent.setUseShiftBuffer(true);
        return evoAgent;
    }

}
