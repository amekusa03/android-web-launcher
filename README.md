# WebLauncher

WebLauncherは、Androidのホーム画面に特定のWebサイトを開くためのショートカット（スロット）を最大5つまで追加できるアプリです。

## 特徴

- **マルチスロット構成**: 最大5つの独立したランチャーアイコン（Web①〜Web⑤）を利用可能。
- **透過型ランチャー**: `LauncherActivity` は透過テーマを採用しており、ユーザー体験を損なわずにバックグラウンドでリダイレクト処理を行います。
- **動的なアイコン管理**: 各スロットは `activity-alias` として定義されており、プログラム側から有効/無効を切り替えることができます。

## プロジェクト構成

### 画面（Activity）

1. **管理画面 (`MainActivity`)**:
    - アプリの設定や各スロットの有効化・無効化、URLの割り当てを行うためのメインUIです。
    - ランチャー上のラベルは「WebLauncher Admin」として表示されます。

2. **実行ロジック (`LauncherActivity`)**:
    - 各スロットから起動される共通のActivityです。
    - `Theme.Translucent.NoTitleBar` を使用しており、画面自体は表示されず、目的のURLへ遷移するロジックを実行します。
    - `singleInstance` かつ `taskAffinity=""` 設定により、他のアプリのタスクスタックに影響を与えません。

### スロット (`activity-alias`)

以下の5つのエイリアスが定義されています。初期状態ではすべて `enabled="false"` です。

- **Slot 1 (Web①)**: `ic_launcher_slot1`
- **Slot 2 (Web②)**: `ic_launcher_slot2`
- **Slot 3 (Web③)**: `ic_launcher_slot3`
- **Slot 4 (Web④)**: `ic_launcher_slot4`
- **Slot 5 (Web⑤)**: `ic_launcher_slot5`

## 仕組み

このアプリは Android の `PackageManager` を利用して、マニフェスト内で定義された `activity-alias` の `component-enabled-state` を切り替えます。

1. 管理画面でスロットを有効にする。
2. ホーム画面に該当するスロットのアイコンが出現する。
3. アイコンをタップすると `LauncherActivity` が起動し、設定されたURLをブラウザ等で開く
