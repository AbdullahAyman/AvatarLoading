package tps.com.avatarpro

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.androidnetworking.AndroidNetworking
import tps.com.avatarpro.cacheUtil.BitmapImageCaching
import tps.com.avatarpro.viewComponents.AvatarView

class MainActivity : AppCompatActivity() {

    lateinit var avatarView: AvatarView
    lateinit var loadBtn: Button
    lateinit var list: ArrayList<String>
    lateinit var imageLoadingRef: BitmapImageCaching
    var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupEnvironment()

        loadBtn = findViewById(R.id.main_loadBtn)
        avatarView = findViewById(R.id.main_avatarImg)
        loadBtn.setOnClickListener { it -> loadImage() }
    }

    private fun setupEnvironment() {
        //define new instance object of networking lib for downloading images
        AndroidNetworking.initialize(getApplicationContext())
        imageLoadingRef = BitmapImageCaching.getInstance()
        //list of images from nasa webservices
        list = ArrayList<String>()
        list.add("http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00152/opgs/edr/ncam/NLA_410988823EDR_F0051954NCAM00354M_.JPG")
        list.add("http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00152/opgs/edr/ncam/NLA_410988037EDR_D0051916TRAV00040M_.JPG")
        list.add("http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00152/opgs/edr/ncam/NLA_410988681EDR_F0051954NCAM05144M_.JPG")
    }

    private fun loadImage() {
        count++
        if (count >= list.size)
            count = 0
        imageLoadingRef
                .allowCaching(true)
                .loadUrl(list.get(count))
                .setImagePlaceHolder(R.drawable.ic_ninja)
                .setImageRef(avatarView)
                .execute()
    }
}
