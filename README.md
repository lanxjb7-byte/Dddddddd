# LIXXZY AI Android source

## Logo PNG
Copy your existing logo to `app/src/main/res/drawable/lixxzy_logo.png` (lowercase filename). The Java code looks it up by resource name and falls back to the app icon if absent.

## Build
Push the project to the `main` branch on GitHub, then run Actions > LIXXZY AI APK BUILDER. The debug APK is uploaded as the `LIXXZY-AI-APK` artifact.

## Website settings integration
The website must call the Android bridge when the floating setting changes:

```js
// When user turns floating window ON:
window.LixxzyAndroid?.setFloatingEnabled(true);
// When user turns it OFF:
window.LixxzyAndroid?.setFloatingEnabled(false);
// When transparency slider changes (1-100):
window.LixxzyAndroid?.setTransparency(Number(value));
```

The bridge exists inside the APK WebView only. In a normal browser, `window.LixxzyAndroid` is absent, so use optional chaining as shown.
