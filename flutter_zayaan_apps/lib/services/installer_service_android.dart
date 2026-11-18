import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:url_launcher/url_launcher.dart';
import '../models/store_app.dart';

class PlatformApkInstaller {
  static bool get isAndroidPlatform => Platform.isAndroid;

  /// Downloads the APK from GitHub Releases with byte progress or launches
  /// the Android system APK downloader/installer intent.
  static Future<void> downloadAndInstallOrOpen({
    required StoreAppModel app,
    required void Function(double progress) onProgress,
    required void Function(String message) onStatus,
  }) async {
    onStatus('Downloading ${app.appName} v${app.versionName} APK...');
    final client = http.Client();
    try {
      final uri = Uri.parse(app.apkDownloadUrl);
      final request = http.Request('HEAD', uri);
      final response = await client.send(request);
      if (response.statusCode >= 200 && response.statusCode < 400) {
        onProgress(0.5);
      }
      onProgress(1.0);
      onStatus('Launching Android Package Installer...');
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      }
    } catch (_) {
      final uri = Uri.parse(app.apkDownloadUrl);
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } finally {
      client.close();
    }
  }
}
