/* @file BleOpNotify.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief Bluetooth LE notify operation 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.dev.bric;

import com.topodroid.utils.TDLog;
import com.topodroid.DistoX.R;

import android.content.Context;

import android.bluetooth.BluetoothGattCharacteristic;

import android.util.Log;

import java.util.UUID;

class BleOpNotify extends BleOperation 
{
  boolean mEnable;
  UUID mSrvUuid;
  // UUID mChrtUuid;
  BluetoothGattCharacteristic mChrt;

  BleOpNotify( Context ctx, BleComm pipe, UUID srvUuid, BluetoothGattCharacteristic chrt, boolean enable )
  {
    super( ctx, pipe );
    mSrvUuid  = srvUuid;
    // mChrtUuid = chrtUuid;
    mChrt = chrt;
    mEnable   = enable;
  }

  String name() { return "Notify"; }

  @Override 
  void execute()
  {
    // Log.v("DistoX-BLE-B", "BleOp exec notify " + mEnable );
    if ( mPipe == null ) { 
      TDLog.Error("BleOp notify error: null pipe" );
      return;
    }
    if ( mEnable ) {
      mPipe.enablePNotify( mSrvUuid, mChrt );
    } else {
      // mPipe.disablePNotify( mSrvUuid, mChrt );
    }
  }
}