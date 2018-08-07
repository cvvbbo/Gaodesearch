package com.gaodesearch;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

public class MainActivity extends AppCompatActivity implements PoiSearch.OnPoiSearchListener,
        AMapLocationListener,AMap.OnCameraChangeListener {


    public AMapLocationClient mLocationClient = null;

    public AMapLocationClientOption mLocationOption = null;

    private double latitude, longitude;
    private String cityName;

    private ProgressDialog progressDialog;
    private AMap aMap;
    private MapView mMapView;

    private MyLocationStyle myLocationStyle;

    private boolean isNeedCheck = true;

    private static final int PERMISSON_REQUESTCODE = 0;

    private String DINING_ROOM = "餐厅";

    private boolean isfrist=false;

    private LatLng latlng = new LatLng(39.761, 116.434);
    private MarkerOptions markerOption;

    private int with=600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = (MapView) this.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        initLocation();
    }


    //添加market点
    private void addMarkersToMap() {
        markerOption = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_car))
                .draggable(true);
        //通过markerOption设置完market的相关参数，然后再得到market对象。
        Marker marker = aMap.addMarker(markerOption);
        marker.setPositionByPixels(with,with);
    }


    private void initLocation() {
        if (aMap == null) {
            aMap = mMapView.getMap();
            addMarkersToMap();
        }
        aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                aMap.moveCamera(CameraUpdateFactory.zoomTo(16));
            }
        });
        //指南针
        aMap.getUiSettings().setCompassEnabled(true);
        //显示默认的定位按钮
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        //显示实时交通状况(默认地图)
        aMap.setTrafficEnabled(true);
        //监听地图改变的回调
        aMap.setOnCameraChangeListener(this);
        //地图模式-标准地图：MAP_TYPE_NORMAL、卫星地图：MAP_TYPE_SATELLITE
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
        myLocationStyle = new MyLocationStyle();
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0)).radiusFillColor(Color.argb(0, 0, 0, 0));
        //设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMap.setMyLocationEnabled(true);
        //定位一次，且将视角移动到地图中心点。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        //设置定位蓝点的Style
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setPointToCenter(with,with);

        //动态设置权限
        if (isNeedCheck) {
            if (PermissionsUtils.checkPermissions(this, PERMISSON_REQUESTCODE, PermissionsUtils.locationPermissions)) {
                initaion();
            }
        } else {
            initaion();
        }




    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSON_REQUESTCODE) {
            if (!PermissionsUtils.verifyPermissions(grantResults)) {
                isNeedCheck = false;
            } else {
                initaion();
            }
        }
    }

    //定位参数相关
    private void initaion() {
        //基本的定位参数
        mLocationOption = LocationUtils.getDefaultOption();
        mLocationClient = LocationUtils.initLocation(this, mLocationOption, this);
        //启动定位
        mLocationClient.startLocation();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        Log.e("233",aMapLocation+"");
        if (aMapLocation.getErrorCode() == 0) {
            //停止定位后，本地定位服务并不会被销毁
            mLocationClient.stopLocation();
            //销毁定位客户端，同时销毁本地定位服务。
            mLocationClient.onDestroy();
            //更多返回看(文档：http://lbs.amap.com/api/android-location-sdk/guide/android-location/getlocation)
            latitude = aMapLocation.getLatitude();//获取纬度
            longitude = aMapLocation.getLongitude();//获取经度
            cityName = aMapLocation.getCity();//城市信息
            //定位成功在请求附近point  默认餐厅
            seartchPoiStart();
        } else {
            //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
            String str = aMapLocation.getErrorInfo();
            String[] split = str.split(" ");
            //截取第一个空格之前的错误日志
            Toast.makeText(this, "定位失败，" + split[0], Toast.LENGTH_LONG).show();
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
    }


    private void seartchPoiStart() {
        //创建搜索对象,空字符串是搜索全部
        PoiSearch.Query query = new PoiSearch.Query("", "", cityName);
        //设置每页最多返回多少条poiitem
        query.setPageSize(30);
        //设置查询页码
        query.setPageNum(1);
        //构造 PoiSearch 对象，并设置监听。
        PoiSearch search = new PoiSearch(this, query);
        //设置周边搜索的中心点以及区域 5000米-5公里
        search.setBound(new PoiSearch.SearchBound(new LatLonPoint(latitude, longitude), 3000));
        search.setOnPoiSearchListener(this);
        //开始搜索
        search.searchPOIAsyn();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在查询，请稍后...");
        progressDialog.show();
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            isfrist=true;
        }
        if (i == 1000) {
            if (poiResult.getPois().size() > 0) {
                //搜索结果的地名,集合都在这里
                Log.e("233",poiResult.getPois()+"");
            } else {
                Toast.makeText(this, "暂无结果", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "搜索失败", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //重新绘制加载地图
        if (mMapView != null) {
            mMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //暂停地图的绘制
        if (mMapView != null) {
            mMapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            //停止定位后，本地定位服务并不会被销毁
            mLocationClient.stopLocation();
            //销毁定位客户端，同时销毁本地定位服务。
            mLocationClient.onDestroy();
            isfrist=false;
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        LatLng finishTarget=cameraPosition.target;
        //Log.i("info","====滑动结束时地图中心点的经纬度==="+finishTarget.toString());
        //seartchPoiStart();

    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        LatLng finishTarget=cameraPosition.target;
        Log.i("info","====滑动结束时地图中心点的经纬度==="+finishTarget.toString());
        //seartchPoiStart();
        latitude=finishTarget.latitude;
        longitude=finishTarget.longitude;
        if (isfrist) {
            seartchPoiStart();
        }

    }


}
