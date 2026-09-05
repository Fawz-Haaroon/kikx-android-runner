package com.luvbyte.kikx;
import android.app.*;
import android.os.*;
import android.view.*;
import android.content.*;
import android.net.*;
import android.webkit.*;
import android.widget.*;
public class MainActivity extends Activity {
private MainBinding binding;
private ValueCallback<Uri[]> fileChooserCallback;
private static final int FILE_CHOOSER_REQUEST = 100;
private static final int WEB_PERMISSION_REQUEST = 1000;
private PermissionRequest pendingWebPermissionRequest;
private String[] pendingWebPermissionResources;
private String[] pendingAndroidPermissions;
private GeolocationPermissions.Callback pendingGeolocationCallback;
private String pendingGeolocationOrigin;
private WebChromeClient webChromeClient;
private View customView;
private WebChromeClient.CustomViewCallback customViewCallback;
private int customViewSystemUiVisibility;
private String pendingDownloadUrl;
private String pendingDownloadUserAgent;
private String pendingDownloadContentDisposition;
private String pendingDownloadMimeType;
@Override protected void onCreate(Bundle _savedInstanceState) {
 super.onCreate(_savedInstanceState);
 initialize(_savedInstanceState);
}
private void initialize(Bundle _savedInstanceState) {

 initializeLogic();
}
private void initializeLogic() {
WebSettings ws = binding.webview1.getSettings();

ws.setJavaScriptEnabled(true);
ws.setDomStorageEnabled(true);
ws.setDatabaseEnabled(true);
ws.setAllowContentAccess(true);

if (Build.VERSION.SDK_INT >= 21) {
	ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
}

ws.setJavaScriptCanOpenWindowsAutomatically(true);
ws.setSupportMultipleWindows(true);
ws.setLoadWithOverviewMode(true);
ws.setUseWideViewPort(true);
ws.setDisplayZoomControls(false);
ws.setMediaPlaybackRequiresUserGesture(false);
ws.setCacheMode(WebSettings.LOAD_DEFAULT);
ws.setGeolocationEnabled(true);

CookieManager.getInstance().setAcceptCookie(true);

if (Build.VERSION.SDK_INT >= 21) {
	CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview1, true);
}

binding.webview1.setWebViewClient(new WebViewClient());

binding.webview1.setDownloadListener(new DownloadListener() {
	@Override
	public void onDownloadStart(
	String url,
	String userAgent,
	String contentDisposition,
	String mimeType,
	long contentLength) {
		if (Build.VERSION.SDK_INT < 29 &&
		checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
		!= android.content.pm.PackageManager.PERMISSION_GRANTED) {
			pendingDownloadUrl = url;
			pendingDownloadUserAgent = userAgent;
			pendingDownloadContentDisposition = contentDisposition;
			pendingDownloadMimeType = mimeType;
			requestPermissions(
			new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
			WEB_PERMISSION_REQUEST);
			return;
		}

		String downloadFileName =
		URLUtil.guessFileName(url, contentDisposition, mimeType);
		DownloadManager.Request downloadRequest =
		new DownloadManager.Request(Uri.parse(url));

		if (mimeType != null) {
			downloadRequest.setMimeType(mimeType);
		}
		if (userAgent != null) {
			downloadRequest.addRequestHeader("User-Agent", userAgent);
		}

		String cookies = CookieManager.getInstance().getCookie(url);
		if (cookies != null) {
			downloadRequest.addRequestHeader("Cookie", cookies);
		}

		downloadRequest.setTitle(downloadFileName);
		downloadRequest.setNotificationVisibility(
		DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		downloadRequest.setDestinationInExternalPublicDir(
		Environment.DIRECTORY_DOWNLOADS, downloadFileName);

		DownloadManager downloadManager =
		(DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		if (downloadManager != null) {
			downloadManager.enqueue(downloadRequest);
		}
	}
});

binding.webview1.setWebChromeClient(webChromeClient = new WebChromeClient() {
	@Override
	public boolean onShowFileChooser(
	WebView view,
	ValueCallback<Uri[]> filePathCallback,
	FileChooserParams fileChooserParams) {
		if (fileChooserCallback != null) {
			fileChooserCallback.onReceiveValue(null);
		}

		fileChooserCallback = filePathCallback;

		Intent pickerIntent;
		try {
			pickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			pickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
			pickerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

			String[] acceptTypes = fileChooserParams.getAcceptTypes();
			ArrayList<String> requestedMimeTypes = new ArrayList<>();
			boolean acceptsAnyType = false;
			if (acceptTypes != null) {
				for (String acceptType : acceptTypes) {
					if (acceptType == null) {
						continue;
					}
					String[] splitTypes = acceptType.split(",");
					for (String splitType : splitTypes) {
						String mimeType = splitType.trim();
						if (mimeType.length() == 0) {
							continue;
						}
						if ("*/*".equals(mimeType)) {
							acceptsAnyType = true;
							requestedMimeTypes.clear();
							break;
						}
						if (!requestedMimeTypes.contains(mimeType)) {
							requestedMimeTypes.add(mimeType);
						}
					}
					if (acceptsAnyType) {
						break;
					}
				}
			}

			if (acceptsAnyType || requestedMimeTypes.isEmpty()) {
				pickerIntent.setType("*/*");
			} else if (requestedMimeTypes.size() == 1) {
				pickerIntent.setType(requestedMimeTypes.get(0));
			} else {
				pickerIntent.setType("*/*");
				pickerIntent.putExtra(
					Intent.EXTRA_MIME_TYPES,
					requestedMimeTypes.toArray(
						new String[requestedMimeTypes.size()]));
			}

			if (fileChooserParams.getMode() ==
				FileChooserParams.MODE_OPEN_MULTIPLE) {
				pickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			}
		} catch (Exception exception) {
			fileChooserCallback.onReceiveValue(null);
			fileChooserCallback = null;
			return true;
		}

		try {
			startActivityForResult(pickerIntent, FILE_CHOOSER_REQUEST);
		} catch (Exception exception) {
			fileChooserCallback.onReceiveValue(null);
			fileChooserCallback = null;
		}
		return true;
	}

	@Override
	public void onPermissionRequest(final PermissionRequest request) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Uri permissionOrigin = request.getOrigin();
boolean isKikxOrigin =
permissionOrigin != null &&
"http".equalsIgnoreCase(permissionOrigin.getScheme()) &&
"127.0.0.1".equals(permissionOrigin.getHost()) &&
permissionOrigin.getPort() == 1303;

				// KIKX is served from loopback; do not turn this into a site-wide permission broker.
				if (!isKikxOrigin) {
					request.deny();
					return;
				}

				if (pendingWebPermissionRequest != null) {
					pendingWebPermissionRequest.deny();
				}
				pendingWebPermissionRequest = null;
				pendingWebPermissionResources = null;
				pendingAndroidPermissions = null;

				ArrayList<String> allowedResources = new ArrayList<>();
				ArrayList<String> androidPermissions = new ArrayList<>();

				for (String resource : request.getResources()) {
					if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
						allowedResources.add(resource);
						if (!androidPermissions.contains(
						android.Manifest.permission.CAMERA)) {
							androidPermissions.add(android.Manifest.permission.CAMERA);
						}
					} else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
						allowedResources.add(resource);
						if (!androidPermissions.contains(
						android.Manifest.permission.RECORD_AUDIO)) {
							androidPermissions.add(android.Manifest.permission.RECORD_AUDIO);
						}
					} else {
						request.deny();
						return;
					}
				}

				if (allowedResources.isEmpty()) {
					request.deny();
					return;
				}

				pendingWebPermissionRequest = request;
				pendingWebPermissionResources =
				allowedResources.toArray(new String[allowedResources.size()]);
				pendingAndroidPermissions =
				androidPermissions.toArray(new String[androidPermissions.size()]);

				if (Build.VERSION.SDK_INT >= 23) {
					ArrayList<String> missingPermissions = new ArrayList<>();
					for (String permission : pendingAndroidPermissions) {
						if (checkSelfPermission(permission)
						!= android.content.pm.PackageManager.PERMISSION_GRANTED) {
							missingPermissions.add(permission);
						}
					}

					if (!missingPermissions.isEmpty()) {
						requestPermissions(
						missingPermissions.toArray(
						new String[missingPermissions.size()]),
						WEB_PERMISSION_REQUEST);
						return;
					}
				}

				String[] resourcesToGrant = pendingWebPermissionResources;
				pendingWebPermissionRequest = null;
				pendingWebPermissionResources = null;
				pendingAndroidPermissions = null;
				request.grant(resourcesToGrant);
			}
		});
	}

	@Override
	public void onGeolocationPermissionsShowPrompt(
	String origin,
	GeolocationPermissions.Callback callback) {
		Uri permissionOrigin = Uri.parse(origin);
boolean isKikxOrigin =
"http".equalsIgnoreCase(permissionOrigin.getScheme()) &&
"127.0.0.1".equals(permissionOrigin.getHost()) &&
permissionOrigin.getPort() == 1303;

		if (!isKikxOrigin) {
			callback.invoke(origin, false, false);
			return;
		}

		if (pendingGeolocationCallback != null) {
			pendingGeolocationCallback.invoke(
			pendingGeolocationOrigin, false, false);
		}
		if (pendingWebPermissionRequest != null) {
			pendingWebPermissionRequest.deny();
		}

		pendingWebPermissionRequest = null;
		pendingWebPermissionResources = null;
		pendingAndroidPermissions = null;
		pendingGeolocationCallback = callback;
		pendingGeolocationOrigin = origin;

		if (Build.VERSION.SDK_INT >= 23 &&
		checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
		!= android.content.pm.PackageManager.PERMISSION_GRANTED &&
		checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
		!= android.content.pm.PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[] {
			android.Manifest.permission.ACCESS_FINE_LOCATION,
			android.Manifest.permission.ACCESS_COARSE_LOCATION
			}, WEB_PERMISSION_REQUEST);
			return;
		}

		pendingGeolocationCallback = null;
		pendingGeolocationOrigin = null;
		callback.invoke(origin, true, false);
	}

	@Override
	public void onShowCustomView(
	View view,
	CustomViewCallback callback) {
		if (customView != null) {
			callback.onCustomViewHidden();
			return;
		}

		customView = view;
		customViewCallback = callback;
		customViewSystemUiVisibility =
		getWindow().getDecorView().getSystemUiVisibility();
		getWindow().getDecorView().setSystemUiVisibility(
		View.SYSTEM_UI_FLAG_FULLSCREEN |
		View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
		View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
		View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
		View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
		View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		setContentView(view);
	}

	@Override
	public void onHideCustomView() {
		if (customView == null) {
			return;
		}

		setContentView(binding.getRoot());
		getWindow().getDecorView().setSystemUiVisibility(
		customViewSystemUiVisibility);
		if (customViewCallback != null) {
			customViewCallback.onCustomViewHidden();
		}
		customView = null;
		customViewCallback = null;
	}
});

