package com.getbonkers.bottlecaps;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/6/12
 * Time: 2:20 PM
 * To change this template use File | Settings | File Templates.
 */

public interface AsyncNetworkDelegate {
    public void onOperationFailed(long operationID);
    public void onOperationComplete(long operationID);
    public void onOperationProgress(int progress);

    public void onCapReconcileComplete(long capID);
    public void onBatchCapReconcileComplete(long[] capIDs);

    public void onQueueRunComplete();
}
