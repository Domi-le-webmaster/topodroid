/* @file SapComm.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid SAP5 communication REQUIRES API-18
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 *
 * WARNING TO BE FINISHED
 */
package com.topodroid.dev.sap;

// import com.topodroid.utils.TDLog;
// import com.topodroid.prefs.TDSetting;
import com.topodroid.DistoX.TDInstance;
import com.topodroid.DistoX.DataDownloader;
import com.topodroid.DistoX.TopoDroidApp;
import com.topodroid.dev.ConnectionState;
import com.topodroid.dev.DataType;
import com.topodroid.dev.Device;
import com.topodroid.dev.TopoDroidComm;

import android.util.Log;

import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.content.Context;

import android.bluetooth.BluetoothDevice;
// import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

// -----------------------------------------------------------------------------
public class SapComm extends TopoDroidComm
{
  // -----------------------------------------------
  private boolean mWriteInitialized = false;
  private boolean mReadInitialized  = false;

  // BluetoothAdapter   mAdapter;
  private BluetoothGatt mGatt = null;
  BluetoothGattCharacteristic mWriteChrt;
  private BluetoothGattCharacteristic mReadChrt;

  private String          mRemoteAddress;
  private BluetoothDevice mRemoteBtDevice;


  public SapComm( TopoDroidApp app, String address, BluetoothDevice bt_device ) 
  {
    super( app );
    mRemoteAddress = address;
    mRemoteBtDevice  = bt_device;
    // Log.v("DistoX-BLE-S", "SAP comm cstr, addr " + address );
  }

  // void setRemoteDevice( BluetoothDevice device ) 
  // { 
  //   Log.v("DistoX-BLE-S", "SAP comm set remote " + device.getAddress() );
  //   mRemoteBtDevice = device;
  // }

  // -------------------------------------------------------------
  /** 
   * connection and data handling must run on a separate thread
   */
  private void setConnected( boolean connected ) 
  { 
    mBTConnected = connected;
    // if ( ! connected ) {
    //   TDUtil.yieldDown( 500 );
    //   Log.v("DistoX-BLE-S", "SAP comm try re-connect: does not do anything");
    //   // connectDevice( TDInstance.deviceAddress(), mLister, DataType.DATA_SHOT );
    // } else {
    //   Log.v("DistoX-BLE-S", "SAP comm set connected: does not do anything " + connected );
    //   // notify ???
    // }
  }

  // Device has mAddress, mModel, mName, mNickname, mType
  // the only thing that coincide with the remote_device is the address
  //
  private void connectSapDevice( Device device, Context context, int data_type ) // FIXME BLEX_DATA_TYPE
  {
    if ( mRemoteBtDevice == null ) {
      // TDToast.makeBad( R.string.ble_no_remote );
      Log.v("DistoX-BLE-S", "SAP comm error: null remote device");
    } else {
      // check that device.mAddress.equals( mRemoteBtDevice.getAddress() 
      // Log.v("DistoX-BLE-S", "SAP comm connect remote addr " + mRemoteBtDevice.getAddress() + " " + device.mAddress );
      SapProtocol protocol     = new SapProtocol( this, device, context );
      SapGattCallback callback = new SapGattCallback( protocol, data_type );
      mProtocol = protocol;
      if ( Build.VERSION.SDK_INT < 23 ) {
        // mGatt = mRemoteBtDevice.connectGatt( context, false, callback ); // true: autoconnect as soon as the device becomes available
        mGatt = mRemoteBtDevice.connectGatt( context, true, callback ); // true: autoconnect as soon as the device becomes available
      } else {
        mGatt = mRemoteBtDevice.connectGatt( context, true, callback, BluetoothDevice.TRANSPORT_LE ); 
      }
    }
  }

  private boolean mDisconnecting = false;

  private void disconnectSapGatt()
  {
    // Log.v("DistoX-BLE-S", "SAP comm disconnect Gatt");
    if ( mDisconnecting ) return;
    mDisconnecting = true;
    mConnectionMode = -1;
    setConnected( false );
    mWriteInitialized = false; 
    mReadInitialized  = false; 
    if ( mGatt != null ) {
      mGatt.disconnect();
      mGatt.close();
      mGatt = null;
    }
    mApp.notifyStatus( ConnectionState.CONN_DISCONNECTED );
    mDisconnecting = false;
  }

  // -------------------------------------------------------------
  private class SapGattCallback extends BluetoothGattCallback
  {
    SapProtocol mProto;
    int mDataType;

    SapGattCallback( SapProtocol proto, int data_type ) 
    { 
      mProto = proto;
      mDataType = data_type;
    }

