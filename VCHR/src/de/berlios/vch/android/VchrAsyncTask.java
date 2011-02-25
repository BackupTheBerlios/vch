package de.berlios.vch.android;

import static de.berlios.vch.android.BrowseActivity.TAG;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public abstract class VchrAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private ProgressDialog dialog;

    private Exception e;

    protected Context ctx;

    private int progressMessageId;

    public VchrAsyncTask(Context ctx) {
        this(ctx, R.string.executing);
    }

    public VchrAsyncTask(Context ctx, int progressMessageId) {
        this.ctx = ctx;
        this.progressMessageId = progressMessageId;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        try {
            dialog = ProgressDialog.show(ctx, "", ctx.getString(progressMessageId), true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    protected Result doInBackground(Params... params) {
        try {
            return doTheWork(params);
        } catch (Exception e) {
            this.e = e;
        }
        return null;
    }

    protected abstract Result doTheWork(Params... params) throws Exception;

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);

        Log.i(TAG, "Task finished");

        if (!isCancelled()) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

        if (e != null) {
            handleException(e);
            return;
        }

        finished(result);
    }

    protected abstract void handleException(Exception e);

    protected abstract void finished(Result result);

    public interface ExceptionHandler {
        public void handleException(Exception e);
    }
}
