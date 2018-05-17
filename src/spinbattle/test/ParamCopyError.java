package spinbattle.test;

import spinbattle.actuator.SourceTargetActuator;
import spinbattle.core.Planet;
import spinbattle.core.SpinGameState;
import spinbattle.params.Constants;
import spinbattle.params.SpinBattleParams;

public class ParamCopyError {
    public static void main(String[] args) {
        // find the launch error

        SpinBattleParams params = new SpinBattleParams();
        // params.transitSpeed = 0;


        SpinGameState gameState = new SpinGameState().setParams(params).setPlanets();

        SourceTargetActuator actuator = new SourceTargetActuator().setPlayerId(0);

        gameState.actuators[0] = actuator;

        // now launch a transit

        gameState.planets.get(0).ownedBy = Constants.playerOne;



        SpinBattleParams params2 = new SpinBattleParams();
        params2.transitSpeed = 0;
        Planet source = gameState.planets.get(0);
        Planet target = gameState.planets.get(1);

        gameState.setParams(params2);
        // source.setParams(params2);
        gameState.planets.get(0).getTransporter().launch(source.position, target.position, Constants.playerOne, gameState);


    }
}
