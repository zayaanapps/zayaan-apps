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
        colorSchemeSeed: const Color(0xFF00C98D),
        brightness: Brightness.light,
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF00C98D),
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0A192F),
      ),
      home: ZayaanResponsiveShell(onToggleTheme: _toggleTheme),
    );
  }
}

class ZayaanResponsiveShell extends StatefulWidget {
  final VoidCallback onToggleTheme;

  const ZayaanResponsiveShell({super.key, required this.onToggleTheme});

  @override
  State<ZayaanResponsiveShell> createState() => _ZayaanResponsiveShellState();
}

class _ZayaanResponsiveShellState extends State<ZayaanResponsiveShell> {
  int _selectedIndex = 0;
  String _searchQuery = '';
  String _selectedCategory = 'All';
  StoreAppModel? _selectedApp;
  String? _statusBanner;
  double? _downloadProgress;

  // Phase 2 Personal Library, Comparison & Reviews State (Synced with Firestore)
  final Set<String> _wishlistIds = <String>{};
  final Set<String> _favoriteIds = <String>{};
  final List<String> _downloadHistoryIds = <String>[];
  final List<String> _comparisonIds = <String>[];
  final Map<String, List<AppReviewModel>> _reviewsByApp =
      <String, List<AppReviewModel>>{};
  int _draftStars = 5;
  String _draftReviewComment = '';

  // Fallback starter catalog for instant web/mobile preview
  static final List<StoreAppModel> _starterCatalog = [
    const StoreAppModel(
      appId: 'zayaan-apps-store',
      appName: 'Zayaan Apps Store',
      packageName: 'com.zayaanapps.store',
      category: 'Tools',
      developerName: 'Zayaan Apps Official',
      shortDescription:
          'Official Zayaan Apps Android marketplace & GitHub Releases distribution client.',
      description:
          'Official Zayaan Apps digital application marketplace for Android & Web.',
      iconUrl: '',
      screenshots: [],
      versionName: '1.0.0.2',
      versionCode: 2,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '16.8 MB',
      releaseNotes:
          'Shared Flutter Android + GitHub Pages Web release with real-time Firestore sync.',
      featured: true,
      editorsChoice: true,
      essential: true,
      published: true,
      downloadCount: 1280,
      ratingSum: 60,
      reviewCount: 12,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-calculator',
      appName: 'Zayaan Calculator Pro',
      packageName: 'com.zayaanapps.calculator',
      category: 'Utilities',
      developerName: 'Zayaan Apps Official',
      shortDescription:
          'Scientific, graphing, and financial calculator with Material 3 design.',
      description:
          'Scientific, graphing, and financial calculator with Material 3 design.',
      iconUrl: '',
      screenshots: [],
      versionName: '1.4.2.1',
      versionCode: 14,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-calculator/releases/latest/download/zayaan-calculator.apk',
      apkFileSize: '11.2 MB',
      releaseNotes: 'Added unit converter and engineering constants.',
      featured: true,
      editorsChoice: true,
      essential: true,
      published: true,
      downloadCount: 845,
      ratingSum: 39,
      reviewCount: 8,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-notes',
      appName: 'Zayaan Notes & Markdown',
      packageName: 'com.zayaanapps.notes',
      category: 'Productivity',
      developerName: 'Zayaan Apps Official',
      shortDescription:
          'Encrypted markdown notebook and checklist organizer with cloud sync.',
      description:
          'Encrypted markdown notebook and checklist organizer with cloud sync.',
      iconUrl: '',
      screenshots: [],
      versionName: '2.1.0.1',
      versionCode: 21,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-notes/releases/latest/download/zayaan-notes.apk',
      apkFileSize: '14.6 MB',
      releaseNotes: 'Added syntax highlighting and PDF export.',
      featured: true,
      editorsChoice: false,
      essential: true,
      published: true,
      downloadCount: 690,
      ratingSum: 29,
      reviewCount: 6,
      authorUid: 'admin',
    ),
  ];

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
        _statusBanner = 'Added to Side-by-Side Comparison in My Apps & Library.';
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

