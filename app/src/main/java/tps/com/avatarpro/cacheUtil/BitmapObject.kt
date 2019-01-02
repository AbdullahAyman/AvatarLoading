package tps.com.avatarpro.cacheUtil

import android.graphics.Bitmap
import java.util.*

class BitmapObject private constructor(builder: Builder) {

    var url: String?
    var alteredDate: Long?
    var bitmap: Bitmap?

    init {
        url = builder.url
        alteredDate = builder.alteredDate
        bitmap = builder.bitmap
    }

    class Builder {
        var url: String? = null
        var alteredDate: Long? = null
        var bitmap: Bitmap? = null

        fun setUrl(url: String): Builder {
            this.url = url
            return this
        }

        fun setAlteredDate(): Builder {
            this.alteredDate = Date().time
            return this
        }

        fun setBitmap(bitmap: Bitmap): Builder {
            this.bitmap = bitmap
            return this
        }

        fun create(): BitmapObject {
            return BitmapObject(this)
        }
    }
}
