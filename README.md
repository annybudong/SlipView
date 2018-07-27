# SlipView-安卓左滑控件

github上有不少左滑控件，有些存在bug，有些写得太重学习成本很大，一旦项目有什么定制需求，难以维护。基于此考虑，自己重写了一个非常简单但又稳定的SlipView左滑控件，代码里有注释，方便阅读理解，希望能帮到大家。

## 配置xml

使用方式特别简单，配置xml就好：

```
<com.annybudong.slipview.SlipView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="70dp"
    android:orientation="horizontal">
    <TextView
        android:id="@+id/content"
        android:clickable="false"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:textSize="16sp"/>
    <TextView
        android:id="@+id/menu_delete"
        android:layout_width="100dp"
        android:layout_height="match_parent"
        android:background="#ff7766"
        android:text="删除"
        android:textSize="16sp"
        android:textColor="#000000"
        android:gravity="center"/>
    <TextView
        android:id="@+id/menu_edit"
        android:layout_width="100dp"
        android:layout_height="match_parent"
        android:text="编辑"
        android:background="#00ff88"
        android:textSize="16sp"
        android:textColor="#000000"
        android:gravity="center"/>
</com.annybudong.slipview.SlipView>
```

第一个TextView是显示区域，后面两个TextView就是左滑按钮，如果想要更多左滑按钮，依次添加就好。

**PS:** 此例中显示区域是一个TextView，实际开发中很有可能是LinearLayout、RelativeLayout等ViewGroup，别担心，直接替换即可。 