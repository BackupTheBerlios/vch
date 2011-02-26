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

    private TaskCallback<Result> callback;

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
            if (callback != null) {
                callback.failed(e);
            }

            return;
        }

        finished(result);
        if (callback != null) {
            callback.success(result);
        }
    }

    protected abstract void handleException(Exception e);

    protected abstract void finished(Result result);

    public void setCallback(TaskCallback<Result> callback) {
        this.callback = callback;
    }

    public interface TaskCallback<Result> {
        public void success(Result r);

        public void failed(Exception e);
    }

}
