package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.support.v7.graphics.Palette;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

class ImageLoaderHelper {
    private static final int MAX_COLORS = 16;
    private static ImageLoaderHelper sInstance;

    static ImageLoaderHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageLoaderHelper(context.getApplicationContext());
        }

        return sInstance;
    }

    private final LruCache<String, Bitmap> mImageCache = new LruCache<String, Bitmap>(20);
    private ImageLoader mImageLoader;

    private ImageLoaderHelper(Context applicationContext) {
        RequestQueue queue = Volley.newRequestQueue(applicationContext);
        ImageLoader.ImageCache imageCache = new ImageLoader.ImageCache() {
            @Override
            public void putBitmap(String key, Bitmap value) {
                mImageCache.put(key, value);
            }

            @Override
            public Bitmap getBitmap(String key) {
                return mImageCache.get(key);
            }
        };
        mImageLoader = new ImageLoader(queue, imageCache);
    }

    ImageLoader getImageLoader() {
        return mImageLoader;
    }

    void load(String url, final ImagePaletteLoaderCallback callback) {
        getImageLoader().get(url, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                final Bitmap bitmap = imageContainer.getBitmap();
                if (bitmap == null) {
                    return;
                }
                new Palette.Builder(bitmap).maximumColorCount(MAX_COLORS).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        callback.onImagePaletteLoaded(bitmap, palette);
                    }
                });
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {
                callback.onImagePaletteError(volleyError);
            }
        });
    }



    interface ImagePaletteLoaderCallback {
        void onImagePaletteLoaded(Bitmap bitmap, Palette palette);
        void onImagePaletteError(VolleyError error);
    }
}
