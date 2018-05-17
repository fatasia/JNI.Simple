package com.elastos.wallet;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.elastos.spvcore.ChainParams;
import com.elastos.spvcore.CoreChainParams;
import com.elastos.spvcore.CoreKey;
import com.elastos.spvcore.CoreMasterPubKey;
import com.elastos.spvcore.CorePeer;
import com.elastos.spvcore.CoreWallet;
import com.elastos.spvcore.CoreWalletManager;
import com.elastos.spvcore.WalletManager;

public class MainActivity extends Activity {

    private final String TAG = MainActivity.class.getName();

    // Used to load the 'native-lib' library on application startup.
    static {
        //System.loadLibrary("c++_shared");
        //System.loadLibrary("log");
        System.loadLibrary("spvsdk");
    }


    private WalletManager manager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testWallet();
    }


    public void testWallet(){
        manager = new WalletManager("wallet abc","english",new ChainParams());
        //manager.createJniWalletManager();
    }

//    public void testManager(){
//        CoreMasterPubKey puKey = new CoreMasterPubKey();
//        manager = new CoreWalletManager(puKey);
//    }

    public void test() {
//        String addresss = "EciyeLa45qX8AwVWuekEweCsfb676LdRNe";
//       CorePeer peer = new CorePeer(addresss.getBytes(),20866,System.currentTimeMillis());
//

        CoreKey key = new CoreKey();
        Log.i(TAG, key.getPrivKey());

//        CoreMasterPubKey master = new CoreMasterPubKey(key.getPubKey(),false);
//        CoreChainParams params = CoreChainParams.testnetChainParams;
//        CoreWalletManager manager = new CoreWalletManager(master,params,100000);
//        CoreWallet wallet = manager.getWallet();
//
//        Log.i(TAG,"余额:"+  wallet.getBalance());
//        Log.i(TAG,"地址:"+ wallet.getAllAddresses().toString());
        // Log.i("pubKey",key.address());
        //Log.i("address",key.address());

    }

}
