# CustomSurfaceViewDemo
>**1.Surface、SurfaceView、SurfaceHolder及SurfaceHolder.Callback之间的联系
><br>2.SurfaceView的使用
><br>3.SurfaceView双缓冲机制及使用注意
><br>4.SurfaceView绘制原理及与一般View的区别**

## 1. Surface、SurfaceView、SurfaceHolder及SurfaceHolder.Callback之间的联系 ##

**1.1  Surface**

>Handle onto a raw buffer that is being managed by the screen compositor

SDK中对Surface的描述是：<font color = "#007FFF" >**Surface是原始图像缓冲区（raw buffer）的一个句柄，而原始图像缓冲区是由屏幕图像合成器（screen compositor 即 Surfaceflinger）管理的。简单地说Surface对应了一块屏幕缓冲区，每个window对应一个Surface，任何View都是画在Surface上的，传统的view共享一块屏幕缓冲区，所有的绘制必须在UI线程中进行**。</font>

**(1)得到一个Surface对象时，同时会得到一个Canvas（画布）对象。这一点可以通过查看\frameworks\base\core\java\android\view\Surface.java文件可知道Surface类定义了一个Canvas成员变量**

<br>**(2)Canvas对象，可以把它当做画布，Canvas的方法大多数是设置画布的大小、形状、画布背景颜色等等，要想在画布上面画画，一般要与Paint对象结合使用，顾名思义，Paint就是画笔的风格，颜料的色彩之类的**

<br>**(3)Surface实现了Parcelable接口，（implements Parcelable），也就是说Surface对象可以把显示内容的数据写入到 Parcel 中，并且能够从Parcel读回数据。**

---

**1.2  SurfaceView**

>   SurfaceView提供了一个专门用于绘制的surface，这个surface内嵌于。你可以控制这个Surface的格式和尺寸。**<font color = "#003A6C" >Surfaceview控制这个Surface在屏幕的正确绘制位置</font>**。

>**<font color = "#007FFF" >surface是Z-ordered的（也就是说在xyz坐标系中，按照Z坐标排序的，Z值大的表面覆盖在Z值小的表面的上方），这表明它总在自己所在窗口的后面。surfaceview在显示窗口处为Surface提供了一个可见区域，通过这个区域，才能看到Surface里面的内容。可以放置一些覆盖图层（overlays）在Surface上面，如Button、Textview之类的。但是，需要注意的是，如果Surface上面有全透明的控件，那么随着Surface的每一次变化，这些全透明的控件就会重新渲染，这样的话，就影响性能与显示的效果。</font>**

>你可以通过SurfaceHolder这个接口去访问Surface，而执行getHolder()方法可以得到SurfaceHolder接口。

>当SurfaceView的窗口可见时，Surface就会被创建，当SurfaceView窗口隐藏时，Surface就会被销毁。当然了，你也可以通过复写surfaceCreated(SurfaceHolder) 和 surfaceDestroyed(SurfaceHolder)  这两个方法来验证一下Surface何时被创建与何时被销毁。

>SurfaceView提供了一个运行在渲染线程的surface，若你要更新屏幕，你需要了解以下线程知识。
<br>**<font color = "#007FFF" >(1) 所有SurfaceView 和 SurfaceHolder.Callback的方法都应该在主线程（UI线程）里面调用，应该要确保渲染进程所访问变量的同步性。**
<br>**(2) 必须确保只有当Surface有效的时候，（也就是当Surface的生命周期在SurfaceHolder.Callback.surfaceCreated() 和SurfaceHolder.Callback.surfaceDestroyed()之间）才能让渲染进程访问。</font>**

**SurfaceView与Surface的联系就是，Surface是管理显示内容的数据（implementsParcelable），包括存储于数据的交换。而SurfaceView就是把这些数据显示出来到屏幕上面**

