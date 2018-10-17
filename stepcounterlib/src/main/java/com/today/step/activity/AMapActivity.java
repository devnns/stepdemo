package com.today.step.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.amap.api.location.AMapLocation;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolylineOptions;
import com.today.step.lib.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class AMapActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
    private TextView tvMsg;
    private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private MapView mapView;
    private AMap aMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amap);
        tvMsg = findViewById(R.id.tv_msg);
        mapView = findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        aMap.setMyLocationEnabled(false);
        aMap.setMyLocationStyle(getMyLocationStyle());//设置定位蓝点的Style
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17));//数字越小，视野越大
        //默认显示上海
//        aMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(31.232159016927085, 121.49405734592014)));
        List<LatLng> points = getIntent().getParcelableArrayListExtra("trace");
        if (points != null && points.size() != 0) {
            aMap.moveCamera(CameraUpdateFactory.newLatLng(points.get(points.size() - 1)));
            aMap.addPolyline(new PolylineOptions().addAll(points).width(10).color(Color.argb(255, 1, 1, 1)));
        }
    }


    private MyLocationStyle getMyLocationStyle() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);//生成透明bitmap
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
//        myLocationStyle.showMyLocation(false);此方法有bug，无效，所以要添加一个透明的bitmap
//        myLocationStyle.radiusFillColor(Color.TRANSPARENT);
//        myLocationStyle.strokeColor(Color.TRANSPARENT);
        return myLocationStyle;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
