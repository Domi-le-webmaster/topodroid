/* @file Selection.java
 *
 * @author marco corvi
 * @date feb 2013
 *
 * @brief Selection among drawing items
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * ----------------------------------------------------
 */
package com.topodroid.DistoX;

// import java.util.LinkedList;
// import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;

class Selection
{
  // LinkedList< SelectionPoint > mPoints;
  ArrayList< SelectionPoint > mPoints;

  Selection( )
  {
    // Log.v("DistoX", "Selection cstr" );
    // mPoints = new LinkedList< SelectionPoint >();
    mPoints = new ArrayList< SelectionPoint >();
  }

  void shiftSelectionBy( float x, float y )
  {
    for ( SelectionPoint sp : mPoints ) {
      int t = sp.type();
      if ( t == DrawingPath.DRAWING_PATH_POINT
        || t == DrawingPath.DRAWING_PATH_LINE
        || t == DrawingPath.DRAWING_PATH_AREA ) {
        sp.shiftSelectionBy(x, y);
      }
    }
  }

  void clearSelectionPoints()
  {
    // Log.v("DistoX", "Selection clear" );
    mPoints.clear();
  }

  // private void dumpPointsTypes()
  // {
  //   int nfxd = 0;
  //   int nspl = 0;
  //   int ngrd = 0;
  //   int nsta = 0;
  //   int npnt = 0;
  //   int nlin = 0;
  //   int nare = 0;
  //   int nnam = 0;
  //   int nnrt = 0;
  //   int noth = 0;
  //   for ( SelectionPoint p : mPoints ) {
  //     switch ( p.mItem.mType ) {
  //       case DrawingPath.DRAWING_PATH_FIXED:   nfxd++; break;
  //       case DrawingPath.DRAWING_PATH_SPLAY:   nspl++; break;
  //       case DrawingPath.DRAWING_PATH_GRID:    ngrd++; break;
  //       case DrawingPath.DRAWING_PATH_STATION: nsta++; break;
  //       case DrawingPath.DRAWING_PATH_POINT:   npnt++; break;
  //       case DrawingPath.DRAWING_PATH_LINE:    nlin++; break;
  //       case DrawingPath.DRAWING_PATH_AREA:    nare++; break;
  //       case DrawingPath.DRAWING_PATH_NAME:    nnam++; break;
  //       case DrawingPath.DRAWING_PATH_NORTH:   nnrt++; break;
  //       default: noth ++;
  //     }
  //   }
  //   Log.v("DistoX", "Selection points " + mPoints.size() + " " + nfxd + " " + nspl + " " + ngrd + " " + nsta 
  //                   + " " + npnt + " " + nlin + " " + nare + " " + nnam + " " + nnrt + " " + noth  );
  // }

  void clearReferencePoints()
  {
    synchronized ( mPoints ) {
      Iterator< SelectionPoint > it = mPoints.iterator();
      while( it.hasNext() ) {
        SelectionPoint sp1 = (SelectionPoint)it.next();
        if ( sp1.isReferenceType() ) {
          it.remove( );
        }
      }
    }
  }

  void clearDrawingPoints()
  {
    synchronized ( mPoints ) {
      Iterator< SelectionPoint > it = mPoints.iterator();
      while( it.hasNext() ) {
        SelectionPoint sp1 = (SelectionPoint)it.next();
        if ( sp1.isDrawingType() ) {
          it.remove( );
        }
      }
    }
  }

  void insertStationName( DrawingStationName st )
  {
    insertItem( st, null );
  }

  /** like insertItem, but it returns the inserted SelectionPoint
   * @param path     point-line path
   * @param point    new point on the point-line
   * @return
   */
  SelectionPoint insertPathPoint( DrawingPointLinePath path, LinePoint pt )
  {
    // Log.v("DistoX", "Selection insert path point" );
    SelectionPoint sp = new SelectionPoint( path, pt );
    mPoints.add( sp );
    return sp;
  }
  
  void insertLinePath( DrawingLinePath path )
  {
    for ( LinePoint p2 = path.mFirst; p2 != null; p2 = p2.mNext ) {
      insertItem( path, p2 );
    }
  }

  void insertPath( DrawingPath path )
  {
    // Log.v("DistoX", "Selection insert path" );
    // LinePoint p1;
    LinePoint p2;
    switch ( path.mType ) {
      case DrawingPath.DRAWING_PATH_FIXED:
      case DrawingPath.DRAWING_PATH_SPLAY:
        insertItem( path, null );
        break;
      case DrawingPath.DRAWING_PATH_GRID:
        // nothing
        break;
      case DrawingPath.DRAWING_PATH_STATION:
        insertItem( path, null );
        break;
      case DrawingPath.DRAWING_PATH_POINT:
        insertItem( path, null );
        break;
      case DrawingPath.DRAWING_PATH_LINE:
        DrawingLinePath lp = (DrawingLinePath)path;
        for ( p2 = lp.mFirst; p2 != null; p2 = p2.mNext ) {
          insertItem( path, p2 );
        }
        break;
      case DrawingPath.DRAWING_PATH_AREA:
        DrawingAreaPath ap = (DrawingAreaPath)path;
        for ( p2 = ap.mFirst; p2 != null; p2 = p2.mNext ) {
          insertItem( path, p2 );
        }
        break;
      default:
    }
  }

