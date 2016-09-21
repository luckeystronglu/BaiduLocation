package com.qf.baidulocation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.List;

/**
 * Created by Ken on 2016/9/20.15:11
 * 定位
 *      基站定位：
 *            wifi定位:定位快，但是精准度不是很高，偏差的范围0米到800米
 *            流量定位：定位快，但是精准度非常低，偏差范围800米到1500米
 *      GPS定位：
 *            定位准确，民用GPS的范围0~100米，军用0~10米，
 *            但是定位很慢，第一次需要搜星
 *            信号很弱，在室内或者高楼林立的地方可能收不到GPS信号
 *
 * 坐标：
 *      标准的GPS坐标
 *      火星坐标：腾讯地图、搜狗地图、高德地图
 *      百度坐标：
 *
 */

public class MainActivity extends AppCompatActivity {
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {

            //接收定位返回结果
            //Receive Location
            StringBuffer sb = new StringBuffer(256);
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());// 单位：公里每小时
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\nheight : ");
                sb.append(location.getAltitude());// 单位：米
                sb.append("\ndirection : ");
                sb.append(location.getDirection());// 单位度
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                //运营商信息
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");


                //获得经纬度
                lat = location.getLatitude();
                lng = location.getLongitude();
//                Log.d("print", "定位坐标Latitude： "+ lat +" Longitude:"+ lng);
                Toast.makeText(MainActivity.this, "您当前位置为北纬：" + lat + "，东经:" + lng, Toast.LENGTH_SHORT).show();

                //构建Marker图标
                BitmapDescriptor bitmap = BitmapDescriptorFactory
                        .fromResource(R.mipmap.txl_g);
                //构建MarkerOption，用于在地图上添加Marker
                MarkerOptions option = new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .icon(bitmap)
                        .draggable(true);//可拖拽

                // 生长动画
                option.animateType(MarkerOptions.MarkerAnimateType.grow);

                //在地图上添加Marker，并显示
                map.addOverlay(option);

                //设置地图显示的位置
                MapStatus mMapStatus = new MapStatus.Builder()
                        .target(new LatLng(lat, lng))//地图显示位置的中心点
                        .build();

                MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                map.setMapStatus(mMapStatusUpdate);

            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }
            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());// 位置语义化信息
            List<Poi> list = location.getPoiList();// POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }

            }
            Log.d("print", sb.toString());
//            Toast.makeText(DingWeiActivity.this, "----->" + sb, Toast.LENGTH_SHORT).show();
        }
    };

    private MapView mMapView;
    private BaiduMap map;

    //搜索相关
    private EditText et;
    private PoiSearch mPoiSearch;
    private TextView tv01, tv02, tv03;
    private Button callbtn, closebtn;

    private double lng, lat;
    private View showview;

    //电话号码
    String telephone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = (MapView) findViewById(R.id.bmapView);
        map = mMapView.getMap();

        et = (EditText) findViewById(R.id.et_search);
        //创建POI检索对象
        mPoiSearch = PoiSearch.newInstance();


        mPoiSearch.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(final PoiResult geoCodeResult) {

                if (geoCodeResult == null || geoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                    //没有检索到结果
                    Toast.makeText(MainActivity.this, "没有这个东西", Toast.LENGTH_SHORT).show();
                    return;
                }


                if (geoCodeResult.error == SearchResult.ERRORNO.NO_ERROR) {
                    map.clear();//清空原来的标注
                    final PoiOverlay poiOverlay = new PoiOverlay(map) {

                        @Override
                        public boolean onPoiClick(int i) {
                            final PoiInfo poiInfo = geoCodeResult.getAllPoi().get(i);
//                            String name = poiInfo.name;
//                            Log.d("out", "名字：" + name );

                            showview = LayoutInflater.from(MainActivity.this).inflate(R.layout.view_message_layout, null);
                            showview.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    map.hideInfoWindow();
                                }
                            });
                            tv01 = (TextView) showview.findViewById(R.id.tvmsg_01);
                            tv02 = (TextView) showview.findViewById(R.id.tvmsg_02);
                            tv03 = (TextView) showview.findViewById(R.id.tvmsg_03);
                            callbtn = (Button) showview.findViewById(R.id.call_btn);
                            closebtn = (Button) showview.findViewById(R.id.close_btn);

//                            boolean b = mPoiSearch.searchPoiDetail(new PoiDetailSearchOption().poiUid(poiInfo.uid));

                            tv01.setText(poiInfo.name); //获得商户名

                            if (poiInfo.phoneNum.length() < 15){
                                telephone = poiInfo.phoneNum;
                            }else {
                                String[] onephone = poiInfo.phoneNum.split(",");
                                telephone = onephone[0];
                            }
//                            Log.d("print", "onPoiClick: "+telephone.length());

                            tv02.setText(telephone); //获得电话号码
                            tv03.setText(poiInfo.address); //获得地址
                            closebtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    map.hideInfoWindow();

                                }
                            });
                            callbtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
