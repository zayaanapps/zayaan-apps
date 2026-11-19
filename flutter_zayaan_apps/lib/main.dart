import 'dart:async';
import 'dart:convert';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:url_launcher/url_launcher.dart';
import 'models/store_app.dart';
import 'services/firestore_service.dart';
import 'services/installer_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await Firebase.initializeApp();
  } catch (_) {
    // Allows running local preview tests before Firebase web options are injected.
  }
  runApp(const ZayaanAppsStoreApp());
}

class ZayaanAppsStoreApp extends StatefulWidget {
  const ZayaanAppsStoreApp({super.key});

  @override
  State<ZayaanAppsStoreApp> createState() => _ZayaanAppsStoreAppState();
}

class _ZayaanAppsStoreAppState extends State<ZayaanAppsStoreApp> {
  ThemeMode _themeMode = ThemeMode.system;

  void _setThemeMode(ThemeMode mode) {
    setState(() {
      _themeMode = mode;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Zayaan Apps',
      debugShowCheckedModeBanner: false,
      themeMode: _themeMode,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: const ColorScheme.light(
          primary: Color(0xFF006C4A),
          onPrimary: Colors.white,
          primaryContainer: Color(0xFF8BF8C4),
          onPrimaryContainer: Color(0xFF002114),
          secondary: Color(0xFF00658E),
          onSecondary: Colors.white,
          secondaryContainer: Color(0xFFC8E6FF),
          onSecondaryContainer: Color(0xFF001E2E),
          tertiary: Color(0xFF3E5F90),
          onTertiary: Colors.white,
          tertiaryContainer: Color(0xFFD6E3FF),
          onTertiaryContainer: Color(0xFF001B3E),
          surface: Color(0xFFFFFFFF),
          onSurface: Color(0xFF171D1A),
          surfaceContainerHighest: Color(0xFFDBE5DE),
          onSurfaceVariant: Color(0xFF404944),
          outline: Color(0xFF707973),
        ),
        scaffoldBackgroundColor: const Color(0xFFF6FBF8),
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF6EDBA9),
          onPrimary: Color(0xFF003824),
          primaryContainer: Color(0xFF005236),
          onPrimaryContainer: Color(0xFF8BF8C4),
          secondary: Color(0xFF68D3FF),
          onSecondary: Color(0xFF003546),
          secondaryContainer: Color(0xFF004D64),
          onSecondaryContainer: Color(0xFFBDE9FF),
          tertiary: Color(0xFFADC6FF),
          onTertiary: Color(0xFF0F2F60),
          tertiaryContainer: Color(0xFF2B4678),
          onTertiaryContainer: Color(0xFFD8E2FF),
          surface: Color(0xFF0E1E38),
          onSurface: Color(0xFFE0E6ED),
          surfaceContainerHighest: Color(0xFF1B2E4B),
          onSurfaceVariant: Color(0xFFB8C4D4),
          outline: Color(0xFF3E5272),
        ),
        scaffoldBackgroundColor: const Color(0xFF0A192F),
      ),
      home: ZayaanResponsiveShell(
        themeMode: _themeMode,
        onThemeModeChanged: _setThemeMode,
      ),
    );
  }
}

class ZayaanResponsiveShell extends StatefulWidget {
  final ThemeMode themeMode;
  final ValueChanged<ThemeMode> onThemeModeChanged;

  const ZayaanResponsiveShell({
    super.key,
    required this.themeMode,
    required this.onThemeModeChanged,
  });

  @override
  State<ZayaanResponsiveShell> createState() => _ZayaanResponsiveShellState();
}

class _ZayaanResponsiveShellState extends State<ZayaanResponsiveShell> {
  int _selectedIndex = 0;
  String _searchQuery = '';
  String _selectedCategory = 'All';
  String _selectedSubcategory = 'All';
  MarketplaceSortOption _selectedSort = MarketplaceSortOption.relevance;
  StoreAppModel? _selectedApp;
  String? _statusBanner;
  double? _downloadProgress;

  // Phase 2 & Phase 3 synchronized state
  final Set<String> _wishlistIds = <String>{};
  final Set<String> _favoriteIds = <String>{'zayaan-apps-store'};
  final List<String> _downloadHistoryIds = <String>[];
  final List<String> _recentlyViewedIds = <String>[];
  final List<String> _searchHistory = <String>['AI Assistant', 'File Commander'];
  final List<String> _comparisonIds = <String>[];
  final Map<String, List<AppReviewModel>> _reviewsByApp =
      <String, List<AppReviewModel>>{};
  int _draftStars = 5;
  final TextEditingController _reviewCommentController = TextEditingController();
  bool _notifyUpdates = true;
  bool _whatsNewExpanded = false;
  bool _aboutExpanded = false;
  StreamSubscription<List<AppReviewModel>>? _reviewsSubscription;
  final List<TapGestureRecognizer> _linkRecognizers = <TapGestureRecognizer>[];

  String get _currentUserId {
    try {
      if (Firebase.apps.isNotEmpty) {
        return FirebaseAuth.instance.currentUser?.uid ?? 'web_user';
      }
    } catch (_) {}
    return 'web_user';
  }

  ZayaanFirestoreService? _tryFirestore() {
    try {
      if (Firebase.apps.isNotEmpty) {
        return ZayaanFirestoreService();
      }
    } catch (_) {}
    return null;
  }

  @override
  void initState() {
    super.initState();
    _syncLatestGitHubRelease();
  }

  @override
  void dispose() {
    _reviewsSubscription?.cancel();
    _reviewCommentController.dispose();
    for (final recognizer in _linkRecognizers) {
      recognizer.dispose();
    }
    super.dispose();
  }

  Future<void> _syncLatestGitHubRelease() async {
    try {
      final response = await http.get(
        Uri.parse('https://api.github.com/repos/zayaanapps/zayaan-apps/releases/latest'),
        headers: const {'Accept': 'application/vnd.github+json'},
      );
      if (response.statusCode != 200) return;
      final jsonMap = jsonDecode(response.body) as Map<String, dynamic>;
      final rawTag = (jsonMap['tag_name'] as String?) ?? '';
      final versionName = rawTag.replaceFirst(RegExp(r'^v'), '').trim();
      if (versionName.isEmpty) return;

      final assets = (jsonMap['assets'] as List<dynamic>?) ?? const [];
      String apkUrl = '';
      String sizeFormatted = '16.8 MB';
      for (final item in assets) {
        if (item is Map<String, dynamic>) {
          final name = (item['name'] as String?) ?? '';
          final url = (item['browser_download_url'] as String?) ?? '';
          if (name.endsWith('.apk') || url.endsWith('.apk')) {
            apkUrl = url;
            final bytes = (item['size'] as num?)?.toInt() ?? 0;
            if (bytes > 0) {
              sizeFormatted = '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
            }
            if (name.contains('release')) break;
          }
        }
      }

      final parsedCode = int.tryParse(versionName.split('.').last) ?? 1;
      final rawNotes = ((jsonMap['body'] as String?) ?? '').trim();
      final notes = _sanitizeWhatsNewNotes(rawNotes, versionName);
      final shaMatch = RegExp(
        r'SHA-?256[^0-9a-fA-F]*([0-9a-fA-F]{64})',
        caseSensitive: false,
      ).firstMatch(rawNotes);
      final parsedSha256 = shaMatch?.group(1)?.toLowerCase() ?? '';

      if (!mounted) return;
      setState(() {
        final idx = _catalog.indexWhere((a) => a.appId == 'zayaan-apps-store');
        if (idx >= 0) {
          final existing = _catalog[idx];
          _catalog[idx] = StoreAppModel(
            appId: existing.appId,
            appName: existing.appName,
            packageName: existing.packageName,
            category: existing.category,
            developerName: existing.developerName,
            shortDescription: existing.shortDescription,
            description: existing.description,
            iconUrl: existing.iconUrl,
            screenshots: existing.screenshots,
            versionName: versionName,
            versionCode: parsedCode > 0 ? parsedCode : existing.versionCode,
            apkDownloadUrl: apkUrl.isNotEmpty ? apkUrl : existing.apkDownloadUrl,
            apkFileSize: sizeFormatted,
            releaseNotes: notes.isNotEmpty ? notes : existing.releaseNotes,
            featured: existing.featured,
            editorsChoice: existing.editorsChoice,
            essential: existing.essential,
            published: existing.published,
            downloadCount: existing.downloadCount,
            ratingSum: existing.ratingSum,
            reviewCount: existing.reviewCount,
            sha256Checksum: parsedSha256.isNotEmpty
                ? parsedSha256
                : existing.sha256Checksum,
            minAndroidSdk: existing.minAndroidSdk,
            targetAndroidSdk: existing.targetAndroidSdk,
            architecture: existing.architecture,
            permissionsList: existing.permissionsList,
            offlineCapable: existing.offlineCapable,
            pageViews: existing.pageViews,
            authorUid: existing.authorUid,
          );
          if (_selectedApp?.appId == 'zayaan-apps-store') {
            _selectedApp = _catalog[idx];
          }
        }
      });
    } catch (_) {
      // Keep bundled fallback catalog cleanly when offline.
    }
  }

