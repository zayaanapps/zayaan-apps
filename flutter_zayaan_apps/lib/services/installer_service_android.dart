import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';
import '../models/store_app.dart';

class PlatformApkInstaller {
  static bool get isAndroidPlatform => Platform.isAndroid;

  /// Downloads the APK from GitHub Releases with byte progress and triggers
  /// the native Android Package Installer confirmation dialog.
  static Future<void> downloadAndInstallOrOpen({
    required StoreAppModel app,
    required void Function(double progress) onProgress,
    required void Function(String message) onStatus,
  }) async {
    onStatus('Downloading ${app.appName} v${app.versionName} APK...');
    final client = http.Client();
    try {
      final request = http.Request('GET', Uri.parse(app.apkDownloadUrl));
      final response = await client.send(request);
      if (response.statusCode != 200) {
        throw HttpException('HTTP ${response.statusCode} from GitHub Releases');
      }

      final totalBytes = response.contentLength ?? 0;
      int receivedBytes = 0;

      final dir = await getTemporaryDirectory();
      final apkFile = File('${dir.path}/${app.appId}-v${app.versionCode}.apk');
      final sink = apkFile.openWrite();

      await for (final chunk in response.stream) {
        sink.add(chunk);
        receivedBytes += chunk.length;
        if (totalBytes > 0) {
          onProgress(receivedBytes / totalBytes);
        }
      }
      await sink.flush();
      await sink.close();

      onStatus('Launching Android Package Installer...');
      await OpenFilex.open(
        apkFile.path,
        type: 'application/vnd.android.package-archive',
      );
    } finally {
      client.close();
    }
  }
}