//                                    Intent intent = new Intent(Intent.ACTION_CALL);
//                                    intent.setData(Uri.parse("tel://"+ poiInfo.phoneNum));
//                                    startActivity(intent);
                                    if (telephone == null ||telephone == ""){
                                        Toast.makeText(MainActivity.this, "没有号码，无法拨号", Toast.LENGTH_SHORT).show();

                                    }else {
                                        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+telephone));
                                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                            // TODO: Consider calling
                                            //    ActivityCompat#requestPermissions
                                            // here to request the missing permissions, and then overriding
                                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            //                                          int[] grantResults)
                                            // to handle the case where the user grants the permission. See the documentation
                                            // for ActivityCompat#requestPermissions for more details.
                                            return;
                                        }

                                        MainActivity.this.startActivity(intent);
                                    }


                                }
                            });

                            //创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
                            InfoWindow mInfoWindow = new InfoWindow(showview, poiInfo.location, -50);
                            //显示InfoWindow
                            map.showInfoWindow(mInfoWindow);

                            return false;
                        }
                    };
                    //设置overlay可以处理标注点击事件
                    map.setOnMarkerClickListener(poiOverlay);
                    //设置PoiOverlay数据
                    poiOverlay.setData(geoCodeResult);
                    //添加PoiOverlay到地图中
                    poiOverlay.addToMap();
                    poiOverlay.zoomToSpan();
                }





            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult result) {
//                Log.d("print", "----->详情"+ result.getName()+"----------"+result.getTelephone()+"------"+result.getShopHours());
//                tv01.setText(result.getAddress());

            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

            }
        });

        //设置地图的点击事件
//        map.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
//            @Override
//            public void onMapClick(LatLng latLng) {
//                //定义Maker坐标点
//                //      LatLng point = new LatLng(39.963175, 116.400244);
//                //构建Marker图标
//                Log.d("print", "onMapClick: --->"+latLng.latitude + " " + latLng.longitude);
//                BitmapDescriptor bitmap = BitmapDescriptorFactory
//                        .fromResource(R.mipmap.a2i);
//                //构建MarkerOption，用于在地图上添加Marker
//                MarkerOptions option = new MarkerOptions()
//                        .position(latLng)
//                        .icon(bitmap)
//                        .draggable(true);//设置可拖拽
//                option.animateType(MarkerOptions.MarkerAnimateType.grow);
////                option.animateType(MarkerOptions.MarkerAnimateType.drop);
//                //在地图上添加Marker，并显示
//                map.addOverlay(option);
//            }
//
//            @Override
//            public boolean onMapPoiClick(MapPoi mapPoi) {
//                return false;
//            }
//        });

        //标记物点击事件
//        map.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
//            @Override
//            public boolean onMarkerClick(final Marker marker) {
//                //创建InfoWindow展示的view
//
//                View showview = LayoutInflater.from(MainActivity.this).inflate(R.layout.view_message_layout,null);
//
//                showview.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        marker.remove();
//                        map.hideInfoWindow();
//                    }
//                });
//
////                Button button = new Button(getApplicationContext());
////                button.setOnClickListener(new View.OnClickListener() {
////                    @Override
////                    public void onClick(View v) {
////                        marker.remove();
////                        map.hideInfoWindow();
////                    }
////                });
////                button.setBackgroundResource(R.mipmap.icon_gift);
////                button.setText("点击关闭");
////                button.setTextColor(Color.BLUE);
//
//
//                //定义用于显示该InfoWindow的坐标点
//                //    LatLng pt = new LatLng(39.86923, 116.397428);
//                //创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
////                InfoWindow mInfoWindow = new InfoWindow(showview, marker.getPosition(), -50);
//
//                //显示InfoWindow
////                map.showInfoWindow(mInfoWindow);
//                return false;
//            }
//        });


        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);    //注册监听函数

        initLocation();
    }

    public void btnclick(View v){
        switch (v.getId()) {
            case R.id.btn:
                //开始定位
                mLocationClient.start();
                break;
            case R.id.btn_search:
                String keyWord = et.getText().toString();
                //检索POI
//                mPoiSearch.searchInCity((new PoiCitySearchOption())
//                        .city("深圳")
//                        .keyword(keyWord)
//                        .pageNum(10));
        //    LatLng ll1 = new LatLng(29.714459,113.898036);
                mPoiSearch.searchNearby(new PoiNearbySearchOption()
                        .location(new LatLng(lat,lng))
                        .radius(2000)
                        .keyword(keyWord));
                break;
        }

    }

    /**
     * 初始化定位SDK配置
     */
    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span= 0; //仅定位一次
//        int span= 5000; //每隔5秒定位一次
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPoiSearch.destroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }
}
