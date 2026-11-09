package com.appliedmoremachines.nativeeight;
import com.appliedmoremachines.production.MultiplicationHost;
/** Marker limits native hooks to the four registered variants. */
public interface NativeEightHost extends MultiplicationHost {
    default void multiplicationChanged() {}
}
