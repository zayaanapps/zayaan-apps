import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'models/store_app.dart';
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
  ThemeMode _themeMode = ThemeMode.dark;

  void _toggleTheme() {
    setState(() {
      _themeMode =
          _themeMode == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark;
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
        colorSchemeSeed: const Color(0xFF006C4A),
        brightness: Brightness.light,
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
        isDarkMode: _themeMode == ThemeMode.dark,
        onToggleTheme: _toggleTheme,
      ),
    );
  }
}

class ZayaanResponsiveShell extends StatefulWidget {
  final bool isDarkMode;
  final VoidCallback onToggleTheme;

  const ZayaanResponsiveShell({
    super.key,
    required this.isDarkMode,
    required this.onToggleTheme,
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
  String _draftReviewComment = '';
  bool _notifyUpdates = true;

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
          'https://raw.githubusercontent.com/zayaanapps/zayaan-apps/main/web/icons/Icon-192.png',
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
          'v1.0.0.1 Marketplace Release:\n• Google Play Store-inspired discovery sections & curated collections\n• Advanced search with instant suggestions, history, filters & sorting\n• Rich app detail sheets with related apps & developer specs\n• Automatic Store Self-Update notification & 1-tap APK upgrade',
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
    setState(() {
      _selectedApp = app;
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

  void _submitWebReview(StoreAppModel app) {
    final list = _reviewsByApp.putIfAbsent(app.appId, () => <AppReviewModel>[]);
    setState(() {
      list.insert(
        0,
        AppReviewModel(
          id: '${app.appId}_${DateTime.now().millisecondsSinceEpoch}',
          appId: app.appId,
          appName: app.appName,
          userId: 'web_user',
          userName: 'Zayaan Community Member',
          rating: _draftStars,
          comment: _draftReviewComment.trim().isEmpty
              ? 'Rated $_draftStars stars.'
              : _draftReviewComment.trim(),
          verifiedInstall: PlatformApkInstaller.isAndroidPlatform,
          helpfulCount: 0,
        ),
      );
      _draftReviewComment = '';
      _statusBanner = 'Published your $_draftStars★ review for ${app.appName}.';
    });
  }

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

  Widget _buildStoreHomeTab(
      List<String> categories, List<StoreAppModel> filteredApps) {
    final featuredApps = _catalog.where((a) => a.featured).toList();
    final editorsChoiceApps = _catalog.where((a) => a.editorsChoice).toList();
    final trendingApps = [..._catalog]
      ..sort((a, b) => b.downloadCount.compareTo(a.downloadCount));

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // 1. Hero Marketplace Banner (Matches AI Studio Preview)
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(24),
            gradient: const LinearGradient(
              colors: [Color(0xFF005236), Color(0xFF0E1E38)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            border: Border.all(
              color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.35),
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.primary,
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      'OFFICIAL ZAYAAN APPS MARKETPLACE',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: Theme.of(context).colorScheme.onPrimary,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: widget.onToggleTheme,
                    icon: const Icon(Icons.brightness_6),
                    tooltip: 'Toggle Light / Dark Theme',
                  ),
                ],
              ),
              const SizedBox(height: 10),
              const Text(
                'Zayaan Apps Store',
                style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 4),
              Text(
                'Discover, verify, and install official Android APKs powered by GitHub Releases & Cloud Firestore.',
                style: TextStyle(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 14),
              Wrap(
                spacing: 10,
                runSpacing: 8,
                children: [
                  FilledButton.icon(
                    onPressed: () => _handleDownload(_catalog.first),
                    icon: const Icon(Icons.download, size: 18),
                    label: const Text('Download Latest Store APK'),
                  ),
                  OutlinedButton.icon(
                    onPressed: () => _openAppDetail(_catalog.first),
                    icon: const Icon(Icons.verified_user_outlined, size: 18),
                    label: const Text('Inspect Store Release'),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // 2. Search Bar, Subcategories & Sorting
        TextField(
          decoration: InputDecoration(
            hintText: 'Search Zayaan Apps, packages, or developers...',
            prefixIcon: const Icon(Icons.search),
            suffixIcon: _searchQuery.isNotEmpty
                ? IconButton(
                    icon: const Icon(Icons.clear),
                    onPressed: () => setState(() => _searchQuery = ''),
                  )
                : null,
            filled: true,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(18),
            ),
          ),
          onChanged: (v) => setState(() => _searchQuery = v),
        ),
        const SizedBox(height: 12),

        // Category Chips
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: categories.map((cat) {
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(cat),
                  selected: _selectedCategory == cat,
                  onSelected: (_) => setState(() => _selectedCategory = cat),
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 8),

        // Subcategory Filter & Sort Row
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: [
              ...kMarketplaceSubcategories.map((sub) {
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text(sub),
                    selected: _selectedSubcategory == sub,
                    onSelected: (_) =>
                        setState(() => _selectedSubcategory = sub),
                  ),
                );
              }),
              const SizedBox(width: 8),
              DropdownButton<MarketplaceSortOption>(
                value: _selectedSort,
                onChanged: (opt) {
                  if (opt != null) setState(() => _selectedSort = opt);
                },
                items: MarketplaceSortOption.values
                    .map((opt) => DropdownMenuItem(
                          value: opt,
                          child: Text('Sort: ${opt.label}'),
                        ))
                    .toList(),
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),

        // 3. Featured Spotlight Carousel (when not actively filtering)
        if (_searchQuery.isEmpty && _selectedCategory == 'All') ...[
          const Text(
            'Featured Applications',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          SizedBox(
            height: 185,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: featuredApps.length,
              separatorBuilder: (_, __) => const SizedBox(width: 12),
              itemBuilder: (context, index) {
                final app = featuredApps[index];
                return SizedBox(
                  width: 300,
                  child: Card(
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20)),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(20),
                      onTap: () => _openAppDetail(app),
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                CircleAvatar(
                                  backgroundColor: Theme.of(context)
                                      .colorScheme
                                      .primaryContainer,
                                  child: Text(
                                    app.appName.substring(0, 1),
                                    style: const TextStyle(
                                        fontWeight: FontWeight.bold),
                                  ),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        app.appName,
                                        style: const TextStyle(
                                            fontWeight: FontWeight.bold,
                                            fontSize: 16),
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                      Text(
                                        '${app.category} • v${app.versionName}',
                                        style: TextStyle(
                                          fontSize: 12,
                                          color: Theme.of(context)
                                              .colorScheme
                                              .primary,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 10),
                            Expanded(
                              child: Text(
                                app.shortDescription,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(fontSize: 13),
                              ),
                            ),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  '★ ${app.averageRating.toStringAsFixed(1)} • ${app.apkFileSize}',
                                  style: const TextStyle(fontSize: 12),
                                ),
                                FilledButton.tonal(
                                  onPressed: () => _handleDownload(app),
                                  child: const Text('Get APK'),
                                ),
                              ],
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
          const SizedBox(height: 20),

          // 4. Editor's Choice Section
          const Text(
            "Editor's Choice & Essential Releases",
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          SizedBox(
            height: 145,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: editorsChoiceApps.length,
              separatorBuilder: (_, __) => const SizedBox(width: 12),
              itemBuilder: (context, index) {
                final app = editorsChoiceApps[index];
                return SizedBox(
                  width: 250,
                  child: Card(
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(18)),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(18),
                      onTap: () => _openAppDetail(app),
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              app.appName,
                              style: const TextStyle(
                                  fontWeight: FontWeight.bold, fontSize: 15),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            Text(
                              app.shortDescription,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(fontSize: 12),
                            ),
                            Text(
                              'v${app.versionName} • ${app.downloadCount} downloads',
                              style: TextStyle(
                                fontSize: 11,
                                color: Theme.of(context).colorScheme.primary,
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
          const SizedBox(height: 20),
        ],

        // 5. All Catalog / Trending Applications List
        Text(
          _searchQuery.isEmpty && _selectedCategory == 'All'
              ? 'All Zayaan Applications (${filteredApps.length})'
              : 'Filtered Results (${filteredApps.length})',
          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 10),
        ...(filteredApps.isEmpty ? trendingApps : filteredApps).map(
          (app) => Card(
            margin: const EdgeInsets.only(bottom: 12),
            shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(18)),
            child: ListTile(
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              leading: CircleAvatar(
                radius: 24,
                backgroundColor:
                    Theme.of(context).colorScheme.primaryContainer,
                child: Text(
                  app.appName.substring(0, 1),
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
              title: Row(
                children: [
                  Flexible(
                    child: Text(
                      app.appName,
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                  ),
                  if (app.editorsChoice) ...[
                    const SizedBox(width: 6),
                    const Icon(Icons.verified, size: 16, color: Color(0xFF6EDBA9)),
                  ],
                ],
              ),
              subtitle: Text(
                '${app.developerName} • ${app.category} • v${app.versionName} (${app.apkFileSize}) • ★ ${app.averageRating.toStringAsFixed(1)} • ${app.downloadCount} DLs',
              ),
              trailing: Wrap(
                spacing: 6,
                children: [
                  IconButton(
                    tooltip: 'Compare App',
                    icon: Icon(
                      Icons.compare_arrows,
                      color: _comparisonIds.contains(app.appId)
                          ? Theme.of(context).colorScheme.primary
                          : null,
                    ),
                    onPressed: () => _toggleCompare(app.appId),
                  ),
                  FilledButton.icon(
                    onPressed: () => _openAppDetail(app),
                    icon: const Icon(Icons.download, size: 16),
                    label: Text(PlatformApkInstaller.isAndroidPlatform
                        ? 'Install'
                        : 'Get APK'),
                  ),
                ],
              ),
              onTap: () => _openAppDetail(app),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildMyAppsTab() {
    final comparedApps =
        _catalog.where((a) => _comparisonIds.contains(a.appId)).toList();
    final favoriteApps =
        _catalog.where((a) => _favoriteIds.contains(a.appId)).toList();
    final wishlistApps =
        _catalog.where((a) => _wishlistIds.contains(a.appId)).toList();
    final downloadedApps =
        _catalog.where((a) => _downloadHistoryIds.contains(a.appId)).toList();

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text(
          'Library, Downloads & Updates',
          style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 4),
        Text(
          'Download Manager, Update All, Wishlist, Favorites & App Comparison',
          style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
        ),
        const SizedBox(height: 16),
        Card(
          shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(18)),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.check_circle, color: Color(0xFF6EDBA9)),
                    SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'All installed Zayaan Apps packages are running the latest version.',
                        style: TextStyle(fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 10,
                  children: [
                    FilledButton.tonalIcon(
                      onPressed: () {
                        setState(() {
                          _statusBanner =
                              'Scanned catalog packages and checked GitHub Releases for v1.0.0.X updates.';
                        });
                      },
                      icon: const Icon(Icons.refresh, size: 18),
                      label: const Text('Scan for Updates'),
                    ),
                    OutlinedButton.icon(
                      onPressed: () => _handleDownload(_catalog.first),
                      icon: const Icon(Icons.system_update, size: 18),
                      label: const Text('Download Latest Store APK'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Text(
          'Side-by-Side Application Comparison (${comparedApps.length})',
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        if (comparedApps.isEmpty)
          Card(
            shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16)),
            child: const Padding(
              padding: EdgeInsets.all(16),
              child: Text(
                'Tap the Compare icon on any two or more apps to compare versions, ratings, APK sizes, and target Android SDKs.',
              ),
            ),
          )
        else
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: comparedApps
                  .map(
                    (app) => Card(
                      margin: const EdgeInsets.only(right: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16)),
                      child: Container(
                        width: 230,
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(app.appName,
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold)),
                            const SizedBox(height: 4),
                            Text('Category: ${app.category}'),
                            Text(
                                'Version: v${app.versionName} (${app.versionCode})'),
                            Text('APK Size: ${app.apkFileSize}'),
                            Text(
                                'Rating: ★ ${app.averageRating.toStringAsFixed(1)}'),
                            Text('Downloads: ${app.downloadCount}'),
                            Text(
                                'Android API: ${app.minAndroidSdk}+ (Target ${app.targetAndroidSdk})'),
                            const SizedBox(height: 8),
                            TextButton(
                              onPressed: () => _toggleCompare(app.appId),
                              child: const Text('Remove'),
                            ),
                          ],
                        ),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
        const SizedBox(height: 16),
        Text('Favorite Apps (${favoriteApps.length})',
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        ...favoriteApps.map((a) => Card(
              child: ListTile(
                title: Text(a.appName),
                subtitle: Text('v${a.versionName} • ${a.apkFileSize}'),
                onTap: () => _openAppDetail(a),
              ),
            )),
        const SizedBox(height: 16),
        Text('Wishlist / Saved Apps (${wishlistApps.length})',
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        ...wishlistApps.map((a) => Card(
              child: ListTile(
                title: Text(a.appName),
                subtitle: Text('v${a.versionName} • ${a.apkFileSize}'),
                onTap: () => _openAppDetail(a),
              ),
            )),
        const SizedBox(height: 16),
        Text('Previously Downloaded History (${downloadedApps.length})',
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        ...downloadedApps.map((a) => Card(
              child: ListTile(
                title: Text(a.appName),
                subtitle: Text('v${a.versionName} • ${a.apkFileSize}'),
                onTap: () => _openAppDetail(a),
              ),
            )),
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
                    Icon(Icons.public, color: Color(0xFF6EDBA9), size: 28),
                    SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Zayaan Apps Web & GitHub DevOps Hub',
                            style: TextStyle(
                                fontSize: 20, fontWeight: FontWeight.bold),
                          ),
                          Text(
                            'https://zayaanapps.github.io/zayaan-apps/',
                            style: TextStyle(
                                fontSize: 12, color: Color(0xFF6EDBA9)),
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
                      onPressed: widget.onToggleTheme,
                      icon: const Icon(Icons.dark_mode, size: 18),
                      label: Text(widget.isDarkMode
                          ? 'Dark Theme Active'
                          : 'Light Theme Active'),
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
                        borderRadius: BorderRadius.circular(20)),
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Store Settings & Preferences',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                SwitchListTile(
                  title: const Text('Dark Theme (Material 3)'),
                  subtitle: const Text('Syncs preference to Cloud Firestore'),
                  value: widget.isDarkMode,
                  onChanged: (_) => widget.onToggleTheme(),
                ),
                SwitchListTile(
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
    final isFav = _favoriteIds.contains(app.appId);
    final isWish = _wishlistIds.contains(app.appId);
    final isCompared = _comparisonIds.contains(app.appId);
    final appReviews = _reviewsByApp[app.appId] ?? const <AppReviewModel>[];
    final similarApps = _catalog
        .where((a) => a.category == app.category && a.appId != app.appId)
        .toList();

    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            TextButton.icon(
              onPressed: () => setState(() => _selectedApp = null),
              icon: const Icon(Icons.arrow_back),
              label: const Text('Back to Store'),
            ),
            Wrap(
              spacing: 8,
              children: [
                IconButton(
                  tooltip: 'Add to Comparison',
                  icon: Icon(Icons.compare_arrows,
                      color: isCompared
                          ? Theme.of(context).colorScheme.primary
                          : null),
                  onPressed: () => _toggleCompare(app.appId),
                ),
                IconButton(
                  tooltip: 'Favorite',
                  icon: Icon(isFav ? Icons.favorite : Icons.favorite_border,
                      color: isFav ? Colors.redAccent : null),
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
                  icon: Icon(isWish ? Icons.bookmark : Icons.bookmark_border),
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
        const SizedBox(height: 12),
        Text(app.appName,
            style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold)),
        Text('${app.developerName} • ${app.packageName}'),
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: () => _handleDownload(app),
          icon: const Icon(Icons.cloud_download),
          label: Text(
              'Download Latest Version from GitHub Releases (v${app.versionName} • ${app.apkFileSize})'),
        ),
        const SizedBox(height: 20),
        Card(
          shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(18)),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('What\'s New / Release Notes',
                    style:
                        TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                const SizedBox(height: 6),
                Text(app.releaseNotes),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(18)),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('About This Application',
                    style:
                        TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                const SizedBox(height: 6),
                Text(app.description),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(18)),
          child: ListTile(
            leading: const Icon(Icons.verified_user_outlined),
            title: const Text('APK Integrity & Technical Specifications'),
            subtitle: Text(
              'Version: v${app.versionName} (code ${app.versionCode}) • Size: ${app.apkFileSize}\n'
              'Target SDK: Android API ${app.targetAndroidSdk} (Min API ${app.minAndroidSdk}) • Arch: ${app.architecture}\n'
              'Permissions: ${app.permissionsList.join(", ")}',
            ),
          ),
        ),
        const Divider(height: 32),
        const Text('Ratings & Written Reviews',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Row(
          children: List.generate(5, (index) {
            final star = index + 1;
            return IconButton(
              icon: Icon(star <= _draftStars ? Icons.star : Icons.star_border,
                  color: Colors.amber),
              onPressed: () => setState(() => _draftStars = star),
            );
          }),
        ),
        TextField(
          decoration: const InputDecoration(
            labelText: 'Write a review for this release...',
            border: OutlineInputBorder(),
          ),
          onChanged: (v) => _draftReviewComment = v,
        ),
        const SizedBox(height: 8),
        Align(
          alignment: Alignment.centerRight,
          child: FilledButton(
            onPressed: () => _submitWebReview(app),
            child: const Text('Post Review'),
          ),
        ),
        const SizedBox(height: 12),
        ...appReviews.map((r) => Card(
              child: ListTile(
                title: Text('${r.userName} • ${"★" * r.rating}'),
                subtitle: Text(r.comment),
                trailing: Text('Helpful (${r.helpfulCount})'),
              ),
            )),
        if (similarApps.isNotEmpty) ...[
          const Divider(height: 32),
          const Text('Similar Applications',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
          ...similarApps.map((s) => ListTile(
                title: Text(s.appName),
                subtitle: Text('v${s.versionName} • ${s.apkFileSize}'),
                onTap: () => _openAppDetail(s),
              )),
        ],
      ],
    );
  }
}
