package tps.com.avatarpro.cacheUtil

import android.graphics.Bitmap
import android.support.v4.util.LruCache
import java.util.*

class BitmapCache private constructor(bitmapCacheBuilder: BitmapCacheBuilder) {
    internal var cacheSize: Int = 0
    internal var itemSnaps: ArrayList<ItemSnap>
    internal var lruCache: LruCache<String, BitmapObject>


    class BitmapCacheBuilder {
        var maxCacheMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt() / 4

        fun setMaxCacheMemory(maxCacheMemory: Int): BitmapCacheBuilder {
            this.maxCacheMemory = maxCacheMemory
            return this
        }

        fun build(): BitmapCache {
            return BitmapCache(this)
        }
    }

    init {
        itemSnaps = ArrayList()
        itemSnaps.clear()
        this.cacheSize = bitmapCacheBuilder.maxCacheMemory
        lruCache = object : LruCache<String, BitmapObject>(cacheSize) {
            override fun sizeOf(key: String, value: BitmapObject): Int {
                val size = (value!!.bitmap!!.byteCount.div(1024))
                return size
            }
        }
    }

    fun evictAll() {
        lruCache.evictAll()
        itemSnaps.clear()
    }

    fun addItemToCache(bitmapObject: BitmapObject) {
        if (lruCache.get(bitmapObject.url!!) != null)
            return
        if (itemSnaps.size > 50)
            itemSnaps.clear()
        val temp = ArrayList<ItemSnap>()
        temp.addAll(itemSnaps)
        for (item in temp) {
            if (item.url == bitmapObject.url) {
                itemSnaps.remove(item)
                break
            }
        }
        var itemSnap = ItemSnap(bitmapObject.url!!)
        itemSnaps.add(itemSnap)
        val newSize = lruCache.size() + bitmapObject.bitmap!!.byteCount / 1024
        if (newSize < cacheSize) {
            lruCache.put(bitmapObject.url!!, bitmapObject)
            if (itemSnaps.size == 0) {
                itemSnap = ItemSnap(bitmapObject.url!!)
                itemSnaps.add(itemSnap)
            }
        } else {
            evictLastItem()
            lruCache.put(bitmapObject.url!!, bitmapObject)
            if (itemSnaps.size == 0) {
                itemSnap = ItemSnap(bitmapObject.url!!)
                itemSnaps.add(itemSnap)
            }
        }
    }

    fun getItemBitmap(url: String): Bitmap? {
        //TO update last altered
        return if (lruCache.get(url) != null) {
            //addItemToCache(lruCache.get(url));
            lruCache.get(url)!!.bitmap
        } else
            null
    }

    fun evictItemCache(url: String) {
        val temp = ArrayList<ItemSnap>()
        temp.addAll(itemSnaps)
        for (item in temp) {
            if (item.url == url) {
                itemSnaps.remove(item)
            }
        }
        lruCache.remove(url)
    }

    fun evictLastItem() {
        if (itemSnaps.size > 0) {
            sortItemSnaps()
            evictItemCache(itemSnaps[itemSnaps.size - 1].url)
        }
    }

    private fun sortItemSnaps() {
        Collections.sort(itemSnaps) { o1, o2 -> o2.lastAltered!!.compareTo(o1.lastAltered!!) }

    }

    internal inner class ItemSnap(val url: String) {
        val lastAltered: Long?

        init {
            lastAltered = Date().time
        }
    }
}


