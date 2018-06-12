## Preview
![效果图](https://github.com/YangShaoXiong/MagicTransformView/blob/master/screenshot/image.gif)
## Usage
```java
<com.dreamer.magictransformview.widget.MagicTransformView
        android:id="@+id/magic_view"
        android:layout_width="match_parent"
        android:layout_height="49dp"
        android:padding="10dp"
        app:leftRightInterval="10dp"
        app:rightSrc="@array/download_imgs"
        app:textLeft="写评论"
        app:textRight="下载(520M)"/>
```
## Attributes
| name                        |  format   | description               | default_value |
| :--------------------------:| :------:  | :-----------:             | :-----------: |
| leftRightInterval            | dimension     | 左右View的间隔距离          | 20       |
| rightSrc         | reference   | 下载中时的动态图             |  |
| textLeft           | string     | 左边所要显示的文本         | |
| textRight        | string   | 右边所要显示的文本             |  |
