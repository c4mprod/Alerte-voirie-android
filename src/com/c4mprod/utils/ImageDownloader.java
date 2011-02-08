package com.c4mprod.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

/**
 * This helper class download images from the Internet and binds those with the provided ImageView.
 * A local cache of downloaded images is maintained internally to improve performance and on sdcard.
 * 
 * <p>
 * It requires the INTERNET permission, which should be added to your application's manifest file.
 * </p>
 * 
 * 
 * 
 * Inspired by {@link http://android-developers.blogspot.com/2010/07/multithreading-for-performance.html}
 * 
 * 
 * @version 1.3
 * @author Pierre-Michel Villa
 * @author Alex Kirchen
 * @author Gilles Debunne @ google
 */
public class ImageDownloader {
    private static final String     LOG_TAG                   = "ImageDownloader";
    private static final String     state                     = Environment.getExternalStorageState();
    private String                  mfolder;
    private String                  mSubfolder;
    private boolean                 mExternalStorageAvailable = false;
    private boolean                 mExternalStorageWriteable = false;
    private ImageDownloaderListener listener                  = null;

    public enum Mode {
        NO_ASYNC_TASK, NO_DOWNLOADED_DRAWABLE, CORRECT
    }

    private BitmapDrawable default_img    = null;

    // Always should be correct. Mode NO_ASYNC_TASK and NO_DOWNLOAD_DRAWABLE are for test purpose only
    private Mode           mode           = Mode.CORRECT;
    private boolean        keepBackground = true;

    public ImageDownloader() {
        this.listener = null;
    }

    public ImageDownloader(ImageDownloaderListener listener) {
        this.listener = listener;
    }

    /**
     * Download the specified image from the Internet and binds it to the provided ImageView. The
     * binding is immediate if the image is found in the cache and will be done asynchronously
     * otherwise. A null bitmap will be associated to the ImageView if an error occurs.
     * 
     * @param url
     *            The URL of the image to download.
     * @param imageView
     *            The ImageView to bind the downloaded image to.
     * 
     * @see download(String url, ImageView imageView, String subfolder)
     * 
     */
    public void download(String url, ImageView imageView) {
        download(url, imageView, null);
    }

