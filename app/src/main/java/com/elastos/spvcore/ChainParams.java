package com.elastos.spvcore;

public class ChainParams  extends JniReference{


    protected ChainParams(long jniReferenceAddress) {
        super(jniReferenceAddress);
    }

    public ChainParams(){
        super(createJniMainnetChainParams());
    }

    public native static  long createJniMainnetChainParams();
    public native  static long createJniTestnetChainParams();
}
