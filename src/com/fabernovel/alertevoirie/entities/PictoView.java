package com.fabernovel.alertevoirie.entities;

import com.fabernovel.alertevoirie.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class PictoView extends ImageView {

    private int      degree      = 0;
    private float    lastx       = 0, lasty = 0;
    private float    curx        = 0, cury = 0;

    Matrix           matrix      = new Matrix();
    Matrix           savedMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE        = 0;
    static final int DRAG        = 1;
    static final int ROTATE      = 2;
    int              mode        = NONE;
    private float    newcenterx  = 0;
    private float    newcentery  = 0;
    private boolean  firstmove   = false;

    public PictoView(Context context) {
        super(context);
        
    }

    public PictoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
    }

    public PictoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        dumpEvent(event);

        Log.d(Constants.PROJECT_TAG, "1:" + (event.getX() < (getWidth() / 2) + newcenterx + 50));
        Log.d(Constants.PROJECT_TAG, "2:" + (event.getX() > (getWidth() / 2) + newcenterx - 50));
        Log.d(Constants.PROJECT_TAG, "3:" + (event.getY() < (getHeight() / 2) + newcentery + 50));
        Log.d(Constants.PROJECT_TAG, "4:" + (event.getX() > (getHeight() / 2) + newcentery - 50));

        if (((event.getX() < (getWidth() / 2) + newcenterx + 50 && event.getX() > (getWidth() / 2) + newcenterx - 50) && (event.getY() < (getHeight() / 2)
                                                                                                                                         + newcentery + 50 && event.getY() > (getHeight() / 2)
                                                                                                                                                                             + newcentery
                                                                                                                                                                             - 50))) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if (lastx == 0 && lasty == 0) {
                        firstmove = true;
                        lastx = event.getX();
                        lasty = event.getY();

                    }
                    Log.d(Constants.PROJECT_TAG, "mode=DRAG");
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    Log.d(Constants.PROJECT_TAG, "mode=NONE");
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = ROTATE;
                    Log.d(Constants.PROJECT_TAG, "mode=ROTATE");
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        curx = event.getX();
                        cury = event.getY();
                    }
                    break;
            }
            
        }else
        {
            mode = ROTATE;
        }
        
        invalidate();

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        

        if (mode == DRAG) {
            canvas.rotate(degree, (getWidth() / 2) + newcenterx, (getHeight() / 2) + newcentery);
            newcenterx = curx - lastx;
            newcentery = cury - lasty;

        } else if (mode == ROTATE) {
            canvas.rotate(degree += 2, (getWidth() / 2) + newcenterx, (getHeight() / 2) + newcentery);

        } else {
            canvas.rotate(degree, (getWidth() / 2) + newcenterx, (getHeight() / 2) + newcentery);

        }

        if (firstmove) {
            firstmove = false;
            newcenterx = 0;
            newcentery = 0;
        }
        Log.d(Constants.PROJECT_TAG, "Translation : " + newcenterx + "," + newcentery);
        canvas.translate(newcenterx, newcentery);
        super.onDraw(canvas);
    }

    /**
     *  Show an event in the LogCat view, for debugging 
     * @param event
     */
    private void dumpEvent(MotionEvent event) {
        String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);
        if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }
        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount()) sb.append(";");
        }
        sb.append("]");
        Log.d(Constants.PROJECT_TAG, sb.toString());
    }
    
    /**
     * Method to link a photo to this arrow, giving the photo position on arrow layout
     * 
     * @param picture
     * @param x
     * @param y
     */
    public Drawable setSupport(Bitmap picture, float x, float y)
    {
        float arrow_x = (getWidth() / 2) + newcenterx-x;
        float arrow_y = (getHeight() / 2) + newcentery-y;
        
        LayerDrawable d = (LayerDrawable) getResources().getDrawable(R.drawable.editable_picture_frame);
        if (picture.getHeight() > picture.getWidth()) {
            Matrix m = new Matrix();
            m.postRotate(90);
            picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), m, true);
        }
        picture = Bitmap.createScaledBitmap(picture, d.getIntrinsicWidth(), d.getIntrinsicHeight(), true);

        d.setDrawableByLayerId(R.id.picture, new BitmapDrawable(picture));
        
        return d;
    }

}
