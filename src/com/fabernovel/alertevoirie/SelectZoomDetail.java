package com.fabernovel.alertevoirie;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.fabernovel.alertevoirie.entities.Constants;
import com.fabernovel.alertevoirie.entities.PictoView;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;


public class SelectZoomDetail extends Activity {
   
    ImageView photo;
    Bitmap picture;
    float photo_width,photo_heigth,layout_with, layout_height;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.layout_report_picture_zoom);
        
        photo = (( ImageView)findViewById(R.id.ImageViewPictoBG));
        
        setPictureToImageView("capture_far", photo);
        
      
       
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon_nouveau_rapport);
        
       ((Button) findViewById(R.id.ButtonViewPicto)).setOnClickListener(new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            ((PictoView)findViewById(R.id.ViewPicto)).setSupport(picture, ((photo_width+layout_with)/2)-photo_width, ((photo_heigth+layout_height)/2)-photo_heigth);
            
        }
    });

      
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
    }
    
    
    private void setPictureToImageView(String pictureName, ImageView imageView) {
        picture = null;

        try {
            InputStream in = openFileInput(pictureName);
            picture = BitmapFactory.decodeStream(in);
            in.close();

            if (picture.getWidth() > picture.getHeight()) {
                Matrix m = new Matrix();
                m.postRotate(90);
                picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), m, true);
            }
            imageView.setImageBitmap(picture);
            
Log.d(Constants.PROJECT_TAG,picture.getWidth()+","+picture.getHeight());
            
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}