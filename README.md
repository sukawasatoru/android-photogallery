Photo Gallery
=============

Overview
--------

大量の写真を表示するサンプルです

GraphQL でデータを取得して Room はそれを記憶する Cache として使用します。

### MyActivity.useMyAdapter ###

Remote から写真を全件取得し View に反映させます。

実装は Room で既存のデータを View に表示したのちに Remote から最新のデータを取得します。
Pixel 4a (dalvik.vm.heapgrowthlimit = 256m) だと 340000 件あたり読み込んだあたりで OOM します。

### MyActivity.usePagingV3 ###

Remote から写真を一定数取得し View に反映させます。

実装は [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) を使用し一定数 Remote から取得します。

5000件を Memory 上に持ち、 5000件以上取得した場合表示している箇所から離れている方向のデータを Drop します。

### MyActivity.usePagingRemoteMediator ###

Remote から写真を一定数取得し View に反映させます。

実装は [MyActivity.usePagingV3](#myactivityusepagingv3) の実装に加え RemoteMediator を使用しています。  
よって Room で既存のデータを View に表示したのちに Remote から最新のデータを取得します。

5000件を Memory 上に持ち、 5000件以上取得した場合表示している箇所から離れている方向のデータを Drop します。一度 Remote から取得したデータは Room に記憶されているためそちらを使用します。

Usage
-----

```bash
# launch image server
cd <path to repo>/tools/server

# your font: "JetBrains Mono", "Conslas", "Ubuntu Mono", etc.
cargo run --release -- -a0.0.0.0 -bhttp://<your pc address>:38082 -f<your font> -p2147483647

# build and install apk
cd <path to repo>
./gradlew -PjpTinyportPhotogalleryApiEndpoint=http://<your pc address>:38038/graphql :app:installDebug
```
