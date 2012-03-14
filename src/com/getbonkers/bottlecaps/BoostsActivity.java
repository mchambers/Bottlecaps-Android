package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import android.widget.TextView;
import com.getbonkers.bottlecaps.BillingService.*;
import com.getbonkers.bottlecaps.Consts.*;

public class BoostsActivity extends Activity {

    private BillingService billingService;
    private BoostsPurchaseObserver observer;
    private Handler handler;
    
    private Player player;
    
    private final String PRODUCT_BAG_OF_BOOSTS="gb_bc_boosts_5";
    private final String PRODUCT_BARREL_OF_BOOSTS="gb_bc_boosts_25";
    private final String PRODUCT_BUCKET_OF_BOOSTS="gb_bc_boosts_15";

    private final String PRODUCT_TEST_ACCEPTED="android.test.purchased";

    private class BoostsPurchaseObserver extends PurchaseObserver {
        public BoostsPurchaseObserver(Handler handler) {
            super(BoostsActivity.this, handler);
        }

        @Override
        public void onBillingSupported(boolean supported) {
            if (Consts.DEBUG) {
                //Log.i(TAG, "supported: " + supported);
            }
            if (supported) {
                //restoreDatabase();
                //mBuyButton.setEnabled(true);
                //mEditPayloadButton.setEnabled(true);
            } else {
                //showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
            }
        }

        @Override
        public void onPurchaseStateChange(Consts.PurchaseState purchaseState, String itemId,
                                          int quantity, long purchaseTime, String developerPayload) {
            if (Consts.DEBUG) {
                //Log.i(TAG, "onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);
            }

            if (developerPayload == null) {
            } else {
            }

            if (purchaseState == PurchaseState.PURCHASED) {
                Log.d("BoostsActivity", "Item purchased: "+itemId);

                if(itemId.equals(PRODUCT_BAG_OF_BOOSTS))
                {
                    player.addBoosts(5, Player.PLAYER_BOOST_TYPE_ALL);
                }
                else if(itemId.equals(PRODUCT_BUCKET_OF_BOOSTS))
                {
                    player.addBoosts(15, Player.PLAYER_BOOST_TYPE_ALL);
                }
                else if(itemId.equals(PRODUCT_BARREL_OF_BOOSTS))
                {
                    player.addBoosts(25, Player.PLAYER_BOOST_TYPE_ALL);
                }

                refreshBoostCounts();
            }
        }

        @Override
        public void onRequestPurchaseResponse(RequestPurchase request,
                                              ResponseCode responseCode) {

            if (Consts.DEBUG) {
                //Log.d(TAG, request.mProductId + ": " + responseCode);
            }
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Consts.DEBUG) {
                    //Log.i(TAG, "purchase was successfully sent to server");
                }
                //logProductActivity(request.mProductId, "sending purchase request");
            } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
                if (Consts.DEBUG) {
                   // Log.i(TAG, "user canceled purchase");
                }
                //logProductActivity(request.mProductId, "dismissed purchase dialog");
            } else {
                if (Consts.DEBUG) {
                    //Log.i(TAG, "purchase failed");
                }
                //logProductActivity(request.mProductId, "request purchase returned " + responseCode);
            }
        }

        @Override
        public void onRestoreTransactionsResponse(RestoreTransactions request,
                                                  ResponseCode responseCode) {
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Consts.DEBUG) {
                    //Log.d(TAG, "completed RestoreTransactions request");
                }
                // Update the shared preferences so that we don't perform
                // a RestoreTransactions again.
                //SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                //SharedPreferences.Editor edit = prefs.edit();
                //edit.putBoolean(DB_INITIALIZED, true);
                //edit.commit();
            } else {
                if (Consts.DEBUG) {
                    //Log.d(TAG, "RestoreTransactions error: " + responseCode);
                }
            }
        }
    }

    private void refreshBoostCounts()
    {
        // populate the item counts
        TextView nitro=(TextView)findViewById(R.id.boostsNitro);
        TextView frenzy=(TextView)findViewById(R.id.boostsFrenzy);
        TextView time=(TextView)findViewById(R.id.boostsTime);
        TextView joker=(TextView)findViewById(R.id.boostsJoker);

        nitro.setText(player.numberOfBoostsForType(Player.PLAYER_BOOST_TYPE_NITRO) + "\nNitro");
        frenzy.setText(player.numberOfBoostsForType(Player.PLAYER_BOOST_TYPE_FRENZY) + "\nFrenzy");
        time.setText(player.numberOfBoostsForType(Player.PLAYER_BOOST_TYPE_MORETIME) + "\nTime");
        joker.setText(player.numberOfBoostsForType(Player.PLAYER_BOOST_TYPE_JOKER) + "\nJoker");
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.boosts);

        handler=new Handler();
        observer=new BoostsPurchaseObserver(handler);

        billingService=new BillingService();
        billingService.setContext(this);

        ResponseHandler.register(observer);
        if(!billingService.checkBillingSupported())
        {
            // disable the purchase buttons
        }

        player=new Player(this);

        refreshBoostCounts();
    }

    public void bagOfBoostsClick(View v)
    {
        billingService.requestPurchase(PRODUCT_BAG_OF_BOOSTS, "");
    }
    
    public void barrelOfBoostsClick(View v)
    {
        billingService.requestPurchase(PRODUCT_BARREL_OF_BOOSTS, "");
    }
    
    public void bucketOfBoostsClick(View v)
    {
        billingService.requestPurchase(PRODUCT_BUCKET_OF_BOOSTS, "");
    }

    public void rateAppClick(View v)
    {
        String appPackageName="com.getbonkers.bottlecaps";

        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        try {
            startActivity(marketIntent);
        } catch(Exception e)
        {
            // the Android Market is unavailable. try the Amazon Appstore instead?
        }
    }
}
