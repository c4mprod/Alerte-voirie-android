/**
 * This file is part of the Alerte Voirie project.
 * 
 * Copyright (C) 2010-2011 C4M PROD
 * 
 * Alerte Voirie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alerte Voirie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alerte Voirie.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.fabernovel.alertevoirie.entities;

import java.io.FileNotFoundException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class PictoView extends ImageView {

    private int      degree      = 0;
    private float    lastx       = 0, lasty = 0;

    Matrix           matrix      = new Matrix();
    Matrix           savedMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE        = 0;
    static final int DRAG        = 1;
    static final int ROTATE      = 2;
    int              mode        = NONE;
    private float    offsetx     = 0;
    private float    offsety     = 0;
    private boolean  firstmove   = false;
    Bitmap           arrow;

    public PictoView(Context context) {
        super(context);
        setDrawingCacheEnabled(true);

    }

    public PictoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDrawingCacheEnabled(true);
    }

    public PictoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDrawingCacheEnabled(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // dumpEvent(event);

        Log.d(Constants.PROJECT_TAG, "1:" + (event.getX() < (getWidth() / 2) + offsetx + 50));
        Log.d(Constants.PROJECT_TAG, "2:" + (event.getX() > (getWidth() / 2) + offsetx - 50));
        Log.d(Constants.PROJECT_TAG, "3:" + (event.getY() < (getHeight() / 2) + offsety + 50));
        Log.d(Constants.PROJECT_TAG, "4:" + (event.getX() > (getHeight() / 2) + offsety - 50));

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (((event.getX() < (getWidth() / 2) + offsetx + 100 && event.getX() > (getWidth() / 2) + offsetx - 100) && (event.getY() < (getHeight() / 2)
                                                                                                                                             + offsety + 100 && event.getY() > (getHeight() / 2)
                                                                                                                                                                               + offsety
                                                                                                                                                                               - 100))) {
                    if (lastx == 0 && lasty == 0) {
                        firstmove = true;
                    }
                    lastx = event.getX();
                    lasty = event.getY();

                    Log.d(Constants.PROJECT_TAG, "mode=DRAG");
                    mode = DRAG;
                } else {
                    mode = ROTATE;
                }
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
                    float deltax = event.getX() - lastx;
                    float deltay = event.getY() - lasty;
                    lastx = event.getX();
                    lasty = event.getY();
                    offsetx += deltax;
                    offsety += deltay;
                    // constrain arrow in view
                    int halfWidth = getWidth() / 2;
                    int halfHeight = getHeight() / 2;
                    if (offsetx < -halfWidth) {
                        offsetx = -halfWidth;
                    } else if (offsetx > halfWidth) {
                        offsetx = halfWidth;
                    }
                    if (offsety < -halfHeight) {
                        offsety = -halfHeight;
                    } else if (offsety > halfHeight) {
                        offsety = halfHeight;
                    }
                    invalidate();
                }
                break;
        }

        invalidate();

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (arrow == null) {
            arrow = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_4444);
            canvas.setBitmap(arrow);
        }

        if (mode == DRAG) {
            canvas.rotate(degree, (getWidth() / 2) + offsetx, (getHeight() / 2) + offsety);

        } else if (mode == ROTATE) {
            canvas.rotate(degree += 2, (getWidth() / 2) + offsetx, (getHeight() / 2) + offsety);

        } else {
            canvas.rotate(degree, (getWidth() / 2) + offsetx, (getHeight() / 2) + offsety);

        }

        if (firstmove) {
            firstmove = false;
            offsetx = 0;
            offsety = 0;
        }
        Log.d(Constants.PROJECT_TAG, "Translation : " + offsetx + "," + offsety);
        canvas.translate(offsetx, offsety);

        super.onDraw(canvas);

    }

    /**
     * Show an event in the LogCat view, for debugging
     * 
     * @param event
     */
    @SuppressWarnings("unused")
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
     * @throws FileNotFoundException
     */
    public Bitmap setSupport(Bitmap picture, float targetx, float targety, Context c) throws FileNotFoundException {
        // 320*480

        Bitmap arrow = ((BitmapDrawable) this.getDrawable()).getBitmap();

        float coeffx = targetx / getWidth();
        float coeffy = targety / getHeight();
        int scaledOffsetX = (int) (offsetx * coeffx);
        int scaledOffsetY = (int) (offsety * coeffy);

        // createa matrix for the manipulation
        Matrix matrix = new Matrix();
        // rotate the Bitmap
        matrix.postRotate(degree, (arrow.getWidth() / 2), (arrow.getHeight() / 2));
        // recreate the new Bitmap
        arrow = Bitmap.createBitmap(arrow, 0, 0, arrow.getWidth(), arrow.getHeight(), matrix, true);

        Bitmap b = picture.copy(picture.getConfig(), true);
        Canvas myCanvas = new Canvas(b);

        // and then just draw them on canvas
        myCanvas.drawBitmap(arrow, (b.getWidth() - arrow.getWidth()) / 2 + scaledOffsetX, (b.getHeight() - arrow.getHeight()) / 2 + scaledOffsetY, new Paint());

        b.compress(CompressFormat.JPEG, 80, c.openFileOutput("arrowed.jpg", Context.MODE_PRIVATE));
        return b;
    }

}