    /**
     * Download the specified image from the Internet and binds it to the provided ImageView. The
     * binding is immediate if the image is found in the cache and will be done asynchronously
     * otherwise. A null bitmap will be associated to the ImageView if an error occurs.
     * 
     * @param url
     * @param imageView
     * @param subfolder
     *            the subfolder in sdcard cache
     * 
     */
    public void download(String url, ImageView imageView, String subfolder) {
        if (subfolder != null) {
            mSubfolder = "/" + subfolder;
        } else {
            mSubfolder = "";
        }

        mfolder = Environment.getExternalStorageDirectory().getPath() + "/Android/data/" + imageView.getContext().getPackageName() + "/files/images"
                  + mSubfolder;

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
            try {
                (new File(mfolder)).mkdirs();
                (new File(mfolder + "/.nomedia")).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        resetPurgeTimer();
        Bitmap bitmap = getBitmapFromCache(url);

        if (bitmap == null) {
            forceDownload(url, imageView);
        } else {
            cancelPotentialDownload(url, imageView);
            imageView.setImageBitmap(bitmap);
            imageView.setBackgroundDrawable(null);
            if (listener != null) {
                listener.onImageDownloaded(imageView, url, mfolder + "/" + URLEncoder.encode(url), imageView.getDrawable().getIntrinsicWidth(),
                                           imageView.getDrawable().getIntrinsicWidth());
            }
        }
    }

    /**
     * Same as download but the image is always downloaded and the cache is not used.
     * Kept private at the moment as its interest is not clear.
     */
    private void forceDownload(String url, ImageView imageView) {
        // State sanity: url is guaranteed to never be null in DownloadedDrawable and cache keys.
        if (url == null) {
            imageView.setImageDrawable(null);
            return;
        }

        if (cancelPotentialDownload(url, imageView)) {
            switch (mode) {
                case NO_ASYNC_TASK:
                    Bitmap bitmap = downloadBitmap(url);
                    addBitmapToCache(url, bitmap);
                    imageView.setImageBitmap(bitmap);
                    break;

                case NO_DOWNLOADED_DRAWABLE:
                    imageView.setMinimumHeight(156);
                    BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
                    task.execute(url);
                    break;

                case CORRECT:
                    task = new BitmapDownloaderTask(imageView);
                    imageView.setBackgroundDrawable((default_img));
                    DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                    imageView.setImageDrawable(downloadedDrawable);

                    imageView.setMinimumHeight(156);
                    task.execute(url);
                    break;
            }
        }
    }

    /**
     * Returns true if the current download has been canceled or if there was no download in
     * progress on this image view.
     * Returns false if the download in progress deals with the same url. The download is not
     * stopped in that case.
     */
    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                bitmapDownloaderTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView
     *            Any imageView
     * @return Retrieve the currently active download task (if any) associated with this imageView.
     *         null if there is no such task.
     */
    private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    public BitmapDrawable getDefault_img() {
        return default_img;
    }

    public void setDefault_img(Bitmap bitmap) {
        this.default_img = new BitmapDrawable(bitmap);
    }

    public void setDefault_img(Drawable drawable) {
        this.default_img = ((BitmapDrawable) drawable);
    }

    Bitmap downloadBitmap(String url) {
        // AndroidHttpClient is not allowed to be used from the main thread
        final HttpClient client = (mode == Mode.NO_ASYNC_TASK) ? new DefaultHttpClient() : AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        try {

            // Log.i(Constants.PROJECT_TAG, "Downloading " + url);
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                // Log.w(Constants.PROJECT_TAG, "Error " + statusCode + " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();

                    // Log.d(Constants.PROJECT_TAG, "image url:" + urlString);
                    Drawable drawable = Drawable.createFromStream(inputStream, "src");

                    Bitmap bm = ((BitmapDrawable) drawable).getBitmap();

                    // Log.d(Constants.PROJECT_TAG, "Bm width : " + bm.getWidth());

                    // Log.d(Constants.PROJECT_TAG, "got a thumbnail drawable: " + drawable.getBounds() + ", " + drawable.getIntrinsicHeight() + ","
                    // + drawable.getIntrinsicWidth() + ", " + drawable.getMinimumHeight() + "," + drawable.getMinimumWidth());
                    if (mExternalStorageWriteable) {
                        File f = new File(mfolder, URLEncoder.encode(url));
                        // Log.d(Constants.PROJECT_TAG, f.getPath());
                        if (f.createNewFile()) {

                            OutputStream out = new FileOutputStream(f);

                            bm.compress(Bitmap.CompressFormat.JPEG, 80, out);
                            out.flush();
                            out.close();
                            // byte buf[]=new byte[1024];
                            // int len;
                            // while((len=is.read(buf))>0)
                            // out.write(buf,0,len);
                            out.close();

                        }
                    }
                    inputStream.close();
                    return bm;

                    // return BitmapFactory.decodeStream(inputStream);
                    // Bug on slow connections, fixed in future release.
                    // return BitmapFactory.decodeStream(new FlushedInputStream(inputStream));
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (IOException e) {
            getRequest.abort();
            Log.w(LOG_TAG, "I/O error while retrieving bitmap from " + url, e);
        } catch (IllegalStateException e) {
            getRequest.abort();
            Log.w(LOG_TAG, "Incorrect URL: " + url);
        } catch (Exception e) {
            getRequest.abort();
            Log.w(LOG_TAG, "Error while retrieving bitmap from " + url, e);
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
            }
        }
        return null;
    }

    /*
     * An InputStream that skips the exact number of bytes provided, unless it reaches EOF.
     */
    static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break; // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    /**
     * The actual AsyncTask that will asynchronously download the image.
     */
    class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
        private String                         url;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapDownloaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        /**
         * Actual download method.
         */
        @Override
        protected Bitmap doInBackground(String... params) {
            url = params[0];
            return downloadBitmap(url);
        }

        /**
         * Once the image is downloaded, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            addBitmapToCache(url, bitmap);

            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
                // Change bitmap only if this process is still associated with it
                // Or if we don't use any bitmap to task association (NO_DOWNLOADED_DRAWABLE mode)
                if ((this == bitmapDownloaderTask) || (mode != Mode.CORRECT)) {
                    imageView.setImageBitmap(bitmap);
                    imageView.setBackgroundDrawable(null);

                    if (listener != null) {
                        listener.onImageDownloaded(imageView, url, mfolder + "/" + URLEncoder.encode(url), imageView.getDrawable().getIntrinsicWidth(),
                                                   imageView.getDrawable().getIntrinsicHeight());
                    }
                }
            }
        }
    }

    /**
     * A fake Drawable that will be attached to the imageView while the download is in progress.
     * 
     * <p>
     * Contains a reference to the actual download task, so that a download task can be stopped if a new binding is required, and makes sure that only the last
     * started download process can bind its result, independently of the download finish order.
     * </p>
     */
    static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

        public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
            super(Color.TRANSPARENT);
            bitmapDownloaderTaskReference = new WeakReference<BitmapDownloaderTask>(bitmapDownloaderTask);
        }

