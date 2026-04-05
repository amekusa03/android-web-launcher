# WebLauncher

##1. プロジェクト概要
本プロジェクトは、特定のWebページをAndroidの独立したアプリのように振る舞わせる「Webランチャー」アプリである。 ユーザーは最大5つのプリセット（ドロワー表示）と、無制限のホーム画面ショートカットを管理できる。

##2. コア・アーキテクチャ
Android OSの制約（実行時のActivity動的生成不可）を回避するため、以下の構成を採用する。

###2. 1. 静的スロット (App Drawer)
* `AndroidManifest.xml` に`activity-alias`を 5つ（Slot 1〜5） 定義する。
* 各エイリアスは共通の`LauncherActivity`をターゲットとするが、`android:name` によってどのスロットが起動されたかを識別する。
* 各スロットは個別に「有効/無効（enabled）」を切り替え可能とする。

###2. 2. 動的ショートカット (Home Screen)
* `ShortcutManagerCompat`を使用し、ユーザーが指定した任意のURLとアイコンでホーム画面ショートカットを作成する。

###2. 3.  共通ランチャーロジック(`LauncherActivity`)
* 起動時の`intent.component?.className`を判定。
* 対応するスロットの設定（URL、ブラウザ設定）を DataStore から取得。
* 設定が存在すればブラウザを起動、存在しなければ設定画面（管理UI）を起動する。

##3. 機能要件(Requirements)
###[FR-01] 管理画面 (Main UI / Jetpack Compose)
* スロット一覧: 5つの静的スロットの設定状態（名前、URL、アイコン色）を表示。
* 編集機能: 各スロットおよび新規ショートカットの「表示名」「URL」「アイコン色」「起動モード」を設定。
* バリデーション: URLは`http://` または`https://` で始まる必須項目とする。

###[FR-02] 起動体験 (Launch Experience)
* **Chrome Custom Tabs:** アプリ内ブラウザとして起動。ツールバーの色はアイコン色と連動させ、「アプリ感」を演出する。
* **外部ブラウザ:** ユーザー設定により、システム標準ブラウザでの起動も選択可能にする。

###[FR-03] データ永続化 (Data Persistence)
* Jetpack Preferences DataStore を使用。
* スロットID（1〜5）をキーとして、以下のオブジェクトをシリアライズ化して保存する。
 * `label`: String
 * `url`: String
 * `iconColor`: String (Hex Code)
 * `useCustomTabs`: Boolean

###[FR-04] アイコン・ビジュアル
* プリセットアイコンとして5色のカラーバリエーション（例：Material Blue, Red, Green, Yellow, Purple）を用意する。

##4. 画面遷移図 / ユーザーフロー
###1. 未設定スロットをタップ → 設定画面（対象スロットの編集モード）が開く。
###2. 設定済みスロットをタップ → 即座にWebページが開く。
###3. 管理アプリを直接起動 → 全スロットの一覧画面が開く

##5. テクニカルスタック (Technical Stack)
* Language: Kotlin 1.9+
* UI: Jetpack Compose (Material 3)
* Browser:`androidx.browser:browser:1.8.0`
* Storage:`androidx.datastore:datastore-preferences`
* Dependency Injection: Hilt (推奨) または 手動DI

##6. エッジケース (Edge Cases)
* URLが無効な場合: ブラウザ起動に失敗した際、ユーザーに通知し設定画面へ誘導する。
* ネットワーク未接続: Custom Tabs側のエラーハンドリングに任せるが、アプリ側でも事前に接続確認を行うのが望ましい。
* エイリアスの動的無効化:`PackageManager.setComponentEnabledSetting`を使用して、設定されていないスロットをドロワーから非表示にする。

##7. 受け入れ基準 (Acceptance Criteria)
* [ ] ドロワーに最大5つの独立したアイコンが表示されること。
* [ ] 各アイコンから別々のURLが、指定した色味のCustom Tabsで開くこと。
* [ ] 設定画面で名前を変更すると、即座に（あるいは再起動後に）反映されること。
* [ ] ホーム画面に「追加」したショートカットが正常に機能すること。

##ポイント
1. スロット概念の明確化: activity-alias が単なるエイリアスではなく、「Slot 1-5」という管理単位であることを明確にしました。
2. 識別ロジックの追加: LauncherActivity がどうやってスロットを判別するか（intent.component）を明記しました。
3. 管理画面の具体化: Jetpack Compose での実装を前提とし、DataStore の構造まで踏み込みました。
4. 動的非表示: 設定していないアイコンがドロワーを占有しないよう、setComponentEnabledSetting の利用を検討事項に入れました。