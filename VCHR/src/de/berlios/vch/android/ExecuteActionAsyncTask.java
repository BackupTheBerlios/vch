package de.berlios.vch.android;

import static de.berlios.vch.android.BrowseActivity.TAG;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import de.berlios.vch.android.actions.Action;

public class ExecuteActionAsyncTask extends AsyncTask<Action, Integer, Void> {

    private ProgressDialog dialog;
    
    private Exception e;
    
    private Context ctx;
    
    private ExceptionHandler eh;
    
    public ExecuteActionAsyncTask(Context ctx, ExceptionHandler eh) {
        this.ctx = ctx;
        this.eh = eh;
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = ProgressDialog.show(ctx, "", ctx.getString(R.string.executing), true);
    }
    
    @Override
    protected Void doInBackground(Action... actions) {
        try {
            actions[0].execute();
        } catch (Exception e) {
            this.e = e;
        }
        return null;
    }
    
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        
        Log.i(TAG, "Action finished");
        
        if(!isCancelled()) {
            if(dialog.isShowing()) {
                dialog.dismiss();
            }
        }
        
        if(e != null) {
            eh.handleException(e);
        }
    }
    
    public interface ExceptionHandler {
        public void handleException(Exception e);
    }
}
