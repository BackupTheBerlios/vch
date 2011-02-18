package de.berlios.vch.android.actions;

import java.util.ArrayList;
import java.util.List;

public class CompositeAction extends Action {

    private List<Action> actions = new ArrayList<Action>();
    
    public void addAction(Action action) {
        actions.add(action);
    }
    
    @Override
    public void execute() throws Exception {
        for (Action action : actions) {
            action.execute();
        }
    }

}
