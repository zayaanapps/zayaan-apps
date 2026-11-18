import 'package:url_launcher/url_launcher.dart';
import '../models/store_app.dart';

class PlatformApkInstaller {
  static bool get isAndroidPlatform => false;

  /// On Flutter Web (https://zayaanapps.github.io/zayaan-apps/), triggers a direct
  /// browser APK download from GitHub Releases without attempting Android OS installation.
  static Future<void> downloadAndInstallOrOpen({
    required StoreAppModel app,
    required void Function(double progress) onProgress,
    required void Function(String message) onStatus,
  }) async {
    onProgress(1.0);
    onStatus('Starting browser APK download from GitHub Releases...');
    final uri = Uri.parse(app.apkDownloadUrl);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
  }
}