>**源码角度看：**<font color = "#FF0038" >**每个SurfaceView创建的时候都会创建一个MyWindow，new MyWindow(this)中的this正是SurfaceView自身，因此将SurfaceView和window绑定在一起，而前面提到过每个window对应一个Surface，所以SurfaceView也就内嵌了一个自己的Surface，可以认为SurfaceView是来控制Surface的位置和尺寸。大家都知道，传统View及其派生类的更新只能在UI线程，然而UI线程还同时处理其他交互逻辑，这就无法保证view更新的速度和帧率了，而SurfaceView可以用独立的线程来进行绘制，因此可以提供更高的帧率，例如游戏，摄像头取景等场景就比较适合用SurfaceView来实现。</font>**

**SurfaceView与Surface的关系：**

![](http://o9m6aqy3r.bkt.clouddn.com/Surface%E4%B8%8ESurfaceView%E5%85%B3%E7%B3%BB.png)

**<font color = "#FF0038" >这种对应关系不止仅限于SurfaceView与Surface。包括一般的View与其公用的Surface对应关系。
同一个窗口的视图层次树中的View节点，其onDraw中的Canvas参数都是同一个，是由该窗口的ViewRootImpl通过surface.lockCanvas获得，并把该Canvas传给根视图， 对于应用窗口来说，其根视图的Canvas大小一般为整个屏幕；</font>**

---

**1.3  SurfaceHolder**

>  SurfaceHolder是控制surface的一个抽象接口，你可以通过SurfaceHolder来控制surface的尺寸和格式，或者修改surface的像素，监视surface的变化等等，SurfaceHolder是SurfaceView的典型接口。
<br>与直接控制SurfaceView来修改surface不同，使用SurfaceHolder来修改surface时，需要注意lockCanvas() 和Callback.surfaceCreated().这两个方法。

SurfaceHolder控制surface的流程所使用的几个方法如下：

(1) abstract void    addCallback(SurfaceHolder.Callback callback)
             <br>**Add a Callback interface for this holder.// 给SurfaceHolder一个回调对象。**

(2) abstract Canvas    lockCanvas(Rect dirty)
             <br>**Just like lockCanvas() but allows specification of a dirty rectangle.
             // 锁定画布中的某一个区域，返回的画布对象Canvas（当更新的内容只有一个区域时，同时要追求高效，可以只更
             新一部分的区域，而不必更新全部画布区域）**

(3) abstract Canvas    lockCanvas()
             <br>**Start editing the pixels in the surface.// 锁定画布，返回的画布对象Canvas**

(4) abstract void    removeCallback(SurfaceHolder.Callback callback)
             <br>**Removes a previously added Callback interface from this holder.//移除回调对象**

(5) abstract void    unlockCanvasAndPost(Canvas canvas)
             <br>**Finish editing pixels in the surface.// 结束锁定画图，并提交改变。**

---

**1.4  SurfaceHolder.Callback**
>**SurfaceHolder.Callback是监听surface改变的一个接口**

4.1、public abstract voidsurfaceChanged(SurfaceHolder holder, int format, int width, int height)
<br>**//surface发生改变时被调用**


4.2、public abstract voidsurfaceCreated(SurfaceHolder holder)
<br>**//在surface创建时被调用，一般在这个方法里面开启渲染屏幕的线程。**



4.3、public abstract voidsurfaceDestroyed(SurfaceHolder holder)
 <br>**//销毁时被调用，一般在这个方法里将渲染的线程停止。**

<font color = "#FF0038" size = "5"><strong>上述几个类的对应关系：</strong></font>

    SurfaceHolder = SurfaceView.getHolder();  
      
    Surface = SurfaceHolder.getSurface();  
      
    Canvas =SurfaceHolder.LockCanvas(Rect dirty)  
      
    Canvas   =Surface.lockCanvas(Rect dirty)  

## 2. SurfaceView的使用 ##
## 3.SurfaceView双缓冲机制及使用注意 ##
## 4.SurfaceView绘制原理及与一般View的区别 ##