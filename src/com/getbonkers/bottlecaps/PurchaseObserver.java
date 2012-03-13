package com.getbonkers.bottlecaps;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 3/12/12
 * Time: 7:16 PM
 * To change this template use File | Settings | File Templates.
 */

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Handler;
import android.util.Log;

import com.getbonkers.bottlecaps.BillingService.*;

import java.lang.reflect.Method;

public abstract class PurchaseObserver {
    private static final String TAG = "PurchaseObserver";
    private final Activity mActivity;
    private final Handler mHandler;
    private Method mStartIntentSender;
    private Object[] mStartIntentSenderArgs = new Object[5];
    private static final Class[] START_INTENT_SENDER_SIG = new Class[] {
            IntentSender.class, Intent.class, int.class, int.class, int.class
    };

    public PurchaseObserver(Activity activity, Handler handler) {
        mActivity = activity;
        mHandler = handler;
        initCompatibilityLayer();
    }

    public abstract void onBillingSupported(boolean supported);


    public abstract void onPurchaseStateChange(Consts.PurchaseState purchaseState,
                                               String itemId, int quantity, long purchaseTime, String developerPayload);


    public abstract void onRequestPurchaseResponse(RequestPurchase request,
                                                   Consts.ResponseCode responseCode);


    public abstract void onRestoreTransactionsResponse(RestoreTransactions request,
                                                       Consts.ResponseCode responseCode);

    private void initCompatibilityLayer() {
        try {
            mStartIntentSender = mActivity.getClass().getMethod("startIntentSender",
                    START_INTENT_SENDER_SIG);
        } catch (SecurityException e) {
            mStartIntentSender = null;
        } catch (NoSuchMethodException e) {
            mStartIntentSender = null;
        }
    }

    void startBuyPageActivity(PendingIntent pendingIntent, Intent intent) {
        if (mStartIntentSender != null) {
            // This is on Android 2.0 and beyond.  The in-app buy page activity
            // must be on the activity stack of the application.
            try {
                // This implements the method call:
                // mActivity.startIntentSender(pendingIntent.getIntentSender(),
                //     intent, 0, 0, 0);
                mStartIntentSenderArgs[0] = pendingIntent.getIntentSender();
                mStartIntentSenderArgs[1] = intent;
                mStartIntentSenderArgs[2] = Integer.valueOf(0);
                mStartIntentSenderArgs[3] = Integer.valueOf(0);
                mStartIntentSenderArgs[4] = Integer.valueOf(0);
                mStartIntentSender.invoke(mActivity, mStartIntentSenderArgs);
            } catch (Exception e) {
                Log.e(TAG, "error starting activity", e);
            }
        } else {
            // This is on Android version 1.6. The in-app buy page activity must be on its
            // own separate activity stack instead of on the activity stack of
            // the application.
            try {
                pendingIntent.send(mActivity, 0 /* code */, intent);
            } catch (CanceledException e) {
                Log.e(TAG, "error starting activity", e);
            }
        }
    }

    /**
     * Updates the UI after the database has been updated.  This method runs
     * in a background thread so it has to post a Runnable to run on the UI
     * thread.
     * @param purchaseState the purchase state of the item
     * @param itemId a string identifying the item
     * @param quantity the quantity of items in this purchase
     */
    void postPurchaseStateChange(final Consts.PurchaseState purchaseState, final String itemId,
                                 final int quantity, final long purchaseTime, final String developerPayload) {
        mHandler.post(new Runnable() {
            public void run() {
                onPurchaseStateChange(
                        purchaseState, itemId, quantity, purchaseTime, developerPayload);
            }
        });
    }
}