    @Override
    public void onConnectionStateChange( BluetoothGatt gatt, int status, int state )
    {
      // super.onConnectionStateChange( gatt, status, state );
      if ( status == BluetoothGatt.GATT_FAILURE ) {
        // Log.v("DistoX-BLE-S", "SAP comm FAIL state changed");
        disconnectSapGatt();
        return;
      } 
      if ( status != BluetoothGatt.GATT_SUCCESS ) {
        // Log.v("DistoX-BLE-S", "SAP comm state changed: unsuccessful"); // apparently this is ok
        // disconnectSapGatt();
        mApp.notifyStatus( ConnectionState.CONN_WAITING );
        return;
      } 
      if ( state == BluetoothProfile.STATE_CONNECTED ) {
        // Log.v("DistoX-BLE-S", "SAP comm conn state changed: connected");
        if ( ! gatt.discoverServices() ) {
          // Log.v("DistoX-BLE-S", "SAP comm FAIL service discovery");
          mApp.notifyStatus( ConnectionState.CONN_DISCONNECTED );
        } else {
          mApp.notifyStatus( ConnectionState.CONN_CONNECTED );
        }
      } else if ( state == BluetoothProfile.STATE_DISCONNECTED ) {
        // Log.v("DistoX-BLE-S", "SAP comm conn state changed: disconnected");
        disconnectSapGatt();
      }
    }

    @Override
    public void onServicesDiscovered( BluetoothGatt gatt, int status )
    {
      // super.onServicesDiscovered( gatt, status );
      if ( status != BluetoothGatt.GATT_SUCCESS ) {
        Log.v("DistoX-BLE-S", "SAP comm FAIL service discover");
        return;
      }
      // Log.v("DistoX-BLE-S", "SAP comm service discovered ok" );
      BluetoothGattService srv = gatt.getService( SapConst.SAP5_SERVICE_UUID );

      mReadChrt  = srv.getCharacteristic( SapConst.SAP5_CHAR_READ_UUID );
      mWriteChrt = srv.getCharacteristic( SapConst.SAP5_CHAR_WRITE_UUID );

      int writeProp = mWriteChrt.getProperties();
      boolean write_has_write = ( writeProp & BluetoothGattCharacteristic.PROPERTY_WRITE ) != 0;
      boolean write_has_write_no_response = ( writeProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE ) != 0;
      // Log.v("DistoX-BLE-S", "SAP comm W-chrt has write " + write_has_write );

      mWriteChrt.setWriteType( BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT );
      mWriteInitialized = gatt.setCharacteristicNotification( mWriteChrt, true );

      mReadInitialized = gatt.setCharacteristicNotification( mReadChrt, true );

      BluetoothGattDescriptor readDesc = mReadChrt.getDescriptor( SapConst.CCCD );
      if ( readDesc == null ) {
        Log.v("DistoX-BLE-S", "SAP comm FAIL no R-desc CCCD ");
        return; // FAILURE
      }
      int readProp  = mReadChrt.getProperties();
      boolean read_has_write  = ( readProp  & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
      // Log.v("DistoX-BLE-S", "SAP comm R-chrt has write " + read_has_write );

      if ( ( readProp & BluetoothGattCharacteristic.PROPERTY_INDICATE ) != 0 ) {
        // Log.v("DistoX-BLE-S", "SAP comm R-chrt prop INDICATE ");
        readDesc.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE );
      } else if ( ( readProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY ) != 0 ) {
        // Log.v("DistoX-BLE-S", "SAP comm R-chrt prop NOTIFY ");
        readDesc.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE );
      } else {
        Log.v("DistoX-BLE-S", "SAP comm FAIL no indicate/notify R-property ");
        return; // FAILURE
      }
      if ( ! gatt.writeDescriptor( readDesc ) ) {
        Log.v("DistoX-BLE-S", "SAP comm ERROR writing readDesc");
        return; // FAILURE
      }

    }

    @Override
    public void onDescriptorWrite( BluetoothGatt gatt, BluetoothGattDescriptor desc, int status ) 
    {
      if ( status != BluetoothGatt.GATT_SUCCESS ) {
        Log.v("DistoX-BLE-S", "SAP comm FAIL on descriptor write");
        return;
      }
      if ( desc.getCharacteristic() == mReadChrt ) { // everything is ok
        // tell the protocol it is connected
        setConnected( true );
      } else if ( desc.getCharacteristic() == mWriteChrt ) { // should not happen
        Log.v("DistoX-BLE-S", "SAP comm ERROR write-descriptor write: ?? should not happen");
      } else {
        Log.v("DistoX-BLE-S", "SAP comm ERROR unknown descriptor write");
        super.onDescriptorWrite( gatt, desc, status );
      }
    }
        
