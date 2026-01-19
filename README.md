# EtherSense

Wi-Fiネットワーク診断アプリ

![App Demo](docs/images/demo.gif)

## Features

### リアルタイム信号モニタリング
接続中のWi-Fiの信号強度(RSSI)、リンク速度、推定スループット、チャンネル干渉をリアルタイムで表示します。

### スピードテスト
ダウンロード/アップロード速度とレイテンシを測定。動画視聴、オンラインゲーム、ビデオ通話への適性も判定します。

### ネットワーク診断

#### 距離推定 (ITU-R P.1238)
ITU（国際電気通信連合）の屋内電波伝搬モデルを使用して、RSSIからルーターまでの推定距離を算出します。住宅、オフィス、商業施設など環境タイプに応じた経路損失係数を適用。

#### スループット予測 (Shannon-Hartley)
Shannon-Hartley定理による理論上の最大通信容量と、IEEE 802.11 MCSテーブルに基づく実効スループットを予測します。

#### リンクマージン分析
受信感度とRSSIの差分からリンクマージンを計算し、接続の安定性を評価します（Excellent >20dB / Good >12dB / Marginal >6dB / Unstable）。

#### 信号トレンド予測
過去のRSSI履歴から移動平均と線形回帰を用いて、近い将来の信号品質を予測します。

## Download

[GitHub Releases](https://github.com/zabio3/EtherSense/releases) からAPKをダウンロードできます。

## Screenshots

<!-- スクリーンショットを追加してください -->
| ダッシュボード | スピードテスト | 診断 |
|:---:|:---:|:---:|
| ![Dashboard](docs/images/dashboard.png) | ![Speed Test](docs/images/speedtest.png) | ![Diagnostics](docs/images/diagnostics.png) |

## Privacy

- すべての処理はデバイス上でローカルに実行
- データはサーバーに送信されません

## Build

```bash
./gradlew assembleDebug
```

Android 8.0 (API 26) 以上が必要です。
