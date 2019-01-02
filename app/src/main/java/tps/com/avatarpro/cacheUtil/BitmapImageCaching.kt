package tps.com.avatarpro.cacheUtil

import android.graphics.Bitmap
import android.widget.ImageView
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.BitmapRequestListener
import tps.com.avatarpro.viewComponents.AvatarView


class BitmapImageCaching private constructor() {
    private var uRl: String? = null
    private var imageRef: AvatarView? = null
    private var cacheEnabled: Boolean? = false
    var placeholder: Int? = null
    fun loadUrl(url: String): BitmapImageCaching {
        this.uRl = url
        return this
    }

    fun setImageRef(image: AvatarView): BitmapImageCaching {
        this.imageRef = image
        return this
    }


    fun setImagePlaceHolder(image: Int): BitmapImageCaching {
        placeholder = image
        return this
    }

    fun getImageRef(): ImageView? {
        return this.imageRef
    }

    fun allowCaching(value: Boolean): BitmapImageCaching {
        cacheEnabled = value
        if (value) {
            val maxCacheMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt() / 4
            if (bitmapCache == null)
                bitmapCache = BitmapCache.BitmapCacheBuilder().setMaxCacheMemory(maxCacheMemory).build()
        } else {
            bitmapCache = null
        }
        return this
    }

    private fun resetAnimationProgress() {
        imageRef?.progress = 0.0f
        imageRef?.currentProgress = 0.0f
    }

    private fun startAnimationProgress(max: Float) {
        imageRef?.progress = max
        imageRef?.startAnimation()
    }


    @Synchronized
    fun execute() {
        if (placeholder != null) {
            //add place holder if found
            this.imageRef?.setImageResource(placeholder!!)
        }
        resetAnimationProgress()
        if (bitmapCache != null) {
            val bitmap = bitmapCache?.getItemBitmap(uRl!!)
            if (bitmap != null) {
                startAnimationProgress(100.0f)
                getImageRef()?.setImageBitmap(bitmap)
                return
            }
        }
        startAnimationProgress(45.0f)

        //TODO load file
        if (imageViewBitmapRequestQueue?.getOwnerList(uRl!!) == null) {
            imageViewBitmapRequestQueue?.addToRequestQueue(uRl!!, RequestOwner<ImageView>(getImageRef()!!))

            AndroidNetworking.get(uRl)
                    .setTag(uRl)
                    .build()
                    .getAsBitmap(object : BitmapRequestListener {
                        override fun onResponse(response: Bitmap) {
                            startAnimationProgress(100.0f)
                            val ownerList = imageViewBitmapRequestQueue?.getOwnerList(uRl!!)
                            if (ownerList != null)
                                for (ref in ownerList) {
                                    ref._Ref.setImageBitmap(response)
                                }
                            else
                                getImageRef()?.setImageBitmap(response)
                            if (cacheEnabled!!) {
                                val bitmapObject = BitmapObject.Builder()
                                        .setUrl(uRl!!)
                                        .setBitmap(response)
                                        .setAlteredDate()
                                        .create()
                                bitmapCache?.addItemToCache(bitmapObject)

                            }
                            imageViewBitmapRequestQueue?.removeFromRequestQueue(uRl!!)
                        }

                        override fun onError(anError: ANError) {
                            imageViewBitmapRequestQueue?.removeFromRequestQueue(uRl!!)

                        }
                    })
        }
    }

    companion object {
        private var imageViewBitmapRequestQueue: RequestQueue<ImageView>? = null
        private var instance: BitmapImageCaching? = null
        private var bitmapCache: BitmapCache? = null

        fun getInstance(): BitmapImageCaching {
            if (instance == null) {
                synchronized(BitmapImageCaching::class.java) {
                    if (instance == null) {
                        instance = BitmapImageCaching()
                    }
                }
                instance = BitmapImageCaching()
                bitmapCache = BitmapCache.BitmapCacheBuilder().build()
                imageViewBitmapRequestQueue = RequestQueue()
            }
            return instance!!
        }
    }

}