    @Override
    public void onCharacteristicWrite( BluetoothGatt gatt, BluetoothGattCharacteristic chrt, int status )
    {
      if ( status != BluetoothGatt.GATT_SUCCESS ) {
        Log.v("DistoX-BLE-S", "SAP comm FAIL on char write");
        return;
      }
      if ( chrt != mWriteChrt ) {
        // Log.v("DistoX-BLE-S", "SAP comm ERROR not my write chrt");
        super.onCharacteristicWrite( gatt, chrt, status );
        return;
      }
      if ( ! mWriteInitialized ) {
        Log.v("DistoX-BLE-S", "SAP comm ERROR write-uninitialized chrt" );
        return;
      }
      Log.v("DistoX-BLE-S", "SAP comm on char write ok");
      if ( mProto.handleWrite( chrt ) > 0 ) {
        mGatt.writeCharacteristic( chrt );
      } else {
        // done with the buffer writing
      }
    }

    @Override
    public void onCharacteristicRead( BluetoothGatt gatt, BluetoothGattCharacteristic chrt, int status )
    {
      if ( status != BluetoothGatt.GATT_SUCCESS ) {
        Log.v("DistoX-BLE-S", "SAP comm FAIL on char read");
        return;
      }
      if ( chrt != mReadChrt ) {
        // Log.v("DistoX-BLE-S", "SAP comm ERROR not my read chrt");
        super.onCharacteristicRead( gatt, chrt, status );
        return;
      }
      if ( ! mReadInitialized ) {
        Log.v("DistoX-BLE-S", "SAP comm ERROR read-uninitialized chrt");
        return;
      }
      Log.v("DistoX-BLE-S", "SAP comm on char read ok");
      int res = mProto.handleRead( chrt );
      if ( res == DataType.PACKET_DATA ) {
        ++ mNrPacketsRead;
        handleRegularPacket( res, mLister, mDataType );
      }
    }

    @Override
    public void onCharacteristicChanged( BluetoothGatt gatt, BluetoothGattCharacteristic chrt )
    {
      if ( chrt == mReadChrt ) {
        // Log.v("DistoX-BLE-S", "SAP comm read chrt changed");
        int res = mProto.handleNotify( chrt, true ); // true = READ
        if ( res == DataType.PACKET_DATA ) {
          // mProto.handleWrite( mWriteChrt ); // ACKNOWLEDGMENT
          handleRegularPacket( res, mLister, DataType.DATA_SHOT );
        }
        // readSapPacket();
      } else if ( chrt == mWriteChrt ) {
        Log.v("DistoX-BLE-S", "SAP comm write chrt changed");
        mProto.handleNotify( chrt, false ); // false = WRITE
      } else {
        super.onCharacteristicChanged( gatt, chrt );
      }
    }
  }

  private boolean readSapPacket( )
  { 
    Log.v("DistoX-BLE-S", "SAP comm reading packet");
    // BluetoothGattService srv = mGatt.getService( SapConst.SAP5_SERVICE_UUID );
    // BluetoothGattCharacteristic chrt = srv.getCharacteristic( SapConst.SAP5_CHAR_READ_UUID );
    // return mGatt.readCharacteristic( chrt );
    return mGatt.readCharacteristic( mReadChrt );
  }
    
  // ------------------------------------------------------------------------------------
  // CONTINUOUS DATA DOWNLOAD
  private int mConnectionMode = -1;

  @Override
  public boolean connectDevice( String address, Handler /* ILister */ lister, int data_type )
  {
    // Log.v("DistoX-BLE-S", "SAP comm connect device (continuous data download)");
    mLister = lister;
    mConnectionMode = 1;
    mNrPacketsRead = 0;
    connectSapDevice( TDInstance.deviceA, mApp, data_type );
    return true;
  }

  @Override
  public void disconnectDevice() 
  {
    // Log.v("DistoX-BLE-S", "SAP comm disconnect device");
    disconnectSapGatt();
  }

  // -------------------------------------------------------------------------------------
  // ON-DEMAND DATA DOWNLOAD
  private Handler mLister;

  /** download data
   * @param address    device address
   * @param lister     data lister
   * @param data_type  packet datatype
   * @return always 0
   */
  public int downloadData( String address, Handler /* ILister */ lister, int data_type )
  {
    Log.v("DistoX-BLE-S", "SAP comm batch data downlaod");
    mConnectionMode = 0;
    mLister = lister;
    mNrPacketsRead = 0;
    connectSapDevice( TDInstance.deviceA, mApp, data_type );
    // start a thread that keeps track of read packets
    // when read done stop it and return
    return 0;
  }

  // protected boolean startCommThread( int to_read, Handler /* ILister */ lister, int data_type ) 
  // {
  //   if ( mCommThread != null ) {
  //     TDLog.Error( "SAP comm start Comm Thread already running");
  //   }
  //   // Log.v("DistoX-BLE-S", "SAP comm start comm thread");
  //   mCommThread = new CommThread( TopoDroidComm.COMM_GATT, mProtocol, to_read, lister, data_type );
  //   mCommThread.start();
  //   return true;
  // }

}