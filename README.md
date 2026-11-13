# PasskeyRedirectDemoAndroid

Android の App Links を使って、外部ブラウザ経由のログインとアプリ復帰を確認するサンプルです。

## 目的

- ログインページを外部ブラウザで開く
- 認証後の戻り先 URL でアプリを起動する
- `state` と `shortTimeCode` を受け取って画面に表示する
- パスキーやブラウザ遷移の動作確認をする

## 仕様

- 戻り先は `https://ryunosuke-shinkubo.github.io/auth/callback.html`
- Android 側は `MainActivity` の `intent-filter` でこの URL を受ける
- ドメイン所有確認は `https://ryunosuke-shinkubo.github.io/.well-known/assetlinks.json` で行う
- Web 側は共通の `https://` URL を返す前提

## 画面で見られるもの

- 送信先 URL
- pending `state`
- コールバックのプレビュー
- 受信した `shortTimeCode`
- 受信した `state`
- 受信した `error`
- `state` の一致判定

## 使い方

1. Android SDK を入れる
2. `app/build.gradle.kts` を同期する
3. `assetlinks.json` を公開ドメインに置く
4. 実機またはエミュレータでアプリを起動する
5. ログイン開始ボタンを押す