        public BitmapDownloaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        clearCache();
    }

    /*
     * Cache-related fields and methods.
     * We use a hard and a soft cache. A soft reference cache is too aggressively cleared by the
     * Garbage Collector.
     */

    private static final int                                              HARD_CACHE_CAPACITY = 10;
    private static final int                                              DELAY_BEFORE_PURGE  = 10 * 1000;                                                                    // in
                                                                                                                                                                               // milliseconds

    // Hard cache, with a fixed maximum capacity and a life duration
    private final HashMap<String, Bitmap>                                 sHardBitmapCache    = new LinkedHashMap<String, Bitmap>(HARD_CACHE_CAPACITY / 2,
                                                                                                                                  0.75f, true) {

                                                                                                  private static final long serialVersionUID = 244560809668581534L;

                                                                                                  @Override
                                                                                                  protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
                                                                                                      if (size() > HARD_CACHE_CAPACITY) {
                                                                                                          // Entries push-out of hard reference cache are
                                                                                                          // transferred to soft reference cache
                                                                                                          sSoftBitmapCache.put(eldest.getKey(),
                                                                                                                               new SoftReference<Bitmap>(
                                                                                                                                                         eldest.getValue()));
                                                                                                          return true;
                                                                                                      } else {
                                                                                                          return false;
                                                                                                      }
                                                                                                  }
                                                                                              };

    // Soft cache for bitmaps kicked out of hard cache
    private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache    = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
                                                                                                                                                     HARD_CACHE_CAPACITY / 2);

    private final Handler                                                 purgeHandler        = new Handler();

    private final Runnable                                                purger              = new Runnable() {
                                                                                                  public void run() {
                                                                                                      clearCache();
                                                                                                  }
                                                                                              };

    /**
     * Adds this bitmap to the cache.
     * 
     * @param bitmap
     *            The newly downloaded bitmap.
     */
    private void addBitmapToCache(String url, Bitmap bitmap) {
        if (bitmap != null) {
            synchronized (sHardBitmapCache) {
                sHardBitmapCache.put(url, bitmap);
            }
        }
    }

    /**
     * @param url
     *            The URL of the image that will be retrieved from the cache.
     * @return The cached bitmap or null if it was not found.
     */
    private Bitmap getBitmapFromCache(String url) {

        if (mExternalStorageAvailable && url != null) {
            File f = new File(mfolder, URLEncoder.encode(url));

            if (f.exists()) {
                try {
                    return BitmapFactory.decodeFile(f.getPath());

                } catch (Exception e) {
                    // Log.e(Constants.PROJECT_TAG, "Error in retrieving picture", e);
                    e.printStackTrace();
                }
            }
        }

        // First try the hard reference cache
        synchronized (sHardBitmapCache) {
            final Bitmap bitmap = sHardBitmapCache.get(url);
            if (bitmap != null) {
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
                sHardBitmapCache.remove(url);
                sHardBitmapCache.put(url, bitmap);
                return bitmap;
            }
        }

        // Then try the soft reference cache
        try {
            SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
            if (bitmapReference != null) {
                final Bitmap bitmap = bitmapReference.get();
                if (bitmap != null) {
                    // Bitmap found in soft cache
                    return bitmap;
                } else {
                    // Soft reference has been Garbage Collected
                    sSoftBitmapCache.remove(url);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /**
     * Clears the image cache used internally to improve performance. Note that for memory
     * efficiency reasons, the cache will automatically be cleared after a certain inactivity delay.
     */
    public void clearCache() {
        sHardBitmapCache.clear();
        sSoftBitmapCache.clear();
    }

    /**
     * Allow a new delay before the automatic cache clear is done.
     */
    private void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }

}