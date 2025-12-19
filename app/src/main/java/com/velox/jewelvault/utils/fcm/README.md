 # JewelVault Mobile – Notification Handling Quick Reference

## Overview
- FCM support is implemented via `utils/fcm/JewelVaultFirebaseMessagingService.kt` with channel routing, image support, and deep-link handoff through the login flow.
- Tokens are managed in `utils/fcm/FCMTokenManager.kt` and saved to DataStore; initial upload to Firestore happens during first-time admin signup in `LoginViewModel.uploadAdminUser`.
- Navigation after a notification tap is deferred until the user passes login/PIN. Pending targets are stored in `BaseViewModel` and consumed in `MainScreen`.

## Notification payload keys (data)
- `title`, `body`: text shown in the notification (falls back to notification payload if absent).
- `channelId` (optional): `jewel_vault_alerts`, `jewel_vault_marketing`, or defaults to `jewel_vault_general`.
- `imageUrl` (optional): Big Picture style image URL.
- `targetRoute` (optional): route string understood by `SubScreens` (e.g., `order_item_detail`).
- `targetArg` (optional): single argument appended to the route (e.g., an id).

### Sample FCM data message
```json
{
  "to": "<device_fcm_token>",
  "data": {
    "title": "Invoice Ready",
    "body": "Tap to review invoice 123",
    "channelId": "jewel_vault_alerts",
    "imageUrl": "https://example.com/banner.png",
    "targetRoute": "order_item_detail",
    "targetArg": "123"
  }
}
```

## Channels
- General (default): `jewel_vault_general`
- Alerts (high importance): `jewel_vault_alerts`
- Marketing (low importance): `jewel_vault_marketing`

## Tap-through behavior
- The notification intent carries `targetRoute`/`targetArg` into `MainActivity`, which stores them in `BaseViewModel`.
- After login/PIN, `MainScreen` reads the pending target and navigates the sub-nav accordingly.

## Token lifecycle
- Fetch and save: `FCMTokenManager.getAndSaveFCMToken()` and `saveFCMToken()`.
- Initial upload: during first-time admin signup (`LoginViewModel.uploadAdminUser`).
- Rotation: `JewelVaultFirebaseMessagingService.onNewToken` saves the new token locally (add a backend update if needed).

## Permissions
- Android 13+: ensure `POST_NOTIFICATIONS` permission is granted at runtime for notifications to display.

## Build
- Use `./gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug` (Unix).