  // Complete 8-app Official Zayaan Apps Catalog (100% identical to AI Studio Android Preview)
  late final List<StoreAppModel> _catalog = [
    const StoreAppModel(
      appId: 'zayaan-apps-store',
      appName: 'Zayaan Apps Store',
      packageName: 'com.zayaanapps.store',
      category: 'Tools',
      developerName: 'Zayaan Apps Official',
      shortDescription:
          'Official Zayaan Apps Android marketplace & GitHub Releases distribution client.',
      description:
          'The official Zayaan Apps Android App Store & Web Distribution client. Discover, download, verify, and update official Zayaan Android applications directly from GitHub Releases with real-time version tracking.',
      iconUrl:
          'https://raw.githubusercontent.com/zayaanapps/zayaan-apps/main/app/src/main/res/drawable/img_launcher_icon_1795021165006.jpg',
      screenshots: [
        'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80',
        'https://images.unsplash.com/photo-1551650975-87deedd944c3?w=800&q=80',
        'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80',
      ],
      versionName: '1.0.0.1',
      versionCode: 1,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '14.2 MB',
      releaseNotes:
          "### Latest Updates, Fixes & Changes (v1.0.0.1)\n• Automatically deletes downloaded APK packages right after installation completes\n• Guided device permission prompts for Install Unknown Apps and Notifications\n• Moved Package ID, version, compatibility & release specs into About This Application\n• Polished compact bordered Home & App Details UI with System/Light/Dark theme support",
      featured: true,
      editorsChoice: true,
      essential: true,
      published: true,
      downloadCount: 128,
      ratingSum: 60,
      reviewCount: 12,
      minAndroidSdk: 24,
      targetAndroidSdk: 35,
      architecture: 'arm64-v8a, armeabi-v7a, x86_64',
      permissionsList: [
        'INTERNET',
        'REQUEST_INSTALL_PACKAGES',
        'POST_NOTIFICATIONS',
        'QUERY_ALL_PACKAGES',
      ],
      offlineCapable: false,
      pageViews: 410,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-ai-studio',
      appName: 'Zayaan AI Assistant',
      packageName: 'com.zayaanapps.aistudio',
      category: 'AI Apps',
      developerName: 'Zayaan AI Lab',
      shortDescription:
          'Multimodal AI coding assistant, prompt library & voice reasoning companion.',
      description:
          'Next-generation multimodal AI assistant built for Android. Includes code synthesis, markdown export, offline prompt library, and instant voice-to-text reasoning workflows.',
      iconUrl:
          'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=256&q=80',
      screenshots: [
        'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&q=80',
        'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800&q=80',
      ],
      versionName: '2.4.0.1',
      versionCode: 24,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '18.6 MB',
      releaseNotes:
          'v2.4.0.1:\n• Faster streaming responses\n• Added code syntax highlighting for Kotlin, Dart, and Python\n• Offline prompt templates',
      featured: true,
      editorsChoice: true,
      essential: true,
      published: true,
      downloadCount: 342,
      ratingSum: 88,
      reviewCount: 18,
      minAndroidSdk: 26,
      targetAndroidSdk: 35,
      architecture: 'arm64-v8a',
      permissionsList: ['INTERNET', 'RECORD_AUDIO'],
      offlineCapable: true,
      pageViews: 820,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-file-commander',
      appName: 'Zayaan File Commander',
      packageName: 'com.zayaanapps.filecommander',
      category: 'Utilities',
      developerName: 'Zayaan Systems',
      shortDescription:
          'Dual-pane Android file explorer, SHA-256 APK verifier & archive manager.',
      description:
          'High-speed Android file manager, APK inspector, and archive extractor with Material 3 expressive design, storage heatmap analytics, and zero ads.',
      iconUrl:
          'https://images.unsplash.com/photo-1618401471353-b98afee0b2eb?w=256&q=80',
      screenshots: [
        'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&q=80',
        'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80',
      ],
      versionName: '1.8.2.1',
      versionCode: 18,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '9.4 MB',
      releaseNotes:
          'v1.8.2.1:\n• Added SHA-256 APK checksum verification tool\n• Dual-pane tablet and foldable layout support',
      featured: true,
      editorsChoice: false,
      essential: true,
      published: true,
      downloadCount: 215,
      ratingSum: 48,
      reviewCount: 10,
      minAndroidSdk: 24,
      targetAndroidSdk: 35,
      architecture: 'arm64-v8a, armeabi-v7a',
      permissionsList: ['INTERNET'],
      offlineCapable: true,
      pageViews: 530,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-cyber-runner',
      appName: 'Cyber Grid Runner 3D',
      packageName: 'com.zayaanapps.cyberrunner',
      category: 'Games',
      developerName: 'Zayaan Interactive',
      shortDescription:
          '120Hz synthwave arcade runner with gamepad support & offline procedural worlds.',
      description:
          'Fast-paced neon synthwave arcade runner featuring 120Hz display support, controller compatibility, and procedural obstacle courses.',
      iconUrl:
          'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=256&q=80',
      screenshots: [
        'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80',
        'https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800&q=80',
      ],
      versionName: '3.1.0.1',
      versionCode: 31,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '45.0 MB',
      releaseNotes:
          'v3.1.0.1:\n• New Cyber Horizon endless stage\n• Full Bluetooth gamepad support',
      featured: false,
      editorsChoice: true,
      essential: false,
      published: true,
      downloadCount: 519,
      ratingSum: 116,
      reviewCount: 24,
      minAndroidSdk: 26,
      targetAndroidSdk: 35,
      architecture: 'arm64-v8a',
      permissionsList: ['INTERNET', 'VIBRATE'],
      offlineCapable: true,
      pageViews: 1120,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-focus-flow',
      appName: 'Zayaan Focus & Pomodoro',
      packageName: 'com.zayaanapps.focusflow',
      category: 'Productivity',
      developerName: 'Zayaan Apps Official',
      shortDescription:
          'Minimalist deep-work timer, Kanban task matrix & ambient soundscapes.',
      description:
          'Minimalist deep-work timer, task matrix, and daily productivity analytics with ambient soundscapes and cloud sync.',
      iconUrl:
          'https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=256&q=80',
      screenshots: [
        'https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=800&q=80',
      ],
      versionName: '1.5.0.1',
      versionCode: 15,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '11.1 MB',
      releaseNotes:
          'v1.5.0.1:\n• Added weekly streak heatmaps and Kanban task boards',
      featured: false,
      editorsChoice: false,
      essential: true,
      published: true,
      downloadCount: 176,
      ratingSum: 39,
      reviewCount: 8,
      minAndroidSdk: 24,
      targetAndroidSdk: 35,
      architecture: 'arm64-v8a, armeabi-v7a',
      permissionsList: ['INTERNET', 'POST_NOTIFICATIONS', 'VIBRATE'],
      offlineCapable: true,
      pageViews: 390,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-code-academy',
      appName: 'Zayaan Code Academy',
      packageName: 'com.zayaanapps.codeacademy',
      category: 'Education',
      developerName: 'Zayaan Education',
      shortDescription:
          'Interactive Kotlin, Jetpack Compose, Flutter & GitHub Actions labs.',
      description:
          'Interactive lessons, quizzes, and sandbox labs for learning Kotlin, Jetpack Compose, Flutter, and GitHub Actions automation on the go.',
      iconUrl:
          'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=256&q=80',
      screenshots: [
        'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&q=80',
      ],
      versionName: '1.2.0.1',
      versionCode: 12,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '16.8 MB',
      releaseNotes: 'v1.2.0.1:\n• New Jetpack Compose & CI/CD tracks added',
      featured: false,
      editorsChoice: true,
      essential: false,
      published: true,
      downloadCount: 143,
      ratingSum: 34,
      reviewCount: 7,
      minAndroidSdk: 24,
      targetAndroidSdk: 35,
      architecture: 'arm64-v8a, armeabi-v7a',
      permissionsList: ['INTERNET'],
      offlineCapable: true,
      pageViews: 310,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-invoice-pro',
      appName: 'Zayaan Invoice & Ledger',
      packageName: 'com.zayaanapps.invoicepro',
      category: 'Business',
      developerName: 'Zayaan Systems',
      shortDescription:
          'Offline-first PDF invoice generator, expense ledger & multi-currency billing.',
      description:
          'Professional invoice creator, client billing tracker, and PDF receipt generator designed for freelancers and small businesses with offline-first accuracy.',
      iconUrl:
          'https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=256&q=80',
      screenshots: [
        'https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=800&q=80',
        'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800&q=80',
      ],
      versionName: '1.1.0.1',
      versionCode: 11,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '10.5 MB',
      releaseNotes:
          'v1.1.0.1:\n• Instant PDF export & customizable tax templates',
      featured: false,
      editorsChoice: false,
      essential: true,
      published: true,
      downloadCount: 94,
      ratingSum: 24,
      reviewCount: 5,
      minAndroidSdk: 24,
      targetAndroidSdk: 35,
      architecture: 'arm64-v8a, armeabi-v7a',
      permissionsList: ['INTERNET'],
      offlineCapable: true,
      pageViews: 215,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-connect-hub',
      appName: 'Zayaan Connect Hub',
      packageName: 'com.zayaanapps.connecthub',
      category: 'Social & Communication',
      developerName: 'Zayaan Apps Official',
      shortDescription:
          'Developer community channels, release announcements & markdown discussions.',
      description:
          'Community hub for Zayaan Apps users and Android developers. Share release feedback, join beta testing channels, and discuss open-source Android tools.',
      iconUrl:
          'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=256&q=80',
      screenshots: [
        'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800&q=80',
      ],
      versionName: '1.0.4.1',
      versionCode: 5,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '12.8 MB',
      releaseNotes:
          'v1.0.4.1:\n• Added direct GitHub release discussion threads',
      featured: false,
      editorsChoice: false,
      essential: false,
      published: true,
      downloadCount: 87,
      ratingSum: 19,
      reviewCount: 4,
      minAndroidSdk: 24,
      targetAndroidSdk: 35,
      architecture: 'arm64-v8a, armeabi-v7a',
      permissionsList: ['INTERNET', 'POST_NOTIFICATIONS'],
      offlineCapable: false,
      pageViews: 195,
      authorUid: 'admin',
    ),
  ];

  void _openAppDetail(StoreAppModel app) {
    _reviewsSubscription?.cancel();
    final fs = _tryFirestore();
    if (fs != null) {
      _reviewsSubscription = fs.observeReviewsForApp(app.appId).listen(
        (cloudReviews) {
          if (!mounted) return;
          setState(() {
            _reviewsByApp[app.appId] = cloudReviews;
          });
        },
        onError: (_) {},
      );
    }
    setState(() {
      _selectedApp = app;
      _whatsNewExpanded = false;
      _aboutExpanded = false;
      _recentlyViewedIds.remove(app.appId);
      _recentlyViewedIds.insert(0, app.appId);
    });
  }

  Future<void> _handleDownload(StoreAppModel app) async {
    setState(() {
      _downloadProgress = 0.0;
      if (!_downloadHistoryIds.contains(app.appId)) {
        _downloadHistoryIds.insert(0, app.appId);
      }
    });
    await PlatformApkInstaller.downloadAndInstallOrOpen(
      app: app,
      onProgress: (p) => setState(() => _downloadProgress = p),
      onStatus: (msg) => setState(() => _statusBanner = msg),
    );
    setState(() {
      _downloadProgress = null;
    });
  }

  void _toggleCompare(String appId) {
    setState(() {
      if (_comparisonIds.contains(appId)) {
        _comparisonIds.remove(appId);
      } else {
        if (_comparisonIds.length >= 4) {
          _comparisonIds.removeAt(0);
        }
        _comparisonIds.add(appId);
        _statusBanner = 'Added to Side-by-Side Comparison in My Apps.';
      }
    });
  }

  Future<void> _launchExternalUrl(String rawUrl) async {
    final uri = Uri.tryParse(rawUrl.trim());
    if (uri == null) return;
    try {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } catch (_) {
      if (mounted) {
        setState(() {
          _statusBanner = 'Could not open URL: $rawUrl';
        });
      }
    }
  }

  Future<void> _saveWebReview(
    StoreAppModel app, {
    required int stars,
    required String comment,
  }) async {
    final cleanStars = stars.clamp(1, 5);
    final cleanComment =
        comment.trim().isEmpty ? 'Rated $cleanStars stars.' : comment.trim();
    final uid = _currentUserId;
    String userName = 'Zayaan Community Member';
    bool syncedCloud = false;

    try {
      if (Firebase.apps.isNotEmpty) {
        final user = FirebaseAuth.instance.currentUser;
        if (user != null) {
          userName = (user.displayName ?? user.email ?? 'Zayaan User')
              .split('@')
              .first;
          final fs = _tryFirestore();
          if (fs != null) {
            await fs.submitReview(
              app: app,
              rating: cleanStars,
              comment: cleanComment,
              verifiedInstall: PlatformApkInstaller.isAndroidPlatform,
            );
            syncedCloud = true;
          }
        }
      }
    } catch (_) {}

    if (!mounted) return;
    final list = _reviewsByApp.putIfAbsent(app.appId, () => <AppReviewModel>[]);
    setState(() {
      final existingIndex = list.indexWhere((r) => r.userId == uid);
      if (existingIndex >= 0) {
        final prev = list[existingIndex];
        list[existingIndex] = AppReviewModel(
          id: prev.id,
          appId: app.appId,
          appName: app.appName,
          userId: uid,
          userName: prev.userName.isNotEmpty ? prev.userName : userName,
          rating: cleanStars,
          comment: cleanComment,
          verifiedInstall: prev.verifiedInstall,
          helpfulCount: prev.helpfulCount,
          developerReply: prev.developerReply,
          flagged: prev.flagged,
        );
      } else {
        list.insert(
          0,
          AppReviewModel(
            id: '${app.appId}_$uid',
            appId: app.appId,
            appName: app.appName,
            userId: uid,
            userName: userName,
            rating: cleanStars,
            comment: cleanComment,
            verifiedInstall: PlatformApkInstaller.isAndroidPlatform,
            helpfulCount: 0,
          ),
        );
      }
      _reviewCommentController.clear();
      _statusBanner = syncedCloud
          ? 'Saved your $cleanStars★ review to Cloud Firestore.'
          : 'Saved your $cleanStars★ review for ${app.appName}.';
    });
  }

  Future<void> _deleteWebReview(StoreAppModel app, AppReviewModel review) async {
    bool syncedCloud = false;
    try {
      if (Firebase.apps.isNotEmpty && FirebaseAuth.instance.currentUser != null) {
        final fs = _tryFirestore();
        if (fs != null) {
          await fs.deleteReview(review);
          syncedCloud = true;
        }
      }
    } catch (_) {}

    if (!mounted) return;
    setState(() {
      _reviewsByApp[app.appId]?.removeWhere((r) => r.id == review.id);
      _statusBanner = syncedCloud
          ? 'Deleted review from Cloud Firestore.'
          : 'Removed your review.';
    });
  }

  void _cycleThemeMode() {
    final next = switch (widget.themeMode) {
      ThemeMode.system => ThemeMode.light,
      ThemeMode.light => ThemeMode.dark,
      ThemeMode.dark => ThemeMode.system,
    };
    widget.onThemeModeChanged(next);
  }

  String _themeModeLabel(ThemeMode mode) => switch (mode) {
        ThemeMode.system => 'System',
        ThemeMode.light => 'Light',
        ThemeMode.dark => 'Dark',
      };

  List<StoreAppModel> _getFilteredApps() {
    final list = _catalog.where((app) {
      final matchesCat =
          _selectedCategory == 'All' || app.category == _selectedCategory;
      final matchesSub = switch (_selectedSubcategory) {
        "Editor's Choice" => app.editorsChoice,
        'Essential Apps' => app.essential,
        'Offline Ready' => app.offlineCapable,
        _ => true,
      };
      final q = _searchQuery.trim().toLowerCase();
      final matchesSearch = q.isEmpty ||
          app.appName.toLowerCase().contains(q) ||
          app.shortDescription.toLowerCase().contains(q) ||
          app.description.toLowerCase().contains(q) ||
          app.packageName.toLowerCase().contains(q) ||
          app.developerName.toLowerCase().contains(q);
      return matchesCat && matchesSub && matchesSearch;
    }).toList();

    switch (_selectedSort) {
      case MarketplaceSortOption.popularity:
        list.sort((a, b) => b.downloadCount.compareTo(a.downloadCount));
      case MarketplaceSortOption.highestRated:
        list.sort((a, b) => b.averageRating.compareTo(a.averageRating));
      case MarketplaceSortOption.alphabetical:
        list.sort((a, b) => a.appName.compareTo(b.appName));
      default:
        break;
    }
    return list;
  }

