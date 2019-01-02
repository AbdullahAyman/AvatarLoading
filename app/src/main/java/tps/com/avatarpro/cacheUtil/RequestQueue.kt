package tps.com.avatarpro.cacheUtil

import java.util.*

class RequestQueue<O> {
    var requestQueue: HashMap<String, ArrayList<RequestOwner<O>>>

    init {
        requestQueue = HashMap()
    }

    @Synchronized
    fun addToRequestQueue(url: String, newOrder: RequestOwner<O>) {

        if (requestQueue.get(url) == null) {
            val currentOwnerList = ArrayList<RequestOwner<O>>()
            currentOwnerList.add(newOrder)
            requestQueue[url] = currentOwnerList
        } else {
            var currentOwnerList: ArrayList<RequestOwner<O>>? = requestQueue[url]
            if (currentOwnerList != null) {
                currentOwnerList.add(newOrder)
                requestQueue[url] = currentOwnerList
            } else {
                currentOwnerList = ArrayList()
                currentOwnerList.add(newOrder)
                requestQueue[url] = currentOwnerList
            }
        }
    }

    @Synchronized
    fun removeFromRequestQueue(url: String) {
        requestQueue.remove(url)
    }

    fun getOwnerList(url: String): ArrayList<RequestOwner<O>>? {
        return requestQueue[url]
    }
}
