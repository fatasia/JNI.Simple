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

import java.util.concurrent.Executor;

/**
 * A BRCoreWalletManger instance manages a single wallet, and that wallet's individual connection
 * to the bitcoin network.  After instantiating a BRCoreWalletManager object, call
 * myWalletManager.peerManager.connect() to begin syncing.
 *
 */
public class CoreWalletManager implements
        CorePeerManager.Listener,
        CoreWallet.Listener {

    protected static boolean SHOW_CALLBACK = true;
    protected static boolean SHOW_CALLBACK_DETAIL = false;

    protected static boolean SHOW_CALLBACK_DETAIL_TX_STATUS = false;
    protected static boolean SHOW_CALLBACK_DETAIL_TX_IO = false;

    protected CoreMasterPubKey masterPubKey;

    protected CoreChainParams chainParams;

    double earliestPeerTime;

    CoreWallet wallet; // Optional<BRCoreWallet>

    CorePeerManager peerManager; // Optional<BRCorePeerManager>
    
    //
    //
    //
    public CoreWalletManager(CoreMasterPubKey masterPubKey,
                               CoreChainParams chainParams,
                               double earliestPeerTime) {
        this.masterPubKey = masterPubKey;
        this.chainParams = chainParams;
        this.earliestPeerTime = earliestPeerTime;
    }

    //
    // Wallet
    //
    public synchronized CoreWallet getWallet () {
        if (null == wallet) {
            wallet = createWallet();
        }
        return wallet;
    }

    /**
     * Factory method to create a BRCoreWallet (or subtype).  This can fail if loadTransaction()
     * contains transactions that do not belong to wallet's masterPubKey.  In that case the proper
     * response is to zero out the transactions, create the wallet, and then sync.
     *
     * @return The BRCoreWallet managed by this BRCoreWalletManager
     */

    protected CoreWallet createWallet () {
        try {
            return new CoreWallet(loadTransactions(), masterPubKey,
                    createWalletListener());
        }
        catch (CoreWallet.WalletExecption ex) {
            return createWalletRetry ();
        }
    }

    protected CoreWallet createWalletRetry () {
        return null;
    }

    /**
     * Factory method to create a BRCoreWallet.Listener (or subtype).   This class,
     * BRCoreWalletManager is a BRCoreWallet.Listener and thus `this` can be returned.
     * However, as Listener methods are generally called from the Core, using JNI, it
     * is *important* to 'wrap' this - like with WrappedExceptionWalletListener (which
     * catches exceptions) or with
     *
     * @return a BRCoreWallet.Listener, like `this` or a `wrapped this`
     */
    protected CoreWallet.Listener createWalletListener () {
        return new WrappedExceptionWalletListener (this);
    }

    //
    // Peer Manager
    //
    //    public Optional<BRCorePeerManager> getPeerManager() {
    //        if (null == peerManager) {
    //            peerManager = getWallet()
    //                    // .map (this::createPeerManager)
    //                    .map(new Function<BRCoreWallet, BRCorePeerManager>() {
    //                        @Override
    //                        public BRCorePeerManager apply(BRCoreWallet wallet) {
    //                            return createPeerManager(wallet);
    //                        }
    //                    });
    //        }
    //        return peerManager;
    //    }
    public synchronized CorePeerManager getPeerManager () {
        if (null == peerManager) {
            CoreWallet wallet = getWallet();
            if (null != wallet) {
                peerManager = createPeerManager(wallet);
            }
        }
        return peerManager;
    }

    /**
     * Factory method to create a BRCorePeerManager (or subtype).
     *
     * @param wallet The wallet
     * @return A BRCorePeerManager for the provided wallet.
     */
    protected CorePeerManager createPeerManager (CoreWallet wallet) {
        return new CorePeerManager(chainParams, wallet, earliestPeerTime, loadBlocks(), loadPeers(),
                createPeerManagerListener());
    }

    /**
     * Factory method to create a BRCorePeerManagerListener.  See comments for createWalletListener.
     *
     * @return A BRCorePeerManager.Listener, like `this` or a `wrapped this`
     */
    protected CorePeerManager.Listener createPeerManagerListener () {
        return new WrappedExceptionPeerManagerListener (this);
    }

    //
    //
    //
    public byte[] signAndPublishTransaction (CoreTransaction transaction, CoreMasterPubKey masterPubKey) {
        assert (false);
        return null;
    }

    /**
     * Sign and then publish the `transaction`
     *
     * @param transaction
     * @param phrase the result of BRCoreMasterPubKey.generatePaperKey()
     * @return the transaction hash
     */
    public byte[] signAndPublishTransaction (CoreTransaction transaction, byte[] phrase) {
        getWallet().signTransaction(transaction, getForkId(), phrase);
        getPeerManager().publishTransaction(transaction);
        return transaction.getHash();
    }

    protected int getForkId () {
        if (chainParams == CoreChainParams.mainnetChainParams
                || chainParams == CoreChainParams.testnetChainParams)
            return 0x00;
        else if (chainParams == CoreChainParams.mainnetBcashChainParams
                || chainParams == CoreChainParams.testnetBcashChainParams)
            return 0x40;
        else return -1;
    }

    //
    // Support
    //

    protected CoreTransaction[] loadTransactions ()
    {
        return new CoreTransaction[0];
    }

    protected CoreMerkleBlock[] loadBlocks ()
    {
        return new CoreMerkleBlock[0];
    }

    protected CorePeer[] loadPeers ()
    {
        return new CorePeer[0];
    }

    private void showTxDetail (String label) {
        CoreWallet wallet = getWallet();

        CoreTransaction transactions[] = wallet.getTransactions();
        System.out.println (getChainDescriptiveName() + " " + label + " txCount: " + transactions.length);
        for (CoreTransaction transaction : wallet.getTransactions()) {
            System.out.println("    tx: " + transaction.toString());
            System.out.println("        : " +
                    (transaction.isSigned() ? "SIGNED" : "NOT-SIGNED") + " " +
                    (transaction.isSigned() ? (wallet.transactionIsValid(transaction) ? "VALID" : "NOT-VALID") : "N/A") + " " +
                    "balance: " + (transaction.isSigned() ? wallet.getBalanceAfterTransaction(transaction) : "N/A"));

            if (SHOW_CALLBACK_DETAIL_TX_IO) {
                for (CoreTransactionInput input : transaction.getInputs())
                    System.out.println(input.toString());
                for (CoreTransactionOutput output : transaction.getOutputs())
                    System.out.println(output.toString());
            }
        }
        System.out.println ("    balance: " + wallet.getBalance());
    }
    //
    // BRCorePeerManager.Listener
    //

    @Override
    public void syncStarted() {
        if (!SHOW_CALLBACK) return;
        System.out.println (getChainDescriptiveName() + ": syncStarted");
    }

    @Override
    public void syncStopped(String error) {
        if (!SHOW_CALLBACK) return;
        System.out.println (getChainDescriptiveName() + ": syncStopped: " + error);
    }

    @Override
    public void txStatusUpdate() {
        if (!SHOW_CALLBACK) return;
        System.out.println (getChainDescriptiveName() + ": txStatusUpdate");
        //super.txStatusUpdate();

        if (!SHOW_CALLBACK_DETAIL_TX_STATUS) return;
        showTxDetail("txStatusUpdate");
    }

    @Override
    public void saveBlocks(boolean replace, CoreMerkleBlock[] blocks) {
        if (!SHOW_CALLBACK) return;
        System.out.println(getChainDescriptiveName() + String.format(": saveBlocks: %d", blocks.length));

        if (!SHOW_CALLBACK_DETAIL) return;
        for (int i = 0; i < blocks.length; i++)
            System.out.println(blocks[i].toString());
    }

    @Override
    public void savePeers(boolean replace, CorePeer[] peers) {
        if (!SHOW_CALLBACK) return;
        System.out.println(getChainDescriptiveName() + String.format(": savePeers: %d", peers.length));

        if (!SHOW_CALLBACK_DETAIL) return;
        for (int i = 0; i < peers.length; i++)
            System.out.println(peers[i].toString());
    }

    @Override
    public boolean networkIsReachable() {
        // System.out.println ("networkIsReachable");
        return true;
    }

    @Override
    public void txPublished(String error) {
        if (!SHOW_CALLBACK) return;
        System.out.println (getChainDescriptiveName() + String.format (": txPublished: %s", error));

        if (!SHOW_CALLBACK_DETAIL_TX_STATUS) return;
        showTxDetail("txPublished");
    }

    //
    // BRCoreWallet.Listener
    //

    @Override
    public void balanceChanged(long balance) {
        if (!SHOW_CALLBACK) return;
        System.out.println (getChainDescriptiveName() + String.format (": balanceChanged: %d", balance));
        if (!SHOW_CALLBACK_DETAIL_TX_STATUS) return;
        showTxDetail("balanceChanged");
    }

    @Override
    public void onTxAdded(CoreTransaction transaction) {
        if (!SHOW_CALLBACK) return;
        System.out.println (getChainDescriptiveName() + ": onTxAdded: " + bytesToHex(transaction.getHash()));

        if (!SHOW_CALLBACK_DETAIL_TX_STATUS) return;
        showTxDetail("balanceChanged");
    }

    @Override
    public void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        if (!SHOW_CALLBACK) return;
        System.out.println (getChainDescriptiveName() + ": onTxUpdated: " + hash);

        if (!SHOW_CALLBACK_DETAIL_TX_STATUS) return;
        showTxDetail("onTxUpdated");
    }

    @Override
    public void onTxDeleted(String hash, int notifyUser, int recommendRescan) {
        if (!SHOW_CALLBACK) return;
        System.out.println (getChainDescriptiveName() + ": onTxDeleted: " + hash);

        if (!SHOW_CALLBACK_DETAIL_TX_STATUS) return;
        showTxDetail("onTxDeleted");
    }

    //
    // Object methods
    //
    @Override
    public String toString() {
        return "BRCoreWalletManager {" +
                "\n  masterPubKey      : " + masterPubKey +
                "\n  chainParams       : " + chainParams +
                "\n  earliest peer time: " + earliestPeerTime +
                "\n  wallet rcv addr   : " + (wallet != null ? wallet.getReceiveAddress().stringify() : "") +
                "\n  peerManager status: " + (peerManager != null ? peerManager.getConnectStatus().name() : "") +
                '}';
    }

    private String getChainDescriptiveName ()
    {
        if (chainParams == CoreChainParams.mainnetChainParams)
            return "Bitcoin Mainnet";
        else if (chainParams == CoreChainParams.testnetChainParams)
            return "Bitcoin Testnet";
        else if (chainParams == CoreChainParams.mainnetBcashChainParams)
            return "Bitcash Mainnet";
        else if (chainParams == CoreChainParams.testnetBcashChainParams)
            return "Bitcash Testnet";
        return "Unknown";
    }
    // ============================================================================================
    //
    // Callbacks from JNI code that throw an exception are QUIETLY SWALLOWED.  We'll provide
    // a wrapper class, implementing each Listener used for callbacks.  The wrapper class
    // will catch any exception and issue some warning, or something.

    //
    // Exception Wrapped PeerManagerListener
    //
    static public class WrappedExceptionPeerManagerListener implements CorePeerManager.Listener {
        private CorePeerManager.Listener listener;

        public WrappedExceptionPeerManagerListener(CorePeerManager.Listener listener) {
            this.listener = listener;
        }

        //        private <T> void safeHandler (Supplier<Void> supplier) {
        //            try { supplier.get(); }
        //            catch (Exception ex) {
        //                ex.printStackTrace(System.err);
        //            }
        //        }

        @Override
        public void syncStarted() {
            try { listener.syncStarted(); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        @Override
        public void syncStopped(String error) {
            try { listener.syncStopped(error); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        @Override
        public void txStatusUpdate() {
            try { listener.txStatusUpdate(); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        @Override
        public void saveBlocks(boolean replace, CoreMerkleBlock[] blocks) {
            try { listener.saveBlocks(replace, blocks); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        @Override
        public void savePeers(boolean replace, CorePeer[] peers) {
            try { listener.savePeers(replace, peers); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        @Override
        public boolean networkIsReachable() {
            try { return listener.networkIsReachable(); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
                return false;
            }
        }

        @Override
        public void txPublished(String error) {
            try { listener.txPublished(error); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    // ============================================================================================
    //
    // Callbacks from Core, via JNI, run on a Core thread - they absolutely should not run on a
    // Core thread.

    //
    // Executor Wrapped PeerManagerListener
    //

    static public class WrappedExecutorPeerManagerListener implements CorePeerManager.Listener {
        CorePeerManager.Listener listener;
        Executor executor;

        public WrappedExecutorPeerManagerListener(CorePeerManager.Listener listener, Executor executor) {
            this.listener = listener;
            this.executor = executor;
        }

        @Override
        public void syncStarted() {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.syncStarted();
                }
            });
        }

        @Override
        public void syncStopped(final String error) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.syncStopped(error);
                }
            });
        }

        @Override
        public void txStatusUpdate() {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.txStatusUpdate();
                }
            });
        }

        @Override
        public void saveBlocks(final boolean replace, final CoreMerkleBlock[] blocks) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.saveBlocks(replace, blocks);
                }
            });
        }

        @Override
        public void savePeers(final boolean replace, final CorePeer[] peers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.savePeers(replace, peers);
                }
            });
        }

        @Override
        public boolean networkIsReachable() {
            return listener.networkIsReachable();
        }

        @Override
        public void txPublished(final String error) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.txPublished(error);
                }
            });
        }
    }

    // ============================================================================================

    //
    // Exception Wrapped WalletListener
    //
    static public class WrappedExceptionWalletListener implements CoreWallet.Listener {
        private CoreWallet.Listener listener;

        public WrappedExceptionWalletListener(CoreWallet.Listener listener) {
            this.listener = listener;
        }

        @Override
        public void balanceChanged(long balance) {
            try { listener.balanceChanged(balance); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        @Override
        public void onTxAdded(CoreTransaction transaction) {
            try { listener.onTxAdded(transaction); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        @Override
        public void onTxUpdated(String hash, int blockHeight, int timeStamp) {
            try { listener.onTxUpdated(hash, blockHeight, timeStamp); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        @Override
        public void onTxDeleted(String hash, int notifyUser, int recommendRescan) {
            try { listener.onTxDeleted (hash, notifyUser, recommendRescan); }
            catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    // ============================================================================================

    //
    // Executor Wrapped WalletListener
    //

    static public class WrappedExecutorWalletListener implements CoreWallet.Listener {
        private CoreWallet.Listener listener;
        Executor executor;

        public WrappedExecutorWalletListener(CoreWallet.Listener listener, Executor executor) {
            this.listener = listener;
            this.executor = executor;
        }

        @Override
        public void balanceChanged(final long balance) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.balanceChanged(balance);
                }
            });
        }

        @Override
        public void onTxAdded(final CoreTransaction transaction) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onTxAdded (transaction);
                }
            });
        }

        @Override
        public void onTxUpdated(final String hash, final int blockHeight, final int timeStamp) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onTxUpdated (hash, blockHeight, timeStamp);
                }
            });
        }

        @Override
        public void onTxDeleted (final String hash, final int notifyUser, final int recommendRescan) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onTxDeleted(hash, notifyUser, recommendRescan);
                }
            });
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
