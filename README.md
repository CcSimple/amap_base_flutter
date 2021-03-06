# 高德地图Flutter插件

## 安装
在你的`pubspec.yaml`文件的dependencies节点下添加:
```
amap_base:
  git: https://github.com/yohom/amap_base_flutter.git
```
如果你想要指定某个版本/分支/提交, 那么:
```
amap_base:
  git:
    url: https://github.com/yohom/amap_base_flutter.git
    ref: 0.0.1/branch/commit
```
导入:
```
import 'package:amap_base/amap_base.dart';
```
iOS端的`UiKitView`目前还只是preview状态, 默认是不支持的, 需要手动打开开关, 在info.plist文件中新增一行`io.flutter.embedded_views_preview`为`true`. 参考[iOS view embedding support has landed on master](https://github.com/flutter/flutter/issues/19030#issuecomment-437534853)

## 关于高德的Android SDK和iOS SDK
- 由于Android和iOS端的实现完全不一样, Android端照抄了Google Map的api设计, 而iOS
端又没有去抄Google Map的设计, 导致需要额外的工作去兼容两个平台的功能. 这个库的目标是尽可能的统一双端的api设置, 采用取各自平台api的**并集**, 然后在文档中指出针对哪个平台有效的策略来实现api统一.

## 关于包的大小
- ~~目前主分支的计划是实现全功能的高德地图, 然后开单独的分支实现高德的单独的功能, 这样包会小一点.~~
- 这个库依赖了高德导航库(包含了3dMap库), 以及搜索库.

## FAQ:
1. 定位到非洲去了
- 实际上是定位在了经纬度(0, 0)的位置了, 那个位置大致在非洲西部的几内亚湾, 原因是key
设置错了, 建议检查一下key的设置.

## TODO LIST:
* [ ] 创建地图
    * [x] 显示地图
    * [x] 显示定位蓝点
    * [x] 显示室内地图
    * [x] 切换地图图层
    * [ ] 使用离线地图
    * [x] 显示英文地图
    * [ ] 自定义地图
* [ ] 与地图交互
    * [x] 控件交互
    * [x] 手势交互
    * [ ] 调用方法交互
    * [ ] 地图截屏功能
* [ ] 在地图上绘制
    * [x] 绘制点标记
    * [ ] 绘制折线
    * [ ] 绘制面
    * [ ] 轨迹纠偏
    * [ ] 点平滑移动
    * [ ] 绘制海量点图层
* [ ] 获取地图数据
    * [x] 获取POI数据
    * [ ] 获取地址描述数据
    * [ ] 获取行政区划数据
    * [ ] 获取公交数据
    * [ ] 获取天气数据
    * [ ] 获取业务数据（云图功能）
    * [ ] 获取交通态势信息
* [ ] 出行线路规划
    * [x] 驾车出行路线规划
    * [ ] 步行出行路线规划
    * [ ] 公交出行路线规划
    * [ ] 骑行出行路线规划
    * [ ] 货车出行路线规划
* [ ] 地图计算工具
    * [ ] 坐标转换
    * [ ] 距离/面积计算
    * [ ] 距离测量