  void resetDistances()
  {
    // Log.v("DistoX", "Selection reset distances" );
    for ( SelectionPoint pt : mPoints ) {
      pt.mDistance = 0.0f;
    }
  }

  private void insertItem( DrawingPath path, LinePoint pt )
  {
    mPoints.add( new SelectionPoint( path, pt ) );
    // Log.v("DistoX", "selection inserted path type " + path.mType + " pts " + mPoints.size() );
  }

  SelectionPoint getNearestPoint( SelectionPoint sp, float x, float y, float dmin )
  {
    SelectionPoint spmin = null;
    // final ListIterator it = mPoints.listIterator(0);
    final Iterator it = mPoints.iterator();
    while( it.hasNext() ) {
      SelectionPoint sp1 = (SelectionPoint)it.next();
      if ( sp == sp1 ) continue;
      float d = sp1.distance( x, y );
      if ( d < dmin ) {
        dmin = d;
        spmin = sp1;
      }
    }
    return spmin;
  }

  void removePoint( SelectionPoint sp )
  {
    // Log.v("DistoX", "Selection remove point" );
    mPoints.remove( sp ); 
    // final ListIterator it = mPoints.listIterator(0);
    // while( it.hasNext() ) {
    //   if ( sp == (SelectionPoint)it.next() ) {
    //     it.remove();
    //     return;
    //   }
    // }
  }

  void removePath( DrawingPath path )
  {
    // Log.v("DistoX", "Selection remove path" );
    if ( path.mType == DrawingPath.DRAWING_PATH_LINE || path.mType == DrawingPath.DRAWING_PATH_AREA ) {

      DrawingPointLinePath line = (DrawingPointLinePath)path;
      for ( LinePoint lp = line.mFirst; lp != null; lp = lp.mNext ) {
        for ( SelectionPoint sp : mPoints ) {
          if ( sp.mPoint == lp ) {
            mPoints.remove( sp );
            break;
          }
        }
      }

      // final ListIterator it = mPoints.listIterator(0);
      // while( it.hasNext() ) {
      //   SelectionPoint sp = (SelectionPoint)it.next();
      //   if ( path == sp.mItem ) {
      //     it.remove(); // remove selection point
      //   }
      // }

    } else if ( path.mType == DrawingPath.DRAWING_PATH_POINT ) {  
      for ( SelectionPoint sp : mPoints ) {
        if ( sp.mItem == path ) {
          mPoints.remove( sp );
          break;
        }
      }
    }
  }

  void removeLinePoint( DrawingPointLinePath path, LinePoint lp )
  {
    // Log.v("DistoX", "Selection remove line point" );
    if ( path.mType != DrawingPath.DRAWING_PATH_LINE && path.mType != DrawingPath.DRAWING_PATH_AREA ) return;
    // final Iterator i = mPoints.iterator();
    for ( SelectionPoint sp : mPoints ) {
      if ( sp.mPoint == lp ) {
        mPoints.remove( sp );
        return;
      }
    }
  }

  // void removeLinePath( DrawingLinePath path )
  // {
  //   final Iterator i = mPoints.iterator();
  //   while ( i.hasNext() ) {
  //     final SelectionPoint sp = (SelectionPoint) i.next();
  //     if ( sp.mItem == path ) {
  //       // Log.v(TopoDroidApp.TAG, "sel. remove " + sp.mPoint.mX + " " + sp.mPoint.mY );
  //       mPoints.remove( i ); // FIXME
  //     }
  //   }
  // }

  void selectAt( float x, float y, float zoom, SelectionSet sel, boolean legs, boolean splays, boolean stations )
  {
    float radius = TopoDroidSetting.mCloseCutoff + TopoDroidSetting.mCloseness / zoom;
    // Log.v( "DistoX", "selection select at " + x + " " + y + " pts " + mPoints.size() + " " + legs + " " + splays + " " + stations + " radius " + radius );
    for ( SelectionPoint sp : mPoints ) {
      if ( !legs && sp.type() == DrawingPath.DRAWING_PATH_FIXED ) continue;
      if ( !splays && sp.type() == DrawingPath.DRAWING_PATH_SPLAY ) continue;
      if ( !stations && ( sp.type() == DrawingPath.DRAWING_PATH_STATION || sp.type() == DrawingPath.DRAWING_PATH_NAME ) ) continue;
      sp.mDistance = sp.distance(x, y);
      // Log.v("DistoX", "sp " + sp.name() + " distance " + sp.mDistance );
      if ( sp.mDistance < radius ) {
        sel.addPoint( sp );
      }
    }
    // Log.v(TopoDroidApp.TAG, "selectAt " + sel.size() );
  }

}
