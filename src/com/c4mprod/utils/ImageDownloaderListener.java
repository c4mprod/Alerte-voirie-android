package com.c4mprod.utils;

import android.widget.ImageView;

/**
 * 
 * @author Pierre-Michel Villa
 *
 */
public interface ImageDownloaderListener {
    void onImageDownloaded(ImageView imageView, String distant_uri, String local_uri, long width, long height);
    

}