  @override
  Widget build(BuildContext context) {
    final isWide = MediaQuery.of(context).size.width >= 760;
    final categories = ['All', ...kStoreCategories];
    final filteredApps = _getFilteredApps();

    return Scaffold(
      bottomNavigationBar: isWide
          ? null
          : NavigationBar(
              selectedIndex: _selectedIndex,
              onDestinationSelected: (idx) {
                setState(() {
                  _selectedApp = null;
                  _selectedIndex = idx;
                });
              },
              destinations: const [
                NavigationDestination(
                    icon: Icon(Icons.shop), label: 'Store'),
                NavigationDestination(
                    icon: Icon(Icons.apps), label: 'My Apps'),
                NavigationDestination(
                    icon: Icon(Icons.admin_panel_settings), label: 'Admin'),
                NavigationDestination(
                    icon: Icon(Icons.public), label: 'Web & DevOps'),
              ],
            ),
      body: SafeArea(
        child: Row(
          children: [
            if (isWide)
              NavigationRail(
                selectedIndex: _selectedIndex,
                onDestinationSelected: (idx) {
                  setState(() {
                    _selectedApp = null;
                    _selectedIndex = idx;
                  });
                },
                labelType: NavigationRailLabelType.all,
                destinations: const [
                  NavigationRailDestination(
                      icon: Icon(Icons.shop), label: Text('Store')),
                  NavigationRailDestination(
                      icon: Icon(Icons.apps), label: Text('My Apps')),
                  NavigationRailDestination(
                      icon: Icon(Icons.admin_panel_settings),
                      label: Text('Admin')),
                  NavigationRailDestination(
                      icon: Icon(Icons.public), label: Text('Web & DevOps')),
                ],
              ),
            Expanded(
              child: Column(
                children: [
                  if (_statusBanner != null)
                    Container(
                      margin: const EdgeInsets.fromLTRB(16, 8, 16, 4),
                      padding: const EdgeInsets.symmetric(
                          horizontal: 14, vertical: 10),
                      decoration: BoxDecoration(
                        color: Theme.of(context).colorScheme.secondaryContainer,
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              _statusBanner!,
                              style: TextStyle(
                                color: Theme.of(context)
                                    .colorScheme
                                    .onSecondaryContainer,
                              ),
                            ),
                          ),
                          IconButton(
                            onPressed: () =>
                                setState(() => _statusBanner = null),
                            icon: const Icon(Icons.close, size: 18),
                          ),
                        ],
                      ),
                    ),
                  if (_downloadProgress != null)
                    LinearProgressIndicator(value: _downloadProgress),
                  Expanded(
                    child: _selectedApp != null
                        ? _buildDetailView(_selectedApp!)
                        : _buildTabContent(categories, filteredApps),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTabContent(
      List<String> categories, List<StoreAppModel> filteredApps) {
    switch (_selectedIndex) {
      case 0:
        return _buildStoreHomeTab(categories, filteredApps);
      case 1:
        return _buildMyAppsTab();
      case 2:
        return _buildAdminTab();
      default:
        return _buildWebDevOpsTab();
    }
  }

  IconData _webCategoryIcon(String category) => switch (category) {
        'All' => Icons.apps,
        'Games' => Icons.sports_esports,
        'Tools' => Icons.build,
        'AI Apps' => Icons.auto_awesome,
        'Entertainment' => Icons.movie_outlined,
        'Productivity' => Icons.work_outline,
        'Education' => Icons.school_outlined,
        'Utilities' => Icons.build_circle_outlined,
        'Business' => Icons.storefront_outlined,
        'Social & Communication' => Icons.forum_outlined,
        _ => Icons.category_outlined,
      };

  IconData _webSubcategoryIcon(String sub) => switch (sub) {
        "Editor's Choice" => Icons.workspace_premium_outlined,
        'Essential Apps' => Icons.verified_outlined,
        'Offline Ready' => Icons.offline_bolt_outlined,
        'Under 15 MB' => Icons.sd_storage_outlined,
        'Latest v1.0.0.x' => Icons.new_releases_outlined,
        _ => Icons.apps,
      };

  Widget _buildHomeAppIcon(StoreAppModel app, ColorScheme cs,
      {double size = 48, double radius = 12}) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: cs.primaryContainer,
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: cs.outline.withValues(alpha: 0.28)),
      ),
      clipBehavior: Clip.antiAlias,
      child: app.iconUrl.isNotEmpty
          ? Image.network(
              app.iconUrl,
              fit: BoxFit.cover,
              errorBuilder: (_, __, ___) => Center(
                child: Text(
                  app.appName.substring(0, 1),
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: cs.onPrimaryContainer,
                  ),
                ),
              ),
            )
          : Center(
              child: Text(
                app.appName.substring(0, 1),
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: cs.onPrimaryContainer,
                ),
              ),
            ),
    );
  }