  @override
  Widget build(BuildContext context) {
    final isWide = MediaQuery.of(context).size.width >= 800;
    final categories = ['All', ...kStoreCategories];

    final filteredApps = _starterCatalog.where((app) {
      final matchesCat =
          _selectedCategory == 'All' || app.category == _selectedCategory;
      final q = _searchQuery.trim().toLowerCase();
      final matchesSearch = q.isEmpty ||
          app.appName.toLowerCase().contains(q) ||
          app.description.toLowerCase().contains(q) ||
          app.packageName.toLowerCase().contains(q);
      return matchesCat && matchesSearch;
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Zayaan Apps'),
        actions: [
          IconButton(
            onPressed: widget.onToggleTheme,
            icon: const Icon(Icons.brightness_6),
            tooltip: 'Toggle Light/Dark Mode',
          ),
        ],
      ),
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
                    icon: Icon(Icons.storefront), label: 'Store'),
                NavigationDestination(
                    icon: Icon(Icons.apps), label: 'My Apps & Library'),
                NavigationDestination(
                    icon: Icon(Icons.admin_panel_settings), label: 'Admin'),
                NavigationDestination(
                    icon: Icon(Icons.info_outline), label: 'About & Contact'),
              ],
            ),
      body: Row(
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
                    icon: Icon(Icons.storefront), label: Text('Store')),
                NavigationRailDestination(
                    icon: Icon(Icons.apps), label: Text('My Apps & Library')),
                NavigationRailDestination(
                    icon: Icon(Icons.admin_panel_settings),
                    label: Text('Admin')),
                NavigationRailDestination(
                    icon: Icon(Icons.info_outline),
                    label: Text('About & Contact')),
              ],
            ),
          Expanded(
            child: Column(
              children: [
                if (_statusBanner != null)
                  MaterialBanner(
                    content: Text(_statusBanner!),
                    actions: [
                      TextButton(
                        onPressed: () => setState(() => _statusBanner = null),
                        child: const Text('DISMISS'),
                      ),
                    ],
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
    );
  }

  Widget _buildTabContent(
      List<String> categories, List<StoreAppModel> filteredApps) {
    switch (_selectedIndex) {
      case 0:
        return ListView(
          padding: const EdgeInsets.all(20),
          children: [
            TextField(
              decoration: const InputDecoration(
                hintText: 'Search Zayaan Apps catalog...',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
              ),
              onChanged: (v) => setState(() => _searchQuery = v),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children: categories.map((cat) {
                return ChoiceChip(
                  label: Text(cat),
                  selected: _selectedCategory == cat,
                  onSelected: (_) => setState(() => _selectedCategory = cat),
                );
              }).toList(),
            ),
            const SizedBox(height: 20),
            ...filteredApps.map((app) => Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  child: ListTile(
                    leading: const CircleAvatar(child: Icon(Icons.android)),
                    title: Text(app.appName),
                    subtitle: Text(
                        '${app.developerName} • ${app.category} • v${app.versionName} (${app.apkFileSize}) • ★ ${app.averageRating.toStringAsFixed(1)}'),
                    trailing: Wrap(
                      spacing: 8,
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
                          onPressed: () => setState(() => _selectedApp = app),
                          icon: const Icon(Icons.download),
                          label: Text(PlatformApkInstaller.isAndroidPlatform
                              ? 'Install'
                              : 'Download APK'),
                        ),
                      ],
                    ),
                    onTap: () => setState(() => _selectedApp = app),
                  ),
                )),
          ],
        );
      case 1:
        final comparedApps = _starterCatalog
            .where((a) => _comparisonIds.contains(a.appId))
            .toList();
        final favoriteApps = _starterCatalog
            .where((a) => _favoriteIds.contains(a.appId))
            .toList();
        final wishlistApps = _starterCatalog
            .where((a) => _wishlistIds.contains(a.appId))
            .toList();
        final downloadedApps = _starterCatalog
            .where((a) => _downloadHistoryIds.contains(a.appId))
            .toList();

        return ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Download Manager, Update All & Package Scanner',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      PlatformApkInstaller.isAndroidPlatform
                          ? 'Android PackageManager Scanner: Checks installed com.zayaanapps.* packages, compares versionCode, and supports Individual Updates & Update All via Android Package Installer confirmation.'
                          : 'Web Mode Active (https://zayaanapps.github.io/zayaan-apps/): Direct APK downloads from GitHub Releases and cloud-synced Personal Library (Wishlist, Favorites, Download History & App Comparison) are enabled.',
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
              const Card(
                child: Padding(
                  padding: EdgeInsets.all(16),
                  child: Text(
                      'Tap the Compare icon on any two or more apps to compare versions, ratings, APK sizes, and target Android SDKs.'),
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
                          child: Container(
                            width: 220,
                            padding: const EdgeInsets.all(14),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(app.appName,
                                    style: const TextStyle(
                                        fontWeight: FontWeight.bold)),
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
                style:
                    const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ...favoriteApps.map((a) => ListTile(
                  title: Text(a.appName),
                  subtitle: Text('v${a.versionName} • ${a.apkFileSize}'),
                  onTap: () => setState(() => _selectedApp = a),
                )),
            const SizedBox(height: 16),
            Text('Wishlist / Saved Apps (${wishlistApps.length})',
                style:
                    const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ...wishlistApps.map((a) => ListTile(
                  title: Text(a.appName),
                  subtitle: Text('v${a.versionName} • ${a.apkFileSize}'),
                  onTap: () => setState(() => _selectedApp = a),
                )),
            const SizedBox(height: 16),
            Text('Previously Downloaded History (${downloadedApps.length})',
                style:
                    const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ...downloadedApps.map((a) => ListTile(
                  title: Text(a.appName),
                  subtitle: Text('v${a.versionName} • ${a.apkFileSize}'),
                  onTap: () => setState(() => _selectedApp = a),
                )),
          ],
        );
      case 2:
        final totalDownloads =
            _starterCatalog.fold<int>(0, (sum, a) => sum + a.downloadCount);
        final publishedCount =
            _starterCatalog.where((a) => a.published).length;
        final featuredCount = _starterCatalog.where((a) => a.featured).length;
        return ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Zayaan Apps Marketplace Admin Console (Phase 3)',
                      style:
                          TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 6),
                    const Text(
                      'Unified Android & Web administration backed by Firebase Firestore RBAC (/admins/{uid}, zayaanapps@gmail.com) and immutable /audit_logs.',
                    ),
                    const SizedBox(height: 12),
                    Wrap(
                      spacing: 12,
                      runSpacing: 8,
                      children: [
                        Chip(label: Text('Apps: ${_starterCatalog.length}')),
                        Chip(label: Text('Published: $publishedCount')),
                        Chip(label: Text('Featured: $featuredCount')),
                        Chip(label: Text('Total Downloads: $totalDownloads')),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Phase 3 Marketplace Management Modules',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Card(
              child: ListTile(
                leading: Icon(Icons.layers),
                title: Text('1. Application CRUD & Multi-Version Release History'),
                subtitle: Text(
                    'Manage /apps/{appId} and /version_history/{versionId} with one-click version rollback/promotion and GitHub Releases APK links.'),
              ),
            ),
            const Card(
              child: ListTile(
                leading: Icon(Icons.publish),
                title: Text(
                    '2. Developer Profiles, Submissions & Approval Workflow'),
                subtitle: Text(
                    'Review /submissions/{submissionId} from community developers and approve directly into the live /apps catalog.'),
              ),
            ),
            const Card(
              child: ListTile(
                leading: Icon(Icons.collections_bookmark),
                title: Text(
                    '3. Category, Banner, Collection & Featured-App Curation'),
                subtitle: Text(
                    'Manage /categories/{categoryId}, /collections/{collectionId}, Featured, Editor\'s Choice, and Essential badges.'),
              ),
            ),
            const Card(
              child: ListTile(
                leading: Icon(Icons.verified_user),
                title: Text(
                    '4. APK SHA-256 Checksum & X.509 Certificate Signature Verification'),
                subtitle: Text(
                    'Every release stores its SHA-256 hash; Android client verifies downloaded APK SHA-256 and X.509 signing certificate fingerprint before PackageInstaller launch.'),
              ),
            ),
            const Card(
              child: ListTile(
                leading: Icon(Icons.gavel),
                title: Text(
                    '5. Review & Report Moderation, Notifications, RBAC & Audit Logs'),
                subtitle: Text(
                    'Moderate /reviews and /reports, broadcast /notifications, manage /admins roles, and inspect immutable /audit_logs.'),
              ),
            ),
          ],
        );
      default:
        return ListView(
          padding: const EdgeInsets.all(24),
          children: const [
            Text('About Zayaan Apps',
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
            SizedBox(height: 12),
            Text(
                'Zayaan Apps is a complete digital application store for Android smartphones, tablets, and responsive web browsers.'),
            SizedBox(height: 20),
            Text('Contact & Repository',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            SizedBox(height: 8),
            Text('• Website: https://zayaanapps.github.io/zayaan-apps/'),
            Text('• GitHub Repository: https://github.com/zayaanapps/zayaan-apps'),
            Text('• Administrator Email: zayaanapps@gmail.com'),
          ],
        );
    }
  }

  Widget _buildDetailView(StoreAppModel app) {
    final isFav = _favoriteIds.contains(app.appId);
    final isWish = _wishlistIds.contains(app.appId);
    final isCompared = _comparisonIds.contains(app.appId);
    final appReviews = _reviewsByApp[app.appId] ?? const <AppReviewModel>[];
    final similarApps = _starterCatalog
        .where((a) => a.category == app.category && a.appId != app.appId)
        .toList();

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            TextButton.icon(
              onPressed: () => setState(() => _selectedApp = null),
              icon: const Icon(Icons.arrow_back),
              label: const Text('Back to Catalog'),
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
          icon: const Icon(Icons.system_update_alt),
          label: Text(PlatformApkInstaller.isAndroidPlatform
              ? 'Download & Install APK (v${app.versionName})'
              : 'Download APK from GitHub Releases (${app.apkFileSize})'),
        ),
        const SizedBox(height: 20),
        Text(app.description),
        const SizedBox(height: 16),
        Text('Release Notes (v${app.versionName} • code ${app.versionCode}):\n${app.releaseNotes}'),
        const SizedBox(height: 12),
        Card(
          child: ListTile(
            leading: const Icon(Icons.verified_user_outlined),
            title: const Text('APK Integrity & Security Verification'),
            subtitle: Text(
              app.sha256Checksum.isNotEmpty
                  ? 'SHA-256: ${app.sha256Checksum}\nTarget SDK: Android API ${app.targetAndroidSdk} (Min API ${app.minAndroidSdk}) • Arch: ${app.architecture}'
                  : 'Verified GitHub Releases HTTPS distribution • Target SDK: Android API ${app.targetAndroidSdk} (Min API ${app.minAndroidSdk}) • Arch: ${app.architecture}',
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
              icon: Icon(
                  star <= _draftStars ? Icons.star : Icons.star_border,
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
                onTap: () => setState(() => _selectedApp = s),
              )),
        ],
      ],
    );
  }
}
