# DDownload 

简单实现了一套下载系统，使用说明如下：

## 初始化

Application 中初始化

```
DDownload.getInstance().init(this);
```

### 设置过滤器

因为有相应的业务需求，所以也简单添加了过滤器功能。分为前过滤器和后过滤器。

前过滤器载我用于检测网络，如果非Wifi则拦截本次下载。

后过滤器用于下载后如果是压缩文件则解压，因为过滤器可以修改对应的 task，故可以用解压后的文件地址替换掉原压缩文件地址，使调用者无需关心解压操作。

```
XYDownload.getInstance().addAfterDownloadFilter(new AfterDownloadGZipFilter());
XYDownload.getInstance().addBeforeDownloadFilter(new BeforeDownloadCheckNetStateFilter());
```

## 下载

```
List<DownloadTask> list = new ArrayList<>();
DownloadTask downloadTask = new DownloadTask(AppConstant.path, AppConstant.url);
DownloadTask downloadTask = new DownloadTask(AppConstant.path2, AppConstant.url2);

DDownload.getInstance().start(list, "tag", new DownloadListener() {
    @Override
    public void onError(String msg, int code) {
        
    }

    @Override
    public void onComplete() {
        
    }
});
```

### 下载进度回调

- 支持多个文件同一个总进度回调

```
DDownload.getInstance().setProgressListener(new ProgressListener() {
    @Override
    public void taskProgress(String downloadUrl, long progress, long total) {

    }

    @Override
    public void taskContainerProgress(String containerTag, long progress, long total) {
        
    }
});
```

### 停止所有任务

```
DDownload.getInstance().stopAll();
```