  Widget _buildHomeSectionHeader({
    required ColorScheme cs,
    required IconData icon,
    required Color iconColor,
    required String title,
    required String subtitle,
  }) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(
            color: iconColor.withValues(alpha: 0.14),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: iconColor.withValues(alpha: 0.30)),
          ),
          child: Icon(icon, size: 17, color: iconColor),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style:
                    const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              Text(
                subtitle,
                style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildStoreHomeTab(
      List<String> categories, List<StoreAppModel> filteredApps) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;
    final isDark = theme.brightness == Brightness.dark;
    final featuredApps = _catalog.where((a) => a.featured).toList();
    final editorsChoiceApps = _catalog.where((a) => a.editorsChoice).toList();
    final trendingApps = [..._catalog]
      ..sort((a, b) => b.downloadCount.compareTo(a.downloadCount));
    final totalDownloads =
        _catalog.fold<int>(0, (sum, a) => sum + a.downloadCount);

    final heroBorderColor = isDark
        ? const Color(0xFF6EDBA9).withValues(alpha: 0.38)
        : cs.primary.withValues(alpha: 0.24);
    final heroTitleColor = isDark ? Colors.white : cs.onSurface;
    final heroSubtitleColor =
        isDark ? const Color(0xFFC9DAE8) : cs.onSurfaceVariant;
    final heroMetricBg = isDark
        ? Colors.black.withValues(alpha: 0.26)
        : cs.surface.withValues(alpha: 0.88);
    final heroMetricBorder = isDark
        ? Colors.white.withValues(alpha: 0.16)
        : cs.outline.withValues(alpha: 0.20);
    final heroDividerColor = isDark
        ? Colors.white.withValues(alpha: 0.20)
        : cs.outline.withValues(alpha: 0.20);

    return ListView(
      padding: const EdgeInsets.fromLTRB(14, 10, 14, 14),
      children: [
        // 1. Minimal, Compact & Theme-Adaptive Header
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            gradient: LinearGradient(
              colors: isDark
                  ? const [
                      Color(0xFF005236),
                      Color(0xFF0C2235),
                      Color(0xFF0E1E38),
                    ]
                  : const [
                      Color(0xFFEAF8F1),
                      Color(0xFFF3FAF7),
                      Color(0xFFEAF3FB),
                    ],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            border: Border.all(color: heroBorderColor),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Flexible(
                              child: Text(
                                'Zayaan Apps Store',
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.w800,
                                  color: heroTitleColor,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            const SizedBox(width: 5),
                            Icon(
                              Icons.verified,
                              size: 15,
                              color: isDark
                                  ? const Color(0xFF6EDBA9)
                                  : cs.primary,
                            ),
                          ],
                        ),
                        const SizedBox(height: 1),
                        Text(
                          'Verified Android APKs • GitHub Releases & Cloud',
                          style: TextStyle(
                            fontSize: 11,
                            color: heroSubtitleColor,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  ActionChip(
                    materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    visualDensity:
                        const VisualDensity(horizontal: -2, vertical: -3),
                    padding: const EdgeInsets.symmetric(horizontal: 6),
                    labelPadding: const EdgeInsets.only(right: 4),
                    avatar: Icon(
                      switch (widget.themeMode) {
                        ThemeMode.system => Icons.brightness_auto,
                        ThemeMode.light => Icons.light_mode,
                        ThemeMode.dark => Icons.dark_mode,
                      },
                      size: 14,
                      color: isDark ? const Color(0xFF6EDBA9) : cs.primary,
                    ),
                    label: Text(
                      _themeModeLabel(widget.themeMode),
                      style: TextStyle(
                        fontSize: 11,
                        color: heroTitleColor,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    backgroundColor: isDark
                        ? Colors.white.withValues(alpha: 0.12)
                        : cs.surface.withValues(alpha: 0.90),
                    side: BorderSide(
                      color: isDark
                          ? const Color(0xFF6EDBA9).withValues(alpha: 0.40)
                          : cs.primary.withValues(alpha: 0.25),
                    ),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(9),
                    ),
                    onPressed: _cycleThemeMode,
                    tooltip: 'Switch Theme (System / Light / Dark)',
                  ),
                ],
              ),
              const SizedBox(height: 7),
              // Minimal bordered 3-metric bar inside hero
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(
                  color: heroMetricBg,
                  borderRadius: BorderRadius.circular(9),
                  border: Border.all(color: heroMetricBorder),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.apps,
                            size: 12,
                            color:
                                isDark ? const Color(0xFF6EDBA9) : cs.primary),
                        const SizedBox(width: 4),
                        Text(
                          '${_catalog.length} Apps',
                          style: TextStyle(
                            fontSize: 10.5,
                            fontWeight: FontWeight.w600,
                            color: heroTitleColor,
                          ),
                        ),
                      ],
                    ),
                    Container(width: 1, height: 11, color: heroDividerColor),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.download,
                            size: 12,
                            color: isDark
                                ? const Color(0xFF68D3FF)
                                : cs.secondary),
                        const SizedBox(width: 4),
                        Text(
                          '$totalDownloads DLs',
                          style: TextStyle(
                            fontSize: 10.5,
                            fontWeight: FontWeight.w600,
                            color: heroTitleColor,
                          ),
                        ),
                      ],
                    ),
                    Container(width: 1, height: 11, color: heroDividerColor),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.public,
                            size: 12,
                            color:
                                isDark ? const Color(0xFFADC6FF) : cs.tertiary),
                        const SizedBox(width: 4),
                        Text(
                          'Android & Web',
                          style: TextStyle(
                            fontSize: 10.5,
                            fontWeight: FontWeight.w600,
                            color: heroTitleColor,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 7),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                children: [
                  FilledButton.icon(
                    style: FilledButton.styleFrom(
                      visualDensity: VisualDensity.compact,
                      padding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 6),
                      textStyle: const TextStyle(
                          fontSize: 12, fontWeight: FontWeight.w600),
                    ),
                    onPressed: () => _handleDownload(_catalog.first),
                    icon: const Icon(Icons.download, size: 15),
                    label: const Text('Download Store APK'),
                  ),
                  OutlinedButton.icon(
                    onPressed: () => _openAppDetail(_catalog.first),
                    style: OutlinedButton.styleFrom(
                      visualDensity: VisualDensity.compact,
                      padding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 6),
                      textStyle: const TextStyle(
                          fontSize: 12, fontWeight: FontWeight.w600),
                      foregroundColor: isDark ? Colors.white : cs.primary,
                      side: BorderSide(
                        color: isDark
                            ? const Color(0xFF6EDBA9).withValues(alpha: 0.55)
                            : cs.primary.withValues(alpha: 0.38),
                      ),
                    ),
                    icon: const Icon(Icons.verified_user_outlined, size: 15),
                    label: const Text('Inspect Release'),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 10),

        // 2. Compact Bordered Search Bar & Minimal Filter Rows
        TextField(
          style: const TextStyle(fontSize: 13.5),
          decoration: InputDecoration(
            isDense: true,
            constraints: const BoxConstraints(maxHeight: 42),
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 10, vertical: 9),
            hintText: 'Search Zayaan Apps, packages, developers...',
            hintStyle: const TextStyle(fontSize: 13),
            prefixIcon: const Icon(Icons.search, size: 18),
            prefixIconConstraints:
                const BoxConstraints(minWidth: 36, minHeight: 36),
            suffixIcon: _searchQuery.isNotEmpty
                ? IconButton(
                    icon: const Icon(Icons.clear, size: 16),
                    onPressed: () => setState(() => _searchQuery = ''),
                  )
                : null,
            suffixIconConstraints:
                const BoxConstraints(minWidth: 34, minHeight: 34),
            filled: true,
            fillColor: cs.surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(11),
              borderSide: BorderSide(color: cs.outline.withValues(alpha: 0.30)),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(11),
              borderSide: BorderSide(color: cs.outline.withValues(alpha: 0.30)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(11),
              borderSide: BorderSide(color: cs.primary.withValues(alpha: 0.78)),
            ),
          ),
          onChanged: (v) => setState(() => _searchQuery = v),
        ),
        const SizedBox(height: 7),

        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: categories.map((cat) {
              final selected = _selectedCategory == cat;
              return Padding(
                padding: const EdgeInsets.only(right: 6),
                child: FilterChip(
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  visualDensity:
                      const VisualDensity(horizontal: -2, vertical: -3),
                  padding: const EdgeInsets.symmetric(horizontal: 6),
                  labelPadding: const EdgeInsets.symmetric(horizontal: 3),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(9),
                  ),
                  avatar: Icon(_webCategoryIcon(cat), size: 13),
                  label: Text(cat, style: const TextStyle(fontSize: 11.5)),
                  selected: selected,
                  side: BorderSide(
                    color: selected
                        ? cs.primary.withValues(alpha: 0.50)
                        : cs.outline.withValues(alpha: 0.26),
                  ),
                  onSelected: (_) => setState(() => _selectedCategory = cat),
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 5),

        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: [
              ...kMarketplaceSubcategories.map((sub) {
                final selected = _selectedSubcategory == sub;
                return Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: FilterChip(
                    materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    visualDensity:
                        const VisualDensity(horizontal: -2, vertical: -3),
                    padding: const EdgeInsets.symmetric(horizontal: 6),
                    labelPadding: const EdgeInsets.symmetric(horizontal: 3),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(9),
                    ),
                    avatar: Icon(_webSubcategoryIcon(sub), size: 13),
                    label: Text(sub, style: const TextStyle(fontSize: 11)),
                    selected: selected,
                    side: BorderSide(
                      color: selected
                          ? cs.secondary.withValues(alpha: 0.50)
                          : cs.outline.withValues(alpha: 0.25),
                    ),
                    onSelected: (_) =>
                        setState(() => _selectedSubcategory = sub),
                  ),
                );
              }),
              const SizedBox(width: 4),
              Container(
                height: 30,
                padding: const EdgeInsets.symmetric(horizontal: 8),
                decoration: BoxDecoration(
                  color: cs.surface,
                  borderRadius: BorderRadius.circular(9),
                  border: Border.all(color: cs.outline.withValues(alpha: 0.26)),
                ),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<MarketplaceSortOption>(
                    isDense: true,
                    value: _selectedSort,
                    onChanged: (opt) {
                      if (opt != null) setState(() => _selectedSort = opt);
                    },
                    items: MarketplaceSortOption.values
                        .map((opt) => DropdownMenuItem(
                              value: opt,
                              child: Text('Sort: ${opt.label}',
                                  style: const TextStyle(fontSize: 11)),
                            ))
                        .toList(),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),

        // 3. Featured Spotlight Carousel (compact & bordered)
        if (_searchQuery.isEmpty && _selectedCategory == 'All') ...[
          _buildHomeSectionHeader(
            cs: cs,
            icon: Icons.star,
            iconColor: cs.tertiary,
            title: 'Featured Applications',
            subtitle: 'Official flagship releases hosted on GitHub Releases',
          ),
          const SizedBox(height: 8),
          SizedBox(
            height: 176,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: featuredApps.length,
              separatorBuilder: (_, __) => const SizedBox(width: 10),
              itemBuilder: (context, index) {
                final app = featuredApps[index];
                return SizedBox(
                  width: 274,
                  child: Card(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(18),
                      side: BorderSide(
                          color: cs.outline.withValues(alpha: 0.28)),
                    ),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(18),
                      onTap: () => _openAppDetail(app),
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                _buildHomeAppIcon(app, cs,
                                    size: 48, radius: 12),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Flexible(
                                            child: Text(
                                              app.appName,
                                              style: const TextStyle(
                                                  fontWeight: FontWeight.bold,
                                                  fontSize: 14.5),
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ),
                                          if (app.editorsChoice) ...[
                                            const SizedBox(width: 4),
                                            Icon(Icons.verified,
                                                size: 14, color: cs.primary),
                                          ],
                                        ],
                                      ),
                                      const SizedBox(height: 2),
                                      _buildBorderedBadge(
                                        icon: _webCategoryIcon(app.category),
                                        label: app.category,
                                        bgColor: cs.primaryContainer,
                                        fgColor: cs.onPrimaryContainer,
                                        borderColor:
                                            cs.primary.withValues(alpha: 0.25),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Expanded(
                              child: Text(
                                app.shortDescription,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  fontSize: 11.5,
                                  color: cs.onSurfaceVariant,
                                ),
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 8, vertical: 5),
                              decoration: BoxDecoration(
                                color: cs.surfaceContainerHighest
                                    .withValues(alpha: 0.35),
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(
                                    color: cs.outline.withValues(alpha: 0.22)),
                              ),
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Row(
                                    children: [
                                      Icon(Icons.new_releases_outlined,
                                          size: 12, color: cs.primary),
                                      const SizedBox(width: 3),
                                      Text('v${app.versionName}',
                                          style: const TextStyle(
                                              fontSize: 10.5,
                                              fontWeight: FontWeight.w600)),
                                      const SizedBox(width: 6),
                                      Icon(Icons.sd_storage_outlined,
                                          size: 12, color: cs.secondary),
                                      const SizedBox(width: 3),
                                      Text(app.apkFileSize,
                                          style: const TextStyle(
                                              fontSize: 10.5,
                                              fontWeight: FontWeight.w600)),
                                    ],
                                  ),
                                  InkWell(
                                    borderRadius: BorderRadius.circular(8),
                                    onTap: () => _handleDownload(app),
                                    child: Container(
                                      padding: const EdgeInsets.symmetric(
                                          horizontal: 8, vertical: 4),
                                      decoration: BoxDecoration(
                                        color:
                                            cs.primary.withValues(alpha: 0.14),
                                        borderRadius: BorderRadius.circular(8),
                                        border: Border.all(
                                            color: cs.primary
                                                .withValues(alpha: 0.35)),
                                      ),
                                      child: Row(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          Icon(Icons.cloud_download,
                                              size: 12, color: cs.primary),
                                          const SizedBox(width: 4),
                                          Text(
                                            'Get APK',
                                            style: TextStyle(
                                              fontSize: 10.5,
                                              fontWeight: FontWeight.w600,
                                              color: cs.primary,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 14),

          // 4. Editor's Choice Section (compact & bordered)
          _buildHomeSectionHeader(
            cs: cs,
            icon: Icons.verified,
            iconColor: cs.primary,
            title: "Editor's Choice & Essential Releases",
            subtitle: 'Verified architecture, clean Material 3 UI & zero ads',
          ),
          const SizedBox(height: 8),
          SizedBox(
            height: 136,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: editorsChoiceApps.length,
              separatorBuilder: (_, __) => const SizedBox(width: 10),
              itemBuilder: (context, index) {
                final app = editorsChoiceApps[index];
                return SizedBox(
                  width: 236,
                  child: Card(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                      side: BorderSide(
                          color: cs.outline.withValues(alpha: 0.26)),
                    ),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(16),
                      onTap: () => _openAppDetail(app),
                      child: Padding(
                        padding: const EdgeInsets.all(11),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Row(
                              children: [
                                _buildHomeAppIcon(app, cs,
                                    size: 34, radius: 9),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    app.appName,
                                    style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                        fontSize: 13.5),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                              ],
                            ),
                            Text(
                              app.shortDescription,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontSize: 11.5,
                                color: cs.onSurfaceVariant,
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 7, vertical: 3),
                              decoration: BoxDecoration(
                                color: cs.surfaceContainerHighest
                                    .withValues(alpha: 0.40),
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(
                                    color: cs.outline.withValues(alpha: 0.20)),
                              ),
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    'v${app.versionName}',
                                    style: TextStyle(
                                        fontSize: 10.5,
                                        fontWeight: FontWeight.w600,
                                        color: cs.primary),
                                  ),
                                  Text(
                                    '${app.downloadCount} DLs • ${app.apkFileSize}',
                                    style: TextStyle(
                                        fontSize: 10.5,
                                        color: cs.onSurfaceVariant),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 14),
        ],

        // 5. All Catalog / Filtered Applications List (compact bordered header + row cards)
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          decoration: BoxDecoration(
            color: cs.surfaceContainerHighest.withValues(alpha: 0.30),
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: cs.outline.withValues(alpha: 0.24)),
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: cs.primaryContainer,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: cs.primary.withValues(alpha: 0.25)),
                ),
                child:
                    Icon(Icons.apps, size: 16, color: cs.onPrimaryContainer),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _searchQuery.isEmpty && _selectedCategory == 'All'
                          ? 'All Zayaan Applications (${filteredApps.length})'
                          : 'Filtered Results (${filteredApps.length})',
                      style: const TextStyle(
                          fontSize: 15, fontWeight: FontWeight.bold),
                    ),
                    Text(
                      '$_selectedSubcategory • ${_selectedSort.label}',
                      style:
                          TextStyle(fontSize: 11, color: cs.onSurfaceVariant),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 10),
        ...(filteredApps.isEmpty ? trendingApps : filteredApps).map(
          (app) => Card(
            margin: const EdgeInsets.only(bottom: 10),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
              side: BorderSide(color: cs.outline.withValues(alpha: 0.26)),
            ),
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () => _openAppDetail(app),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    _buildHomeAppIcon(app, cs, size: 50, radius: 13),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Flexible(
                                child: Text(
                                  app.appName,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 14.5),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              if (app.editorsChoice) ...[
                                const SizedBox(width: 5),
                                Icon(Icons.verified,
                                    size: 14, color: cs.primary),
                              ],
                              if (app.offlineCapable) ...[
                                const SizedBox(width: 4),
                                Icon(Icons.offline_bolt,
                                    size: 14, color: cs.tertiary),
                              ],
                            ],
                          ),
                          const SizedBox(height: 1),
                          Text(
                            '${app.developerName} • ${app.category}',
                            style: TextStyle(
                                fontSize: 11.5, color: cs.onSurfaceVariant),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: 4),
                          Wrap(
                            spacing: 8,
                            runSpacing: 2,
                            crossAxisAlignment: WrapCrossAlignment.center,
                            children: [
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.new_releases_outlined,
                                      size: 11.5, color: cs.primary),
                                  const SizedBox(width: 3),
                                  Text('v${app.versionName}',
                                      style: const TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.w600)),
                                ],
                              ),
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.sd_storage_outlined,
                                      size: 11.5, color: cs.secondary),
                                  const SizedBox(width: 3),
                                  Text(app.apkFileSize,
                                      style: const TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.w600)),
                                ],
                              ),
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.download_outlined,
                                      size: 11.5, color: cs.onSurfaceVariant),
                                  const SizedBox(width: 2),
                                  Text('${app.downloadCount}',
                                      style: TextStyle(
                                          fontSize: 11,
                                          color: cs.onSurfaceVariant)),
                                ],
                              ),
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.star,
                                      size: 11.5, color: cs.tertiary),
                                  const SizedBox(width: 2),
                                  Text(app.averageRating.toStringAsFixed(1),
                                      style: const TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.w600)),
                                ],
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 6),
                    IconButton(
                      tooltip: 'Compare App',
                      visualDensity: VisualDensity.compact,
                      icon: Icon(
                        Icons.compare_arrows,
                        size: 20,
                        color: _comparisonIds.contains(app.appId)
                            ? cs.primary
                            : cs.onSurfaceVariant,
                      ),
                      onPressed: () => _toggleCompare(app.appId),
                    ),
                    FilledButton.tonalIcon(
                      onPressed: () => _openAppDetail(app),
                      icon: const Icon(Icons.download, size: 15),
                      label: Text(
                        PlatformApkInstaller.isAndroidPlatform
                            ? 'Install'
                            : 'Get APK',
                        style: const TextStyle(fontSize: 12),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildMyAppsSectionHeader({
    required ColorScheme cs,
    required IconData icon,
    required Color iconTint,
    required String title,
    required int count,
    Widget? trailing,
  }) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(5),
          decoration: BoxDecoration(
            color: iconTint.withValues(alpha: 0.14),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: iconTint.withValues(alpha: 0.28)),
          ),
          child: Icon(icon, size: 15, color: iconTint),
        ),
        const SizedBox(width: 8),
        Flexible(
          child: Text(
            title,
            style: const TextStyle(fontSize: 14.5, fontWeight: FontWeight.bold),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ),
        const SizedBox(width: 6),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
          decoration: BoxDecoration(
            color: cs.surfaceContainerHighest.withValues(alpha: 0.55),
            borderRadius: BorderRadius.circular(7),
            border: Border.all(color: cs.outline.withValues(alpha: 0.22)),
          ),
          child: Text(
            '$count',
            style: TextStyle(
              fontSize: 10.5,
              fontWeight: FontWeight.w600,
              color: cs.onSurfaceVariant,
            ),
          ),
        ),
        if (trailing != null) ...[
          const Spacer(),
          trailing,
        ],
      ],
    );
  }

  Widget _buildMyAppsEmptyCard({
    required ColorScheme cs,
    required IconData icon,
    required Color iconTint,
    required String title,
    required String subtitle,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: cs.outline.withValues(alpha: 0.24)),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              color: iconTint.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(9),
              border: Border.all(color: iconTint.withValues(alpha: 0.26)),
            ),
            child: Icon(icon, size: 17, color: iconTint),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                      fontSize: 12.5, fontWeight: FontWeight.w600),
                ),
                Text(
                  subtitle,
                  style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMyAppsCompactAppRow(StoreAppModel app, ColorScheme cs) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: cs.outline.withValues(alpha: 0.26)),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: () => _openAppDetail(app),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 9),
          child: Row(
            children: [
              _buildHomeAppIcon(app, cs, size: 42, radius: 11),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Flexible(
                          child: Text(
                            app.appName,
                            style: const TextStyle(
                                fontSize: 13.5, fontWeight: FontWeight.bold),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        if (app.editorsChoice) ...[
                          const SizedBox(width: 4),
                          Icon(Icons.verified, size: 13, color: cs.primary),
                        ],
                      ],
                    ),
                    Text(
                      '${app.developerName} • ${app.category}',
                      style:
                          TextStyle(fontSize: 11, color: cs.onSurfaceVariant),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 2),
                    Row(
                      children: [
                        Icon(Icons.new_releases, size: 11, color: cs.primary),
                        const SizedBox(width: 3),
                        Text(
                          'v${app.versionName}',
                          style: TextStyle(
                            fontSize: 10.5,
                            fontWeight: FontWeight.w600,
                            color: cs.primary,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Icon(Icons.sd_storage, size: 11, color: cs.secondary),
                        const SizedBox(width: 3),
                        Text(
                          app.apkFileSize,
                          style: TextStyle(
                              fontSize: 10.5, color: cs.onSurfaceVariant),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, size: 20, color: cs.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMyAppsTab() {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;
    final isDark = theme.brightness == Brightness.dark;
    final comparedApps =
        _catalog.where((a) => _comparisonIds.contains(a.appId)).toList();
    final favoriteApps =
        _catalog.where((a) => _favoriteIds.contains(a.appId)).toList();
    final wishlistApps =
        _catalog.where((a) => _wishlistIds.contains(a.appId)).toList();
    final downloadedApps =
        _catalog.where((a) => _downloadHistoryIds.contains(a.appId)).toList();

    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        // Compact, Theme-Adaptive Library Header Card
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            gradient: LinearGradient(
              colors: isDark
                  ? const [Color(0xFF0C2235), Color(0xFF092A27)]
                  : const [Color(0xFFEAF8F1), Color(0xFFF1F7FC)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            border: Border.all(
              color: cs.primary.withValues(alpha: isDark ? 0.34 : 0.24),
            ),
          ),
          child: Column(
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(7),
                    decoration: BoxDecoration(
                      color: cs.primaryContainer,
                      borderRadius: BorderRadius.circular(10),
                      border:
                          Border.all(color: cs.primary.withValues(alpha: 0.28)),
                    ),
                    child: Icon(Icons.inventory_2,
                        size: 18, color: cs.onPrimaryContainer),
                  ),
                  const SizedBox(width: 9),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Library, Downloads & Updates',
                          style: TextStyle(
                              fontSize: 16, fontWeight: FontWeight.w800),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          'Updates, downloads, comparison & saved apps',
                          style: TextStyle(
                              fontSize: 11, color: cs.onSurfaceVariant),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 6),
                  OutlinedButton.icon(
                    onPressed: () {
                      setState(() {
                        _statusBanner =
                            'Scanned catalog packages and checked GitHub Releases for latest v1.0.0.X updates.';
                      });
                    },
                    style: OutlinedButton.styleFrom(
                      visualDensity: VisualDensity.compact,
                      padding: const EdgeInsets.symmetric(
                          horizontal: 10, vertical: 6),
                      textStyle: const TextStyle(
                          fontSize: 11.5, fontWeight: FontWeight.w600),
                      side:
                          BorderSide(color: cs.primary.withValues(alpha: 0.35)),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(9)),
                    ),
                    icon: const Icon(Icons.refresh, size: 14),
                    label: const Text('Scan'),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(
                  color: cs.surface.withValues(alpha: isDark ? 0.62 : 0.88),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: cs.outline.withValues(alpha: 0.22)),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.apps, size: 12, color: cs.primary),
                        const SizedBox(width: 4),
                        Text('${_catalog.length} Apps',
                            style: const TextStyle(
                                fontSize: 10.5, fontWeight: FontWeight.w600)),
                      ],
                    ),
                    Container(
                        width: 1,
                        height: 11,
                        color: cs.outline.withValues(alpha: 0.22)),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.favorite, size: 12, color: cs.secondary),
                        const SizedBox(width: 4),
                        Text('${favoriteApps.length} Favs',
                            style: const TextStyle(
                                fontSize: 10.5, fontWeight: FontWeight.w600)),
                      ],
                    ),
                    Container(
                        width: 1,
                        height: 11,
                        color: cs.outline.withValues(alpha: 0.22)),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.bookmark, size: 12, color: cs.tertiary),
                        const SizedBox(width: 4),
                        Text('${wishlistApps.length} Saved',
                            style: const TextStyle(
                                fontSize: 10.5, fontWeight: FontWeight.w600)),
                      ],
                    ),
                    Container(
                        width: 1,
                        height: 11,
                        color: cs.outline.withValues(alpha: 0.22)),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.history, size: 12, color: cs.primary),
                        const SizedBox(width: 4),
                        Text('${downloadedApps.length} DLs',
                            style: const TextStyle(
                                fontSize: 10.5, fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 10),

        // Section 1: Available Updates & Release Actions
        _buildMyAppsSectionHeader(
          cs: cs,
          icon: Icons.system_update,
          iconTint: cs.tertiary,
          title: 'Available Updates',
          count: 0,
        ),
        const SizedBox(height: 8),
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: cs.surface,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: cs.primary.withValues(alpha: 0.26)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(6),
                    decoration: BoxDecoration(
                      color: cs.primary.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(9),
                      border:
                          Border.all(color: cs.primary.withValues(alpha: 0.26)),
                    ),
                    child:
                        Icon(Icons.check_circle, size: 17, color: cs.primary),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'All Apps Up to Date',
                          style: TextStyle(
                              fontSize: 12.5, fontWeight: FontWeight.w600),
                        ),
                        Text(
                          'All Zayaan Apps packages are synced with the latest GitHub Releases.',
                          style: TextStyle(
                              fontSize: 11, color: cs.onSurfaceVariant),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              FilledButton.tonalIcon(
                style: FilledButton.styleFrom(
                  visualDensity: VisualDensity.compact,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  textStyle: const TextStyle(
                      fontSize: 11.5, fontWeight: FontWeight.w600),
                ),
                onPressed: () => _handleDownload(_catalog.first),
                icon: const Icon(Icons.system_update, size: 15),
                label: const Text('Download Latest Store APK'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),

        // Section 2: Side-by-Side Comparison
        _buildMyAppsSectionHeader(
          cs: cs,
          icon: Icons.compare_arrows,
          iconTint: cs.primary,
          title: 'Side-by-Side Comparison',
          count: comparedApps.length,
        ),
        const SizedBox(height: 8),
        if (comparedApps.isEmpty)
          _buildMyAppsEmptyCard(
            cs: cs,
            icon: Icons.compare_arrows,
            iconTint: cs.primary,
            title: 'No Apps Selected for Comparison',
            subtitle:
                'Tap Compare on any two or more apps to compare versions, sizes, and SDK specs.',
          )
        else
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: comparedApps
                  .map(
                    (app) => Container(
                      width: 214,
                      margin: const EdgeInsets.only(right: 10),
                      padding: const EdgeInsets.all(11),
                      decoration: BoxDecoration(
                        color: cs.surface,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(
                            color: cs.outline.withValues(alpha: 0.26)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          InkWell(
                            onTap: () => _openAppDetail(app),
                            child: Row(
                              children: [
                                _buildHomeAppIcon(app, cs,
                                    size: 36, radius: 10),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        app.appName,
                                        style: const TextStyle(
                                            fontSize: 12.5,
                                            fontWeight: FontWeight.bold),
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                      Text(
                                        app.category,
                                        style: TextStyle(
                                            fontSize: 10.5, color: cs.primary),
                                        maxLines: 1,
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const Divider(height: 14),
                          Text('Version: v${app.versionName} (${app.versionCode})',
                              style: const TextStyle(fontSize: 11)),
                          Text('APK Size: ${app.apkFileSize}',
                              style: const TextStyle(fontSize: 11)),
                          Text(
                              'Rating: ★ ${app.averageRating.toStringAsFixed(1)}',
                              style: const TextStyle(fontSize: 11)),
                          Text('Downloads: ${app.downloadCount}',
                              style: const TextStyle(fontSize: 11)),
                          Text(
                              'Android: API ${app.minAndroidSdk}+ → ${app.targetAndroidSdk}',
                              style: const TextStyle(fontSize: 11)),
                          const SizedBox(height: 8),
                          SizedBox(
                            width: double.infinity,
                            height: 30,
                            child: OutlinedButton(
                              onPressed: () => _toggleCompare(app.appId),
                              style: OutlinedButton.styleFrom(
                                padding:
                                    const EdgeInsets.symmetric(horizontal: 8),
                                shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(8)),
                              ),
                              child: const Text('Remove',
                                  style: TextStyle(fontSize: 11)),
                            ),
                          ),
                        ],
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
        const SizedBox(height: 12),

        // Section 3: Favorite Apps
        _buildMyAppsSectionHeader(
          cs: cs,
          icon: Icons.favorite,
          iconTint: cs.secondary,
          title: 'Favorite Apps',
          count: favoriteApps.length,
        ),
        const SizedBox(height: 8),
        if (favoriteApps.isEmpty)
          _buildMyAppsEmptyCard(
            cs: cs,
            icon: Icons.favorite_border,
            iconTint: cs.secondary,
            title: 'No Favorites Yet',
            subtitle:
                'Tap the heart icon on any app details screen to sync favorites.',
          )
        else
          ...favoriteApps.map((a) => _buildMyAppsCompactAppRow(a, cs)),
        const SizedBox(height: 12),

        // Section 4: Wishlist / Saved Apps
        _buildMyAppsSectionHeader(
          cs: cs,
          icon: Icons.bookmark,
          iconTint: cs.tertiary,
          title: 'Wishlist / Saved Apps',
          count: wishlistApps.length,
        ),
        const SizedBox(height: 8),
        if (wishlistApps.isEmpty)
          _buildMyAppsEmptyCard(
            cs: cs,
            icon: Icons.bookmark_border,
            iconTint: cs.tertiary,
            title: 'Wishlist Empty',
            subtitle:
                'Tap the bookmark icon on any app details page to save it for later.',
          )
        else
          ...wishlistApps.map((a) => _buildMyAppsCompactAppRow(a, cs)),
        const SizedBox(height: 12),

        // Section 5: Previously Downloaded History
        _buildMyAppsSectionHeader(
          cs: cs,
          icon: Icons.history,
          iconTint: cs.primary,
          title: 'Previously Downloaded History',
          count: downloadedApps.length,
        ),
        const SizedBox(height: 8),
        if (downloadedApps.isEmpty)
          _buildMyAppsEmptyCard(
            cs: cs,
            icon: Icons.history,
            iconTint: cs.onSurfaceVariant,
            title: 'No Download History',
            subtitle:
                'Apps you download are automatically synced to your cloud library.',
          )
        else
          ...downloadedApps.map((a) => _buildMyAppsCompactAppRow(a, cs)),
      ],
    );
  }

  Widget _buildAdminTab() {
    final totalDownloads =
        _catalog.fold<int>(0, (sum, a) => sum + a.downloadCount);
    final totalViews = _catalog.fold<int>(0, (sum, a) => sum + a.pageViews);
    final publishedCount = _catalog.where((a) => a.published).length;
    final featuredCount = _catalog.where((a) => a.featured).length;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(24)),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.admin_panel_settings,
                        color: Color(0xFF6EDBA9), size: 28),
                    SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Zayaan Apps Admin & Marketplace Console',
                            style: TextStyle(
                                fontSize: 20, fontWeight: FontWeight.bold),
                          ),
                          Text(
                            'zayaanapps@gmail.com • Trusted Admin (RBAC Verified)',
                            style: TextStyle(fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    Chip(label: Text('APPS: ${_catalog.length}')),
                    Chip(label: Text('LIVE: $publishedCount')),
                    Chip(label: Text('FEATURED: $featuredCount')),
                    Chip(label: Text('DOWNLOADS: $totalDownloads')),
                    Chip(label: Text('VIEWS: $totalViews')),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          'Managed Store Catalog',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        ..._catalog.map(
          (app) => Card(
            margin: const EdgeInsets.only(bottom: 10),
            shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(18)),
            child: ListTile(
              title: Text(
                '${app.appName} (v${app.versionName} • code ${app.versionCode})',
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              subtitle: Text(
                '${app.packageName} • ${app.category} • ${app.apkFileSize} • ${app.downloadCount} DLs • ${app.pageViews} views',
              ),
              trailing: FilledButton.tonal(
                onPressed: () => _openAppDetail(app),
                child: const Text('Inspect'),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildWebDevOpsTab() {
    final cs = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
            side: BorderSide(color: cs.outline.withValues(alpha: 0.28)),
          ),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.public, color: cs.primary, size: 28),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Zayaan Apps Web & GitHub DevOps Hub',
                            style: TextStyle(
                                fontSize: 20, fontWeight: FontWeight.bold),
                          ),
                          InkWell(
                            onTap: () => _launchExternalUrl(
                                'https://zayaanapps.github.io/zayaan-apps/'),
                            child: Text(
                              'https://zayaanapps.github.io/zayaan-apps/',
                              style: TextStyle(
                                fontSize: 12,
                                color: cs.primary,
                                decoration: TextDecoration.underline,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                const Text(
                  'Both your Native Android APK (com.zayaanapps.store) and responsive Web store share the exact same Cloud Firestore catalog and GitHub Releases distribution pipeline.',
                ),
                const SizedBox(height: 14),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    FilledButton.icon(
                      onPressed: () => _handleDownload(_catalog.first),
                      icon: const Icon(Icons.rocket_launch, size: 18),
                      label: const Text('Latest Release APK'),
                    ),
                    OutlinedButton.icon(
                      onPressed: _cycleThemeMode,
                      icon: Icon(
                        switch (widget.themeMode) {
                          ThemeMode.system => Icons.brightness_auto,
                          ThemeMode.light => Icons.light_mode,
                          ThemeMode.dark => Icons.dark_mode,
                        },
                        size: 18,
                      ),
                      label: Text(
                          'Theme: ${_themeModeLabel(widget.themeMode)}'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
            side: BorderSide(color: cs.outline.withValues(alpha: 0.28)),
          ),
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Store Settings & Preferences',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
                Text(
                  'App Theme',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                ),
                const SizedBox(height: 4),
                Text(
                  switch (widget.themeMode) {
                    ThemeMode.system =>
                      'Following device / browser system appearance',
                    ThemeMode.light => 'Always use Light theme',
                    ThemeMode.dark => 'Always use Dark theme',
                  },
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: cs.onSurfaceVariant,
                      ),
                ),
                const SizedBox(height: 10),
                SizedBox(
                  width: double.infinity,
                  child: SegmentedButton<ThemeMode>(
                    segments: const [
                      ButtonSegment<ThemeMode>(
                        value: ThemeMode.system,
                        label: Text('System'),
                        icon: Icon(Icons.brightness_auto, size: 18),
                      ),
                      ButtonSegment<ThemeMode>(
                        value: ThemeMode.light,
                        label: Text('Light'),
                        icon: Icon(Icons.light_mode, size: 18),
                      ),
                      ButtonSegment<ThemeMode>(
                        value: ThemeMode.dark,
                        label: Text('Dark'),
                        icon: Icon(Icons.dark_mode, size: 18),
                      ),
                    ],
                    selected: <ThemeMode>{widget.themeMode},
                    onSelectionChanged: (selected) {
                      if (selected.isNotEmpty) {
                        widget.onThemeModeChanged(selected.first);
                      }
                    },
                  ),
                ),
                const SizedBox(height: 8),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Available Update Notifications'),
                  subtitle: const Text(
                      'Notify when installed Zayaan apps have newer versionCode'),
                  value: _notifyUpdates,
                  onChanged: (v) => setState(() => _notifyUpdates = v),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildDetailView(StoreAppModel app) {
    final cs = Theme.of(context).colorScheme;
    final isFav = _favoriteIds.contains(app.appId);
    final isWish = _wishlistIds.contains(app.appId);
    final isCompared = _comparisonIds.contains(app.appId);
    final appReviews = _reviewsByApp[app.appId] ?? const <AppReviewModel>[];
    final currentUid = _currentUserId;
    AppReviewModel? existingUserReview;
    for (final r in appReviews) {
      if (r.userId == currentUid) {
        existingUserReview = r;
        break;
      }
    }
    final similarApps = _catalog
        .where((a) => a.category == app.category && a.appId != app.appId)
        .toList();

    final reviewCountDisplay =
        appReviews.length > app.reviewCount ? appReviews.length : app.reviewCount;
    final avgRatingDisplay = appReviews.isNotEmpty
        ? appReviews.map((r) => r.rating).reduce((a, b) => a + b) /
            appReviews.length
        : app.averageRating;
    final ratingSummaryText = reviewCountDisplay > 0
        ? '${avgRatingDisplay.toStringAsFixed(1)} ($reviewCountDisplay)'
        : 'Verified';

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // 1. Top Navigation Row
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            TextButton.icon(
              onPressed: () {
                _reviewsSubscription?.cancel();
                setState(() => _selectedApp = null);
              },
              icon: const Icon(Icons.arrow_back),
              label: const Text('Back to Store'),
            ),
            Wrap(
              spacing: 4,
              children: [
                IconButton(
                  tooltip: 'Add to Comparison',
                  icon: Icon(
                    Icons.compare_arrows,
                    color: isCompared ? cs.primary : null,
                  ),
                  onPressed: () => _toggleCompare(app.appId),
                ),
                IconButton(
                  tooltip: 'Favorite',
                  icon: Icon(
                    isFav ? Icons.favorite : Icons.favorite_border,
                    color: isFav ? cs.error : null,
                  ),
                  onPressed: () => setState(() {
                    if (isFav) {
                      _favoriteIds.remove(app.appId);
                    } else {
                      _favoriteIds.add(app.appId);
                    }
                  }),
                ),
                IconButton(
                  tooltip: 'Wishlist',
                  icon: Icon(
                    isWish ? Icons.bookmark : Icons.bookmark_border,
                    color: isWish ? cs.primary : null,
                  ),
                  onPressed: () => setState(() {
                    if (isWish) {
                      _wishlistIds.remove(app.appId);
                    } else {
                      _wishlistIds.add(app.appId);
                    }
                  }),
                ),
              ],
            ),
          ],
        ),
        const SizedBox(height: 8),

        // 2. Compact Bordered App Identity Header + Single-Line Telemetry Card
        Card(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
            side: BorderSide(color: cs.outline.withValues(alpha: 0.28)),
          ),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Container(
                      width: 62,
                      height: 62,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(16),
                        color: cs.primaryContainer,
                        border: Border.all(
                          color: cs.outline.withValues(alpha: 0.28),
                        ),
                      ),
                      clipBehavior: Clip.antiAlias,
                      child: app.iconUrl.isNotEmpty
                          ? Image.network(
                              app.iconUrl,
                              fit: BoxFit.cover,
                              errorBuilder: (_, __, ___) => Center(
                                child: Text(
                                  app.appName.substring(0, 1),
                                  style: TextStyle(
                                    fontSize: 24,
                                    fontWeight: FontWeight.bold,
                                    color: cs.onPrimaryContainer,
                                  ),
                                ),
                              ),
                            )
                          : Center(
                              child: Text(
                                app.appName.substring(0, 1),
                                style: TextStyle(
                                  fontSize: 24,
                                  fontWeight: FontWeight.bold,
                                  color: cs.onPrimaryContainer,
                                ),
                              ),
                            ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Flexible(
                                child: Text(
                                  app.appName,
                                  style: const TextStyle(
                                    fontSize: 20,
                                    fontWeight: FontWeight.bold,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              if (app.editorsChoice) ...[
                                const SizedBox(width: 5),
                                Icon(Icons.verified,
                                    size: 18, color: cs.primary),
                              ],
                            ],
                          ),
                          const SizedBox(height: 2),
                          Row(
                            children: [
                              Icon(Icons.storefront,
                                  size: 13, color: cs.primary),
                              const SizedBox(width: 4),
                              Flexible(
                                child: Text(
                                  app.developerName,
                                  style: TextStyle(
                                    fontSize: 13,
                                    fontWeight: FontWeight.w600,
                                    color: cs.primary,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 2),
                          Row(
                            children: [
                              Icon(Icons.inventory_2_outlined,
                                  size: 12, color: cs.onSurfaceVariant),
                              const SizedBox(width: 4),
                              Flexible(
                                child: Text(
                                  app.packageName,
                                  style: TextStyle(
                                    fontSize: 11,
                                    fontFamily: 'monospace',
                                    color: cs.onSurfaceVariant,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Wrap(
                            spacing: 6,
                            runSpacing: 4,
                            children: [
                              _buildBorderedBadge(
                                icon: Icons.category_outlined,
                                label: app.category,
                                bgColor: cs.primaryContainer,
                                fgColor: cs.onPrimaryContainer,
                                borderColor: cs.primary.withValues(alpha: 0.28),
                              ),
                              if (app.editorsChoice)
                                _buildBorderedBadge(
                                  icon: Icons.workspace_premium_outlined,
                                  label: "Editor's Choice",
                                  bgColor: cs.tertiaryContainer,
                                  fgColor: cs.onTertiaryContainer,
                                  borderColor:
                                      cs.tertiary.withValues(alpha: 0.28),
                                ),
                              if (app.offlineCapable)
                                _buildBorderedBadge(
                                  icon: Icons.offline_bolt_outlined,
                                  label: 'Offline Ready',
                                  bgColor: cs.secondaryContainer,
                                  fgColor: cs.onSecondaryContainer,
                                  borderColor:
                                      cs.secondary.withValues(alpha: 0.28),
                                ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                if (app.shortDescription.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Text(
                    app.shortDescription,
                    style: TextStyle(
                      fontSize: 12.5,
                      color: cs.onSurfaceVariant,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
                const SizedBox(height: 12),

                // Compact Single-Line 4-Metric Telemetry Bar
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                  decoration: BoxDecoration(
                    color: cs.surfaceContainerHighest.withValues(alpha: 0.42),
                    borderRadius: BorderRadius.circular(12),
                    border:
                        Border.all(color: cs.outline.withValues(alpha: 0.28)),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      _buildCompactTelemetryItem(
                        icon: Icons.new_releases_outlined,
                        value: 'v${app.versionName}',
                        color: cs.primary,
                      ),
                      _buildVerticalTelemetryDivider(cs),
                      _buildCompactTelemetryItem(
                        icon: Icons.sd_storage_outlined,
                        value: app.apkFileSize,
                        color: cs.secondary,
                      ),
                      _buildVerticalTelemetryDivider(cs),
                      _buildCompactTelemetryItem(
                        icon: Icons.download_outlined,
                        value: '${app.downloadCount}',
                        color: cs.tertiary,
                      ),
                      _buildVerticalTelemetryDivider(cs),
                      _buildCompactTelemetryItem(
                        icon: Icons.star,
                        value: ratingSummaryText,
                        color: cs.primary,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: () => _handleDownload(app),
                    icon: const Icon(Icons.cloud_download, size: 18),
                    label: Text(
                      PlatformApkInstaller.isAndroidPlatform
                          ? 'Install (v${app.versionName})'
                          : 'Download APK (v${app.versionName})',
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),

        // 3. What's New Compact Bordered Card (Formatted Markdown + Clickable Links)
        if (app.releaseNotes.trim().isNotEmpty) ...[
          _buildWhatsNewCompactCard(app),
          const SizedBox(height: 12),
        ],

        // 4. About This Application Compact Bordered Card
        _buildAboutCompactCard(app),
        const SizedBox(height: 12),

        // 5. Ratings & Community Reviews (Summary + Edit Dialog)
        _buildRatingsAndReviewsCard(
          app: app,
          reviews: appReviews,
          existingUserReview: existingUserReview,
        ),
        const SizedBox(height: 12),

        // 6. Package & Compatibility Specifications Card
        _buildPackageSpecsCard(app),
        const SizedBox(height: 12),

        // 7. Developer & Distribution Info Card
        _buildDeveloperInfoCard(app),

        if (similarApps.isNotEmpty) ...[
          const Divider(height: 32),
          const Text('Similar Applications',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ...similarApps.map(
            (s) => Card(
              margin: const EdgeInsets.only(bottom: 8),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: BorderSide(color: cs.outline.withValues(alpha: 0.24)),
              ),
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: cs.primaryContainer,
                  child: Text(s.appName.substring(0, 1)),
                ),
                title: Text(s.appName,
                    style: const TextStyle(fontWeight: FontWeight.w600)),
                subtitle: Text('v${s.versionName} • ${s.apkFileSize}'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => _openAppDetail(s),
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildBorderedBadge({
    required IconData icon,
    required String label,
    required Color bgColor,
    required Color fgColor,
    required Color borderColor,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(7),
        border: Border.all(color: borderColor),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 11, color: fgColor),
          const SizedBox(width: 3),
          Text(
            label,
            style: TextStyle(
              fontSize: 10.5,
              fontWeight: FontWeight.w600,
              color: fgColor,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCompactTelemetryItem({
    required IconData icon,
    required String value,
    required Color color,
  }) {
    return Flexible(
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: color),
          const SizedBox(width: 3),
          Flexible(
            child: Text(
              value,
              style: const TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildVerticalTelemetryDivider(ColorScheme cs) {
    return Container(
      width: 1,
      height: 14,
      margin: const EdgeInsets.symmetric(horizontal: 4),
      color: cs.outline.withValues(alpha: 0.25),
    );
  }

  List<_ExtractedLink> _extractLinksFromText(String rawText) {
    final regex = RegExp(
        r'\[([^\]]+)\]\((https?://[^\s)]+)\)|(https?://[^\s<>)\]]+)');
    final links = <_ExtractedLink>[];
    final seen = <String>{};
    for (final rawLine in rawText.split('\n')) {
      final line = rawLine.trim();
      if (line.isEmpty) continue;
      for (final m in regex.allMatches(line)) {
        final mdLabel = (m.group(1) ?? '').trim();
        final mdUrl = (m.group(2) ?? '').trim();
        final bareUrl = (m.group(3) ?? '')
            .replaceAll(RegExp(r'[.,;)]+$'), '');
        final url = mdUrl.isNotEmpty ? mdUrl : bareUrl;
        if (url.isEmpty || !seen.add(url)) continue;
        String label = mdLabel;
        if (label.isEmpty) {
          final prefix = line
              .substring(0, m.start)
              .replaceAll(RegExp(r'^[\-*•#>\s]+'), '')
              .replaceAll('**', '')
              .replaceAll('`', '')
              .trim()
              .replaceAll(RegExp(r'[:\-\s]+$'), '');
          if (prefix.isNotEmpty) {
            label = prefix;
          } else if (url.toLowerCase().contains('/compare/')) {
            label = 'Full Changelog';
          } else if (url.toLowerCase().contains('github.io')) {
            label = 'Web Portal';
          } else if (url.toLowerCase().contains('github.com')) {
            label = 'GitHub';
          } else {
            label = 'Open Link';
          }
        }
        links.add(_ExtractedLink(label: label, url: url));
      }
    }
    return links;
  }

  TextSpan _buildRichMarkdownSpan(String rawText, ColorScheme cs) {
    final tokenRegex = RegExp(
        r'\[([^\]]+)\]\((https?://[^\s)]+)\)|https?://[^\s<>)\]]+|\*\*([^*]+)\*\*|`([^`]+)`');
    final children = <InlineSpan>[];
    final lines = rawText.split('\n');
    bool appendedAny = false;
    bool lastBlank = false;

    for (final rawLine in lines) {
      final trimmed = rawLine.trim();
      if (trimmed.isEmpty) {
        if (appendedAny && !lastBlank) {
          children.add(const TextSpan(text: '\n'));
          lastBlank = true;
        }
        continue;
      }
      if (appendedAny && !lastBlank) {
        children.add(const TextSpan(text: '\n'));
      }
      appendedAny = true;
      lastBlank = false;

      final isHeading = trimmed.startsWith('#');
      String normalized = trimmed;
      if (isHeading) {
        normalized = trimmed.replaceFirst(RegExp(r'^#+\s*'), '');
      } else if (trimmed.startsWith('- ')) {
        normalized = '• ${trimmed.substring(2).trim()}';
      } else if (trimmed.startsWith('* ')) {
        normalized = '• ${trimmed.substring(2).trim()}';
      }

      final baseStyle = TextStyle(
        fontSize: 12.5,
        height: 1.45,
        fontWeight: isHeading ? FontWeight.bold : FontWeight.normal,
        color: isHeading ? cs.primary : cs.onSurface,
      );

      int cursor = 0;
      for (final m in tokenRegex.allMatches(normalized)) {
        if (m.start > cursor) {
          children.add(TextSpan(
            text: normalized.substring(cursor, m.start),
            style: baseStyle,
          ));
        }
        final mdLabel = m.group(1) ?? '';
        final mdUrl = m.group(2) ?? '';
        final boldText = m.group(3) ?? '';
        final codeText = m.group(4) ?? '';
        if (mdUrl.isNotEmpty) {
          final recognizer = TapGestureRecognizer()
            ..onTap = () => _launchExternalUrl(mdUrl);
          _linkRecognizers.add(recognizer);
          children.add(TextSpan(
            text: mdLabel.isNotEmpty ? mdLabel : mdUrl,
            style: baseStyle.copyWith(
              color: cs.secondary,
              fontWeight: FontWeight.w600,
              decoration: TextDecoration.underline,
            ),
            recognizer: recognizer,
          ));
        } else if (boldText.isNotEmpty) {
          children.add(TextSpan(
            text: boldText,
            style: baseStyle.copyWith(fontWeight: FontWeight.w600),
          ));
        } else if (codeText.isNotEmpty) {
          children.add(TextSpan(
            text: codeText,
            style: baseStyle.copyWith(
              fontFamily: 'monospace',
              backgroundColor: cs.surfaceContainerHighest.withValues(alpha: 0.7),
            ),
          ));
        } else {
          final rawUrl = m.group(0) ?? '';
          final cleanUrl = rawUrl.replaceAll(RegExp(r'[.,;)]+$'), '');
          final trailing = rawUrl.substring(cleanUrl.length);
          final recognizer = TapGestureRecognizer()
            ..onTap = () => _launchExternalUrl(cleanUrl);
          _linkRecognizers.add(recognizer);
          children.add(TextSpan(
            text: cleanUrl,
            style: baseStyle.copyWith(
              color: cs.secondary,
              fontWeight: FontWeight.w500,
              decoration: TextDecoration.underline,
            ),
            recognizer: recognizer,
          ));
          if (trailing.isNotEmpty) {
            children.add(TextSpan(text: trailing, style: baseStyle));
          }
        }
        cursor = m.end;
      }
      if (cursor < normalized.length) {
        children.add(TextSpan(
          text: normalized.substring(cursor),
          style: baseStyle,
        ));
      }
    }
    return TextSpan(children: children);
  }

  String _sanitizeWhatsNewNotes(String rawNotes, String versionName) {
    final withoutDetails = rawNotes.replaceAll(
      RegExp(r'<details>[\s\S]*?</details>', caseSensitive: false),
      '',
    );
    const metadataPrefixes = [
      '### zayaan apps official store release',
      'package name:',
      'architecture:',
      'version name:',
      'version code:',
      'commit:',
      'apk sha-256 checksum:',
      'responsive web portal:',
      'direct apk assets:',
    ];
    final kept = <String>[];
    var compareLine = '';
    for (final rawLine in withoutDetails.split('\n')) {
      final trimmed = rawLine.trim();
      var normalized = trimmed
          .replaceAll('**', '')
          .replaceAll('`', '')
          .trim();
      if (normalized.startsWith('- ')) {
        normalized = normalized.substring(2).trim();
      } else if (normalized.startsWith('* ')) {
        normalized = normalized.substring(2).trim();
      }
      final lower = normalized.toLowerCase();
      if (lower.startsWith('full changelog:') ||
          trimmed.toLowerCase().contains('/compare/')) {
        compareLine = trimmed;
        continue;
      }
      final isMetadata = metadataPrefixes.any((p) => lower.startsWith(p));
      if (!isMetadata) {
        kept.add(rawLine);
      }
    }
    final cleanedBody = kept.join('\n').trim();
    final buffer = StringBuffer();
    if (cleanedBody.isNotEmpty) {
      buffer.write(cleanedBody);
    } else {
      buffer.writeln('### Latest Updates, Fixes & Changes (v$versionName)');
      buffer.write(
          '• Updated release v$versionName with latest fixes, performance & UI refinements.');
    }
    if (compareLine.isNotEmpty) {
      buffer.write('\n\n$compareLine');
    }
    return buffer.toString().trim();
  }

  Widget _buildWhatsNewCompactCard(StoreAppModel app) {
    final cs = Theme.of(context).colorScheme;
    final sanitizedNotes =
        _sanitizeWhatsNewNotes(app.releaseNotes, app.versionName);
    final extractedLinks = _extractLinksFromText(sanitizedNotes);
    final nonEmptyLines = sanitizedNotes
        .split('\n')
        .map((l) => l.trim())
        .where((l) => l.isNotEmpty)
        .toList();
    final isLong = nonEmptyLines.length > 6 || sanitizedNotes.length > 320;
    final displayedText = (_whatsNewExpanded || !isLong)
        ? sanitizedNotes
        : nonEmptyLines
            .where((l) {
              final lower = l.toLowerCase();
              return !((lower.contains('full changelog') ||
                      lower.contains('web portal')) &&
                  lower.contains('http'));
            })
            .take(5)
            .join('\n');

    return Card(
      color: cs.surfaceContainerHighest.withValues(alpha: 0.38),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: cs.outline.withValues(alpha: 0.28)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Row(
                    children: [
                      Icon(Icons.new_releases, size: 18, color: cs.primary),
                      const SizedBox(width: 6),
                      Flexible(
                        child: Text(
                          "What's New in v${app.versionName}",
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: cs.primary,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                _buildBorderedBadge(
                  icon: Icons.update,
                  label: 'Latest Release',
                  bgColor: cs.surface,
                  fgColor: cs.onSurfaceVariant,
                  borderColor: cs.outline.withValues(alpha: 0.24),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text.rich(_buildRichMarkdownSpan(displayedText, cs)),
            if (extractedLinks.isNotEmpty) ...[
              const SizedBox(height: 8),
              ...extractedLinks.map(
                (link) => Padding(
                  padding: const EdgeInsets.only(bottom: 5),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(10),
                    onTap: () => _launchExternalUrl(link.url),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 10, vertical: 7),
                      decoration: BoxDecoration(
                        color: cs.surface,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: cs.secondary.withValues(alpha: 0.32),
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.link, size: 14, color: cs.secondary),
                          const SizedBox(width: 6),
                          Text(
                            '${link.label}: ',
                            style: const TextStyle(
                              fontSize: 11.5,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          Expanded(
                            child: Text(
                              link.url,
                              style: TextStyle(
                                fontSize: 11.5,
                                color: cs.secondary,
                                decoration: TextDecoration.underline,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          const SizedBox(width: 6),
                          Icon(Icons.open_in_new,
                              size: 13, color: cs.secondary),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ],
            if (isLong)
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton.icon(
                  onPressed: () =>
                      setState(() => _whatsNewExpanded = !_whatsNewExpanded),
                  icon: Icon(
                    _whatsNewExpanded ? Icons.expand_less : Icons.expand_more,
                    size: 16,
                  ),
                  label: Text(_whatsNewExpanded ? 'Show less' : 'Show more'),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildAboutCompactCard(StoreAppModel app) {
    final cs = Theme.of(context).colorScheme;
    final introText =
        app.description.isNotEmpty ? app.description : app.shortDescription;
    final extractedLinks = _extractLinksFromText(introText);
    final isLong = introText.length > 260;
    final displayed = (_aboutExpanded || !isLong)
        ? introText
        : '${introText.substring(0, 260).trimRight()}...';

    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: cs.outline.withValues(alpha: 0.28)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(Icons.info_outline, size: 18, color: cs.primary),
                    const SizedBox(width: 6),
                    const Text(
                      'About This Application',
                      style:
                          TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                _buildBorderedBadge(
                  icon: Icons.new_releases_outlined,
                  label: 'v${app.versionName}',
                  bgColor: cs.primaryContainer.withValues(alpha: 0.65),
                  fgColor: cs.onPrimaryContainer,
                  borderColor: cs.primary.withValues(alpha: 0.25),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text.rich(_buildRichMarkdownSpan(displayed, cs)),
            if (extractedLinks.isNotEmpty) ...[
              const SizedBox(height: 8),
              ...extractedLinks.map(
                (link) => Padding(
                  padding: const EdgeInsets.only(bottom: 5),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(10),
                    onTap: () => _launchExternalUrl(link.url),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 10, vertical: 7),
                      decoration: BoxDecoration(
                        color: cs.surfaceContainerHighest.withValues(alpha: 0.3),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: cs.secondary.withValues(alpha: 0.32),
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.link, size: 14, color: cs.secondary),
                          const SizedBox(width: 6),
                          Text(
                            '${link.label}: ',
                            style: const TextStyle(
                              fontSize: 11.5,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          Expanded(
                            child: Text(
                              link.url,
                              style: TextStyle(
                                fontSize: 11.5,
                                color: cs.secondary,
                                decoration: TextDecoration.underline,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          Icon(Icons.open_in_new,
                              size: 13, color: cs.secondary),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ],
            if (isLong)
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton.icon(
                  onPressed: () =>
                      setState(() => _aboutExpanded = !_aboutExpanded),
                  icon: Icon(
                    _aboutExpanded ? Icons.expand_less : Icons.expand_more,
                    size: 16,
                  ),
                  label: Text(_aboutExpanded ? 'Show less' : 'Read more'),
                ),
              ),
            const SizedBox(height: 10),
            Container(
              decoration: BoxDecoration(
                color: cs.surfaceContainerHighest.withValues(alpha: 0.28),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: cs.outline.withValues(alpha: 0.22)),
              ),
              child: Column(
                children: [
                  _buildSpecRow(
                      cs, Icons.inventory_2_outlined, 'Package', app.packageName),
                  _buildSpecDivider(cs),
                  _buildSpecRow(cs, Icons.new_releases_outlined, 'Version',
                      'v${app.versionName} (Build ${app.versionCode})'),
                  _buildSpecDivider(cs),
                  _buildSpecRow(
                      cs, Icons.category_outlined, 'Category', app.category),
                  _buildSpecDivider(cs),
                  _buildSpecRow(
                      cs, Icons.sd_storage_outlined, 'APK Size', app.apkFileSize),
                  _buildSpecDivider(cs),
                  _buildSpecRow(cs, Icons.storefront_outlined, 'Developer',
                      app.developerName),
                  _buildSpecDivider(cs),
                  _buildSpecRow(cs, Icons.phone_android, 'Android OS',
                      'API ${app.minAndroidSdk}+ (Target ${app.targetAndroidSdk})'),
                  if (app.architecture.isNotEmpty) ...[
                    _buildSpecDivider(cs),
                    _buildSpecRow(
                        cs, Icons.memory, 'Architecture', app.architecture),
                  ],
                  if (app.sha256Checksum.isNotEmpty) ...[
                    _buildSpecDivider(cs),
                    _buildSpecRow(cs, Icons.fingerprint, 'SHA-256',
                        '${app.sha256Checksum.substring(0, app.sha256Checksum.length.clamp(0, 20))}...'),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRatingsAndReviewsCard({
    required StoreAppModel app,
    required List<AppReviewModel> reviews,
    required AppReviewModel? existingUserReview,
  }) {
    final cs = Theme.of(context).colorScheme;
    final totalCount =
        reviews.length > app.reviewCount ? reviews.length : app.reviewCount;
    final avgRating = reviews.isNotEmpty
        ? reviews.map((r) => r.rating).reduce((a, b) => a + b) / reviews.length
        : app.averageRating;

    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: cs.outline.withValues(alpha: 0.28)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.rate_review_outlined,
                              size: 18, color: cs.primary),
                          const SizedBox(width: 6),
                          const Text(
                            'Ratings & Reviews',
                            style: TextStyle(
                                fontSize: 15, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                      Text(
                        'Verified installs, helpful votes & cloud sync',
                        style: TextStyle(
                            fontSize: 11.5, color: cs.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                _buildBorderedBadge(
                  icon: Icons.star,
                  label: totalCount > 0
                      ? '${avgRating.toStringAsFixed(1)} ($totalCount)'
                      : '5.0 New',
                  bgColor: cs.primaryContainer,
                  fgColor: cs.onPrimaryContainer,
                  borderColor: cs.primary.withValues(alpha: 0.28),
                ),
              ],
            ),
            if (reviews.isNotEmpty) ...[
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: cs.surfaceContainerHighest.withValues(alpha: 0.28),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: cs.outline.withValues(alpha: 0.22)),
                ),
                child: Column(
                  children: [
                    for (int star = 5; star >= 1; star--)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 1.5),
                        child: Row(
                          children: [
                            SizedBox(
                              width: 28,
                              child: Text('$star ★',
                                  style: const TextStyle(fontSize: 11)),
                            ),
                            Expanded(
                              child: ClipRRect(
                                borderRadius: BorderRadius.circular(3),
                                child: LinearProgressIndicator(
                                  minHeight: 5,
                                  value: reviews
                                          .where((r) => r.rating == star)
                                          .length /
                                      reviews.length,
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            SizedBox(
                              width: 22,
                              child: Text(
                                '${reviews.where((r) => r.rating == star).length}',
                                style: TextStyle(
                                    fontSize: 11, color: cs.onSurfaceVariant),
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 10),

            // Review composer (hidden after submission, replaced by compact summary + Edit button)
            if (existingUserReview != null)
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
                decoration: BoxDecoration(
                  color: cs.primaryContainer.withValues(alpha: 0.28),
                  borderRadius: BorderRadius.circular(12),
                  border:
                      Border.all(color: cs.primary.withValues(alpha: 0.28)),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Icon(Icons.star, size: 15, color: cs.tertiary),
                              const SizedBox(width: 5),
                              Text(
                                'Your review (${existingUserReview.rating}/5 ★)',
                                style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
                          if (existingUserReview.comment.trim().isNotEmpty) ...[
                            const SizedBox(height: 2),
                            Text(
                              existingUserReview.comment,
                              style: TextStyle(
                                fontSize: 11.5,
                                color: cs.onSurfaceVariant,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    OutlinedButton.icon(
                      onPressed: () =>
                          _showEditReviewDialog(app, existingUserReview),
                      icon: const Icon(Icons.edit, size: 15),
                      label: const Text('Edit'),
                    ),
                  ],
                ),
              )
            else
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: cs.surfaceContainerHighest.withValues(alpha: 0.25),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: cs.outline.withValues(alpha: 0.22)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Flexible(
                          child: Text(
                            'Rate & Review ${app.appName}',
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        _buildBorderedBadge(
                          icon: Icons.star,
                          label: '$_draftStars / 5',
                          bgColor: cs.surface,
                          fgColor: cs.primary,
                          borderColor: cs.primary.withValues(alpha: 0.25),
                        ),
                      ],
                    ),
                    Row(
                      children: List.generate(5, (index) {
                        final star = index + 1;
                        return IconButton(
                          icon: Icon(
                            star <= _draftStars
                                ? Icons.star
                                : Icons.star_border,
                            color: star <= _draftStars
                                ? Colors.amber
                                : cs.onSurfaceVariant,
                          ),
                          onPressed: () => setState(() => _draftStars = star),
                        );
                      }),
                    ),
                    TextField(
                      controller: _reviewCommentController,
                      minLines: 1,
                      maxLines: 3,
                      decoration: const InputDecoration(
                        labelText: 'Share your experience...',
                        border: OutlineInputBorder(),
                        isDense: true,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Align(
                      alignment: Alignment.centerRight,
                      child: FilledButton.icon(
                        onPressed: () => _saveWebReview(
                          app,
                          stars: _draftStars,
                          comment: _reviewCommentController.text,
                        ),
                        icon: const Icon(Icons.send, size: 15),
                        label: const Text('Post Review'),
                      ),
                    ),
                  ],
                ),
              ),

            const SizedBox(height: 10),
            if (reviews.isEmpty)
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: cs.surfaceContainerHighest.withValues(alpha: 0.35),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: cs.outline.withValues(alpha: 0.22)),
                ),
                child: Row(
                  children: [
                    Icon(Icons.forum_outlined,
                        size: 16, color: cs.onSurfaceVariant),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'No written reviews yet. Be the first to rate this release!',
                        style: TextStyle(
                            fontSize: 12, color: cs.onSurfaceVariant),
                      ),
                    ),
                  ],
                ),
              )
            else ...[
              const Divider(height: 18),
              ...reviews.map((r) {
                final isOwn = r.userId == _currentUserId;
                return Container(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: cs.surfaceContainerHighest.withValues(alpha: 0.38),
                    borderRadius: BorderRadius.circular(14),
                    border:
                        Border.all(color: cs.outline.withValues(alpha: 0.22)),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            children: [
                              Text(r.userName,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 13)),
                              if (r.verifiedInstall) ...[
                                const SizedBox(width: 6),
                                _buildBorderedBadge(
                                  icon: Icons.verified,
                                  label: 'Verified Install',
                                  bgColor: cs.secondaryContainer,
                                  fgColor: cs.onSecondaryContainer,
                                  borderColor:
                                      cs.secondary.withValues(alpha: 0.25),
                                ),
                              ],
                            ],
                          ),
                          Text(
                            '★' * r.rating.clamp(1, 5),
                            style: TextStyle(
                              fontSize: 12,
                              color: cs.tertiary,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(r.comment, style: const TextStyle(fontSize: 12.5)),
                      const SizedBox(height: 8),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          ActionChip(
                            avatar: const Icon(Icons.thumb_up, size: 14),
                            label: Text('Helpful (${r.helpfulCount})',
                                style: const TextStyle(fontSize: 11.5)),
                            onPressed: () async {
                              try {
                                final fs = _tryFirestore();
                                if (fs != null &&
                                    FirebaseAuth.instance.currentUser != null) {
                                  await fs.voteReviewHelpful(r);
                                }
                              } catch (_) {}
                              if (!mounted) return;
                              setState(() {
                                final idx = reviews.indexWhere((x) => x.id == r.id);
                                if (idx >= 0) {
                                  reviews[idx] = AppReviewModel(
                                    id: r.id,
                                    appId: r.appId,
                                    appName: r.appName,
                                    userId: r.userId,
                                    userName: r.userName,
                                    rating: r.rating,
                                    comment: r.comment,
                                    verifiedInstall: r.verifiedInstall,
                                    helpfulCount: r.helpfulCount + 1,
                                    developerReply: r.developerReply,
                                    flagged: r.flagged,
                                  );
                                }
                              });
                            },
                          ),
                          if (isOwn)
                            Row(
                              children: [
                                IconButton(
                                  tooltip: 'Edit review',
                                  icon: Icon(Icons.edit,
                                      size: 18, color: cs.primary),
                                  onPressed: () =>
                                      _showEditReviewDialog(app, r),
                                ),
                                IconButton(
                                  tooltip: 'Delete review',
                                  icon: Icon(Icons.delete_outline,
                                      size: 18, color: cs.error),
                                  onPressed: () => _deleteWebReview(app, r),
                                ),
                              ],
                            ),
                        ],
                      ),
                    ],
                  ),
                );
              }),
            ],
          ],
        ),
      ),
    );
  }

  Future<void> _showEditReviewDialog(
    StoreAppModel app,
    AppReviewModel review,
  ) async {
    int dialogStars = review.rating.clamp(1, 5);
    final controller = TextEditingController(text: review.comment);

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        final cs = Theme.of(dialogContext).colorScheme;
        return StatefulBuilder(
          builder: (ctx, setDialogState) {
            return AlertDialog(
              title: Row(
                children: [
                  Icon(Icons.edit, size: 20, color: cs.primary),
                  const SizedBox(width: 8),
                  const Text('Edit Your Review',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                ],
              ),
              content: SizedBox(
                width: 400,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Flexible(
                          child: Text(
                            app.appName,
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: cs.onSurfaceVariant,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        _buildBorderedBadge(
                          icon: Icons.star,
                          label: '$dialogStars / 5',
                          bgColor: cs.primaryContainer,
                          fgColor: cs.onPrimaryContainer,
                          borderColor: cs.primary.withValues(alpha: 0.28),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(5, (idx) {
                        final star = idx + 1;
                        return IconButton(
                          icon: Icon(
                            star <= dialogStars
                                ? Icons.star
                                : Icons.star_border,
                            color: star <= dialogStars
                                ? Colors.amber
                                : cs.onSurfaceVariant,
                            size: 28,
                          ),
                          onPressed: () =>
                              setDialogState(() => dialogStars = star),
                        );
                      }),
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: controller,
                      minLines: 2,
                      maxLines: 5,
                      decoration: const InputDecoration(
                        labelText: 'Update your review...',
                        border: OutlineInputBorder(),
                      ),
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: const Text('Cancel'),
                ),
                FilledButton.icon(
                  onPressed: () {
                    final updatedText = controller.text;
                    Navigator.of(dialogContext).pop();
                    _saveWebReview(
                      app,
                      stars: dialogStars,
                      comment: updatedText,
                    );
                  },
                  icon: const Icon(Icons.send, size: 15),
                  label: const Text('Save Changes'),
                ),
              ],
            );
          },
        );
      },
    );
    controller.dispose();
  }

  Widget _buildPackageSpecsCard(StoreAppModel app) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: cs.outline.withValues(alpha: 0.28)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(Icons.security_outlined, size: 18, color: cs.primary),
                    const SizedBox(width: 6),
                    const Text(
                      'Package & Compatibility',
                      style:
                          TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                _buildBorderedBadge(
                  icon: Icons.android,
                  label: 'API ${app.minAndroidSdk}+',
                  bgColor: cs.secondaryContainer.withValues(alpha: 0.7),
                  fgColor: cs.onSecondaryContainer,
                  borderColor: cs.secondary.withValues(alpha: 0.28),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Container(
              decoration: BoxDecoration(
                color: cs.surfaceContainerHighest.withValues(alpha: 0.28),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: cs.outline.withValues(alpha: 0.22)),
              ),
              child: Column(
                children: [
                  _buildSpecRow(
                      cs, Icons.inventory_2_outlined, 'Package ID', app.packageName),
                  _buildSpecDivider(cs),
                  _buildSpecRow(cs, Icons.new_releases_outlined, 'Version',
                      'v${app.versionName} (Code ${app.versionCode})'),
                  _buildSpecDivider(cs),
                  _buildSpecRow(
                      cs, Icons.sd_storage_outlined, 'APK Size', app.apkFileSize),
                  _buildSpecDivider(cs),
                  _buildSpecRow(cs, Icons.phone_android, 'Android OS',
                      'API ${app.minAndroidSdk}+ (Target ${app.targetAndroidSdk})'),
                  _buildSpecDivider(cs),
                  _buildSpecRow(
                      cs, Icons.memory, 'Architecture', app.architecture),
                  _buildSpecDivider(cs),
                  _buildSpecRow(
                    cs,
                    app.offlineCapable
                        ? Icons.offline_bolt_outlined
                        : Icons.wifi,
                    'Connectivity',
                    app.offlineCapable ? 'Works Offline' : 'Requires Internet',
                  ),
                  if (app.sha256Checksum.isNotEmpty) ...[
                    _buildSpecDivider(cs),
                    _buildSpecRow(cs, Icons.fingerprint, 'SHA-256',
                        '${app.sha256Checksum.substring(0, app.sha256Checksum.length.clamp(0, 20))}...'),
                  ],
                ],
              ),
            ),
            if (app.permissionsList.isNotEmpty) ...[
              const SizedBox(height: 10),
              Row(
                children: [
                  Icon(Icons.admin_panel_settings_outlined,
                      size: 14, color: cs.primary),
                  const SizedBox(width: 5),
                  Text(
                    'Declared Permissions (${app.permissionsList.length})',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: cs.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: app.permissionsList
                    .map(
                      (perm) => Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: cs.surfaceContainerHighest.withValues(alpha: 0.6),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(
                            color: cs.outline.withValues(alpha: 0.25),
                          ),
                        ),
                        child: Text(
                          perm,
                          style: const TextStyle(
                            fontSize: 10.5,
                            fontFamily: 'monospace',
                          ),
                        ),
                      ),
                    )
                    .toList(),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildDeveloperInfoCard(StoreAppModel app) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: cs.outline.withValues(alpha: 0.28)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.storefront_outlined, size: 18, color: cs.primary),
                const SizedBox(width: 6),
                const Text(
                  'Developer & Distribution Info',
                  style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Container(
              decoration: BoxDecoration(
                color: cs.surfaceContainerHighest.withValues(alpha: 0.28),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: cs.outline.withValues(alpha: 0.22)),
              ),
              child: Column(
                children: [
                  _buildSpecRow(
                      cs, Icons.verified_outlined, 'Developer', app.developerName),
                  _buildSpecDivider(cs),
                  _buildSpecRow(
                      cs, Icons.email_outlined, 'Support', app.supportEmail),
                  _buildSpecDivider(cs),
                  _buildSpecRow(cs, Icons.cloud_done_outlined, 'Source',
                      'GitHub Releases APK'),
                ],
              ),
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                if (app.websiteUrl.isNotEmpty)
                  _buildCompactActionChip(
                    cs,
                    icon: Icons.language,
                    label: 'Website',
                    onTap: () => _launchExternalUrl(app.websiteUrl),
                  ),
                if (app.githubRepoUrl.isNotEmpty)
                  _buildCompactActionChip(
                    cs,
                    icon: Icons.code,
                    label: 'GitHub Repo',
                    onTap: () => _launchExternalUrl(app.githubRepoUrl),
                  ),
                if (app.privacyPolicyUrl.isNotEmpty)
                  _buildCompactActionChip(
                    cs,
                    icon: Icons.policy_outlined,
                    label: 'Privacy Policy',
                    onTap: () => _launchExternalUrl(app.privacyPolicyUrl),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSpecRow(
    ColorScheme cs,
    IconData icon,
    String label,
    String value,
  ) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            children: [
              Icon(icon, size: 14, color: cs.primary),
              const SizedBox(width: 6),
              Text(
                label,
                style: TextStyle(fontSize: 11.5, color: cs.onSurfaceVariant),
              ),
            ],
          ),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              value,
              style: const TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w600,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSpecDivider(ColorScheme cs) {
    return Divider(
      height: 1,
      thickness: 1,
      color: cs.outline.withValues(alpha: 0.14),
    );
  }

  Widget _buildCompactActionChip(
    ColorScheme cs, {
    required IconData icon,
    required String label,
    required VoidCallback onTap,
  }) {
    return InkWell(
      borderRadius: BorderRadius.circular(10),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: cs.surfaceContainerHighest.withValues(alpha: 0.45),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: cs.outline.withValues(alpha: 0.28)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 14, color: cs.primary),
            const SizedBox(width: 5),
            Text(
              label,
              style: const TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ExtractedLink {
  final String label;
  final String url;
  const _ExtractedLink({required this.label, required this.url});
}
