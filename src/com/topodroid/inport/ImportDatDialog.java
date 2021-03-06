/* @file ImportDatDialog.java
 *
 * @author marco corvi
 * @date nov 2014
 *
 * @brief TopoDroid VisualTopo import options dialog
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.inport;

import com.topodroid.utils.TDLog;
import com.topodroid.ui.MyDialog;
import com.topodroid.prefs.TDSetting;
import com.topodroid.DistoX.R;
import com.topodroid.DistoX.MainWindow;
import com.topodroid.DistoX.SurveyInfo;
import com.topodroid.DistoX.TDLevel;

import android.os.Bundle;

import android.content.Context;

import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import android.widget.CheckBox;

import java.io.InputStreamReader;

public class ImportDatDialog extends MyDialog
                      implements View.OnClickListener
{
  private MainWindow mParent;
  private Button mBtnOK;
  private Button mBtnCancel;

  private CheckBox mCBlrud;
  private CheckBox mCBleg;
  private CheckBox mCBdiving;

  private String mFilepath;

  private InputStreamReader isr;

  public ImportDatDialog( Context context, MainWindow parent, InputStreamReader isr, String filepath )
  {
    super( context, R.string.ImportDatDialog );
    mParent = parent;
    // mApp = app;
    mFilepath = filepath;
    this.isr = isr;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    
    initLayout( R.layout.import_dat_dialog, R.string.title_import_dat );

    TextView tv = (TextView) findViewById( R.id.dat_path );
    tv.setText( mFilepath );

    mCBlrud = (CheckBox) findViewById( R.id.dat_lrud );
    mCBleg  = (CheckBox) findViewById( R.id.dat_leg_first );
    mCBdiving = (CheckBox) findViewById( R.id.dat_diving_datamode );

    if ( TDLevel.overExpert ) {
      mCBdiving.setChecked( TDSetting.mImportDatamode == SurveyInfo.DATAMODE_DIVING );
    } else {
      mCBdiving.setVisibility( View.GONE );
    }

    mBtnOK     = (Button) findViewById(R.id.dat_ok);
    mBtnCancel = (Button) findViewById(R.id.dat_cancel);
    mBtnOK.setOnClickListener( this );
    mBtnCancel.setOnClickListener( this );

    // setTitle( R.string.title_calib );
  }

 
  // @Override
  public void onClick(View v) 
  {
    // TDLog.Log(  TDLog.LOG_INPUT, "ImportDatDialog onClick() " );
    Button b = (Button) v;
    hide();
    if ( b == mBtnOK ) {
      int datamode = SurveyInfo.DATAMODE_NORMAL;
      if ( TDLevel.overExpert && mCBdiving.isChecked() ) datamode = SurveyInfo.DATAMODE_DIVING;
      mParent.importDatFile( isr, mFilepath, datamode, mCBlrud.isChecked(), mCBleg.isChecked() );
    // } else if ( b == mBtnCancel ) {
    }
    dismiss();
  }

}
