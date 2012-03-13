package com.getbonkers.bottlecaps;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 3/12/12
 * Time: 7:18 PM
 * To change this template use File | Settings | File Templates.
 */

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.getbonkers.bottlecaps.BillingService.*;

/**
 * This class contains the methods that handle responses from Android Market.  The
 * implementation of these methods is specific to a particular application.
 * The methods in this example update the database and, if the main application
 * has registered a {@llink PurchaseObserver}, will also update the UI.  An
 * application might also want to forward some responses on to its own server,
 * and that could be done here (in a background thread) but this example does
 * not do that.
 *
 * You should modify and obfuscate this code before using it.
 */
public class ResponseHandler {
    private static final String TAG = "ResponseHandler";

    private static PurchaseObserver sPurchaseObserver;

    public static synchronized void register(PurchaseObserver observer) {
        sPurchaseObserver = observer;
    }

    public static synchronized void unregister(PurchaseObserver observer) {
        sPurchaseObserver = null;
    }

    public static void checkBillingSupportedResponse(boolean supported) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.onBillingSupported(supported);
        }
    }

    /**
     * Starts a new activity for the user to buy an item for sale. This method
     * forwards the intent on to the PurchaseObserver (if it exists) because
     * we need to start the activity on the activity stack of the application.
     *
     * @param pendingIntent a PendingIntent that we received from Android Market that
     *     will create the new buy page activity
     * @param intent an intent containing a request id in an extra field that
     *     will be passed to the buy page activity when it is created
     */
    public static void buyPageIntentResponse(PendingIntent pendingIntent, Intent intent) {
        if (sPurchaseObserver == null) {
            if (Consts.DEBUG) {
                Log.d(TAG, "UI is not running");
            }
            return;
        }
        sPurchaseObserver.startBuyPageActivity(pendingIntent, intent);
    }

    public static void purchaseResponse(
            final Context context, final Consts.PurchaseState purchaseState, final String productId,
            final String orderId, final long purchaseTime, final String developerPayload) {

        // Update the database with the purchase state. We shouldn't do that
        // from the main thread so we do the work in a background thread.
        // We don't update the UI here. We will update the UI after we update
        // the database because we need to read and update the current quantity
        // first.
        new Thread(new Runnable() {
            public void run() {
                //PurchaseDatabase db = new PurchaseDatabase(context);
                //int quantity = db.updatePurchase(
                //        orderId, productId, purchaseState, purchaseTime, developerPayload);
                //db.close();

                int quantity=1;

                // This needs to be synchronized because the UI thread can change the
                // value of sPurchaseObserver.
                synchronized(ResponseHandler.class) {
                    if (sPurchaseObserver != null) {
                        sPurchaseObserver.postPurchaseStateChange(
                                purchaseState, productId, quantity, purchaseTime, developerPayload);
                    }
                }
            }
        }).start();
    }

    /**
     * This is called when we receive a response code from Android Market for a
     * RequestPurchase request that we made.  This is used for reporting various
     * errors and also for acknowledging that an order was sent successfully to
     * the server. This is NOT used for any purchase state changes. All
     * purchase state changes are received in the {@link BillingReceiver} and
     * are handled in {@link Security#verifyPurchase(String, String)}.
     * @param context the context
     * @param request the RequestPurchase request for which we received a
     *     response code
     * @param responseCode a response code from Market to indicate the state
     * of the request
     */
    public static void responseCodeReceived(Context context, RequestPurchase request,
                                            Consts.ResponseCode responseCode) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.onRequestPurchaseResponse(request, responseCode);
        }
    }

    /**
     * This is called when we receive a response code from Android Market for a
     * RestoreTransactions request.
     * @param context the context
     * @param request the RestoreTransactions request for which we received a
     *     response code
     * @param responseCode a response code from Market to indicate the state
     *     of the request
     */
    public static void responseCodeReceived(Context context, RestoreTransactions request,
                                            Consts.ResponseCode responseCode) {
        if (sPurchaseObserver != null) {
            sPurchaseObserver.onRestoreTransactionsResponse(request, responseCode);
        }
    }
}