binding.webview1.loadUrl("http://127.0.0.1:1303");
}
@Override protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
 super.onActivityResult(_requestCode, _resultCode, _data);
if (_requestCode == FILE_CHOOSER_REQUEST &&
fileChooserCallback != null) {
	Uri[] selectedFiles = null;
	if (_resultCode == Activity.RESULT_OK) {
		try {
			selectedFiles =
			WebChromeClient.FileChooserParams.parseResult(_resultCode, _data);
		} catch (Exception exception) {
			selectedFiles = null;
		}
	}

	ValueCallback<Uri[]> callback = fileChooserCallback;
	fileChooserCallback = null;
	callback.onReceiveValue(selectedFiles);
}
}
@Override public void onDestroy() {
 super.onDestroy();
if (fileChooserCallback != null) {
	fileChooserCallback.onReceiveValue(null);
	fileChooserCallback = null;
}

if (pendingWebPermissionRequest != null) {
	pendingWebPermissionRequest.deny();
	pendingWebPermissionRequest = null;
}

if (pendingGeolocationCallback != null) {
	pendingGeolocationCallback.invoke(
	pendingGeolocationOrigin, false, false);
	pendingGeolocationCallback = null;
	pendingGeolocationOrigin = null;
}

pendingWebPermissionResources = null;
pendingAndroidPermissions = null;
pendingDownloadUrl = null;
pendingDownloadUserAgent = null;
pendingDownloadContentDisposition = null;
pendingDownloadMimeType = null;

binding.webview1.evaluateJavascript(
"window.dispatchEvent(new CustomEvent('client:logout'))",
null);
}
@Override public void onStart() {
 super.onStart();

}
@Override public void onResume() {
 super.onResume();
if (pendingWebPermissionRequest != null) {
	PermissionRequest permissionRequest = pendingWebPermissionRequest;
	String[] resourcesToGrant = pendingWebPermissionResources;
	String[] androidPermissions = pendingAndroidPermissions;
	boolean permissionsGranted = true;

	if (Build.VERSION.SDK_INT >= 23) {
		for (String permission : androidPermissions) {
			if (checkSelfPermission(permission)
			!= android.content.pm.PackageManager.PERMISSION_GRANTED) {
				permissionsGranted = false;
				break;
			}
		}
	}

	pendingWebPermissionRequest = null;
	pendingWebPermissionResources = null;
	pendingAndroidPermissions = null;

	if (permissionsGranted) {
		permissionRequest.grant(resourcesToGrant);
	} else {
		permissionRequest.deny();
	}
}

if (pendingGeolocationCallback != null) {
	GeolocationPermissions.Callback callback = pendingGeolocationCallback;
	String origin = pendingGeolocationOrigin;
	boolean locationGranted = true;

	if (Build.VERSION.SDK_INT >= 23) {
		locationGranted =
		checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
		== android.content.pm.PackageManager.PERMISSION_GRANTED ||
		checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
		== android.content.pm.PackageManager.PERMISSION_GRANTED;
	}

	pendingGeolocationCallback = null;
	pendingGeolocationOrigin = null;
	callback.invoke(origin, locationGranted, false);
}

if (pendingDownloadUrl != null) {
	String url = pendingDownloadUrl;
	String userAgent = pendingDownloadUserAgent;
	String contentDisposition = pendingDownloadContentDisposition;
	String mimeType = pendingDownloadMimeType;

	pendingDownloadUrl = null;
	pendingDownloadUserAgent = null;
	pendingDownloadContentDisposition = null;
	pendingDownloadMimeType = null;

	if (Build.VERSION.SDK_INT < 29 &&
	checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
	!= android.content.pm.PackageManager.PERMISSION_GRANTED) {
		return;
	}

	String downloadFileName =
	URLUtil.guessFileName(url, contentDisposition, mimeType);
	DownloadManager.Request downloadRequest =
	new DownloadManager.Request(Uri.parse(url));

	if (mimeType != null) {
		downloadRequest.setMimeType(mimeType);
	}
	if (userAgent != null) {
		downloadRequest.addRequestHeader("User-Agent", userAgent);
	}

	String cookies = CookieManager.getInstance().getCookie(url);
	if (cookies != null) {
		downloadRequest.addRequestHeader("Cookie", cookies);
	}

	downloadRequest.setTitle(downloadFileName);
	downloadRequest.setNotificationVisibility(
	DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
	downloadRequest.setDestinationInExternalPublicDir(
	Environment.DIRECTORY_DOWNLOADS, downloadFileName);

	DownloadManager downloadManager =
	(DownloadManager) getSystemService(DOWNLOAD_SERVICE);
	if (downloadManager != null) {
		downloadManager.enqueue(downloadRequest);
	}
}
}
@Override public void onBackPressed() {
if (customView != null) {
	webChromeClient.onHideCustomView();
	return;
}

binding.webview1.evaluateJavascript(
"window.dispatchEvent(new CustomEvent('client:back'))",
null);
}
}
