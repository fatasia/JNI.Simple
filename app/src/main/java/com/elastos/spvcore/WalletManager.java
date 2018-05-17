
package com.elastos.spvcore;

/**
 * WalletManager jni
 */
public class WalletManager extends JniReference {


    public native static long createJniWalletManager(String stringPhrase, String stringLanguage, ChainParams objChainParams);
    public native static long createJniWalletManager(ChainParams objChainParams);

    public native void disposeNative ();

    protected WalletManager(long jniReferenceAddress) {
        super(jniReferenceAddress);
    }

    public WalletManager(String stringPhrase, String stringLanguage, ChainParams objChainParams){
        super( createJniWalletManager(stringPhrase,stringLanguage, objChainParams));

    }




    //    public WalletManager(ChainParams chainParams);
//
//    public WalletManager(String phrase, String language,ChainParams chainParams );
//
//    public WalletManager();
//
//
//    protected  native void start ();
//
//    protected  native void stop();
//
//    protected native int signAndPublishTransaction(TransactionPtr);
//
//
//    protected native Object getTransactions();

//    protected native  void registerWalletListener(Listener listener);
//
//    protected native void registerPeerManagerListener(Listener listener);
//
//    // func balanceChanged(_ balance: UInt64)
//    protected native void balanceChanged(long balance);
//
//    // func txAdded(_ tx: BRTxRef)
//    protected native  void onTxAdded(Transaction tx);
//
//    // func txUpdated(_ txHashes: [UInt256], blockHeight: UInt32, timestamp: UInt32)
//    protected native  void onTxUpdated(String hash, int blockHeight, int timeStamp);
//
//    // func txDeleted(_ txHash: UInt256, notifyUser: Bool, recommendRescan: Bool)
//    protected native  void onTxDeleted(String hash, boolean notifyUser, boolean recommendRescan);
//
//    // func syncStarted()
//    protected native  void syncStarted();
//
//    // func syncStopped(_ error: BRPeerManagerError?)
//    protected native  void syncStopped(String error);
//
//    // func txStatusUpdate()
//    protected native  void txStatusUpdate();
//
//    // func saveBlocks(_ replace: Bool, _ blocks: [BRBlockRef?])
//    protected native  void saveBlocks(boolean replace,Object blocks);
//
//    // func savePeers(_ replace: Bool, _ peers: [BRPeer])
//    protected native  void savePeers(boolean replace,Object peers);
//
//    // func networkIsReachable() -> Bool
//    protected native  boolean networkIsReachable();
//
//    // Called on publishTransaction
//    protected native  void txPublished(String  error);
//
//    protected native  Object loadTransactions();
//
//    protected native Object loadBlocks();
//
//    protected native  Object loadPeers();
//
//    protected native  int getForkId();
//
//    protected native  PeerManagerListenerPtr createPeerManagerListener();
//
//    protected native  WalletListenerPtr createWalletListener();

}
