/*
 * BreadWallet
 *
 * Created by Ed Gamble <ed@breadwallet.com> on 1/22/18.
 * Copyright (c) 2018 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.elastos.spvcore;

import java.lang.ref.WeakReference;

/**
 *
 */
public class CorePeerManager extends JniReference {

    //
    // Callback interface from Core, via JNI.
    //
    public interface Listener {
        // func syncStarted()
        void syncStarted();

        // func syncStopped(_ error: BRPeerManagerError?)
        void syncStopped(String error);

        // func txStatusUpdate()
        void txStatusUpdate();

        // func saveBlocks(_ replace: Bool, _ blocks: [BRBlockRef?])
        void saveBlocks(boolean replace, CoreMerkleBlock[] blocks);

        // func savePeers(_ replace: Bool, _ peers: [BRPeer])
        void savePeers(boolean replace, CorePeer[] peers);

        // func networkIsReachable() -> Bool}
        boolean networkIsReachable();

        // Called on publishTransaction
        void txPublished(String error);
    }

    //
    // The Wallet
    //
    protected CoreWallet wallet;

    //
    // Hold a weak reference to the listener.  It is a weak reference because it is likely to
    // be self-referential which would prevent GC of this PeerManager.  This listener is used
    // by the BRCore PeerManager, and specifically in a BRCore thread context to invoke the
    // Listener methods.  Because of the use in a BRCore thread, the Listener *MUST BE* and
    // JNI Global Ref.  The installListener() method, called in the BRCorePeerManager constructor
    // initializes 'listener' with a GlobalWeakReference.
    //
    protected WeakReference<Listener> listener = null;

    //
    //
    //
    public CorePeerManager(CoreChainParams params,
                           CoreWallet wallet,
                           double earliestKeyTime,
                           CoreMerkleBlock[] blocks,
                           CorePeer[] peers,
                           Listener listener) {
        // double time to int time.
        super(createCorePeerManager(params, wallet, earliestKeyTime, blocks, peers));
        assert (null != listener);
        this.listener = new WeakReference<>(listener);
        this.wallet = wallet;

        installListener(listener);
    }

    /**
     *
     */
    public CorePeer.ConnectStatus getConnectStatus () {
        return CorePeer.ConnectStatus.fromValue(getConnectStatusValue());
    }

    private native int getConnectStatusValue ();

    /**
     * Connect to bitcoin peer-to-peer network (also call this whenever networkIsReachable()
     * status changes)
     */
    public native void connect();

    /*
    // connect to bitcoin peer-to-peer network (also call this whenever networkIsReachable() status changes)
    func connect() {
        if let fixedAddress = UserDefaults.customNodeIP {
            setFixedPeer(address: fixedAddress, port: UserDefaults.customNodePort ?? C.standardPort)
        }
        BRPeerManagerConnect(cPtr)
    }
*/

    //
    // Fixed Peer
    //
    public boolean useFixedPeer (String node, int port) {
        return jniUseFixedPeer(node, port);
    }

    protected native boolean jniUseFixedPeer (String node, int port);

    public native String getCurrentPeerName ();

    @Override
    protected void finalize() throws Throwable {
        if (CorePeer.ConnectStatus.Disconnected != getConnectStatus())
            System.out.println ("Disposing PeerManager while not DISCONNECTED: " + this.toString());
        super.finalize();
    }

    /**
     * Disconnect from bitcoin peer-to-peer network (may cause syncFailed(), saveBlocks() or
     * savePeers() callbacks to fire)
     */
    public native void disconnect();

    /**
     *
     */
    public native void rescan ();

    /**
     *
     * @return
     */
    public native long getEstimatedBlockHeight ();

    /**
     *
     * @return
     */
    public native long getLastBlockHeight ();

    /**
     *
     * @return
     */
    public native long getLastBlockTimestamp ();

    /**
     *
     * @param startHeight
     * @return
     */
    public native double getSyncProgress (long startHeight);

    /**
     * @return
     */
    public native int getPeerCount();

    /**
     *
     * @return
     */
    public native String getDownloadPeerName ();

    /**
     *
     * @param transaction
     */
    public void publishTransaction (CoreTransaction transaction) {
        // Calling publishTransactionWithListener will 'give' transaction to the wallet.  Thus
        // it must be considered 'registered' if we are not copying.
        transaction.isRegistered = transaction.isRegistered
                || !CoreTransaction.JNI_COPIES_TRANSACTIONS;
        publishTransactionWithListener(transaction, listener.get());

    }

    /**
     * A native method that will callback to BRCorePeerManager.Listener::txPublished.  We must
     * pass in the Listener, so that the Core function BRPeerManagerPublishTx() will know where
     * to callback into Java
     *
     * This calls BRPeerManagerPublishTx which notes "publishes tx to bitcoin network (do not
     * call BRTransactionFree() on tx afterward)"
     *
     * @param transaction
     * @param listener
     */
    protected native void publishTransactionWithListener (CoreTransaction transaction,
                                                          Listener listener);

    /**
     * @param txHash
     * @return
     */
    public native long getRelayCount (byte[] txHash);

    //
    // Test
    //

    // TODO: Reimplement w/ proper WeakReference listener access.
    public native void testSaveBlocksCallback (boolean replace, CoreMerkleBlock[] blocks);
    public native void testSavePeersCallback (boolean replace, CorePeer[] peers);

    //
    // Constructor
    //

    /**
     * This will eventually call BRPeerManagerNew() with *copies* of `blocks` and `peers`. Thus
     * these array objects will not be shared.
     *
     * @param params
     * @param wallet
     * @param earliestKeyTime
     * @param blocks
     * @param peers
     * @return
     */
    private static native long createCorePeerManager(CoreChainParams params,
                                                     CoreWallet wallet,
                                                     double earliestKeyTime, // int
                                                     CoreMerkleBlock[] blocks,
                                                     CorePeer[] peers);

    protected native void installListener (Listener listener);

    //
    // Finalization
    //

    public native void disposeNative();

    protected static native void initializeNative ();

    static { initializeNative(); }
}
