import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_auth/firebase_auth.dart';
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

  // Fallback starter catalog for instant web/mobile preview
  static final List<StoreAppModel> _starterCatalog = [
    const StoreAppModel(
      appId: 'zayaan-apps-store',
      appName: 'Zayaan Apps Store',
      packageName: 'com.zayaanapps.store',
      category: 'Tools',
      developerName: 'Zayaan Apps Official',
      description:
          'Official Zayaan Apps digital application marketplace for Android & Web.',
      iconUrl: '',
      screenshots: [],
      versionName: '2.0.0',
      versionCode: 2,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk',
      apkFileSize: '16.8 MB',
      releaseNotes:
          'Shared Flutter Android + GitHub Pages Web release with real-time Firestore sync.',
      featured: true,
      published: true,
      downloadCount: 1280,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-calculator',
      appName: 'Zayaan Calculator Pro',
      packageName: 'com.zayaanapps.calculator',
      category: 'Utilities',
      developerName: 'Zayaan Apps Official',
      description:
          'Scientific, graphing, and financial calculator with Material 3 design.',
      iconUrl: '',
      screenshots: [],
      versionName: '1.4.2',
      versionCode: 14,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-calculator/releases/latest/download/zayaan-calculator.apk',
      apkFileSize: '11.2 MB',
      releaseNotes: 'Added unit converter and engineering constants.',
      featured: true,
      published: true,
      downloadCount: 845,
      authorUid: 'admin',
    ),
    const StoreAppModel(
      appId: 'zayaan-notes',
      appName: 'Zayaan Notes & Markdown',
      packageName: 'com.zayaanapps.notes',
      category: 'Productivity',
      developerName: 'Zayaan Apps Official',
      description:
          'Encrypted markdown notebook and checklist organizer with cloud sync.',
      iconUrl: '',
      screenshots: [],
      versionName: '2.1.0',
      versionCode: 21,
      apkDownloadUrl:
          'https://github.com/zayaanapps/zayaan-notes/releases/latest/download/zayaan-notes.apk',
      apkFileSize: '14.6 MB',
      releaseNotes: 'Added syntax highlighting and PDF export.',
      featured: true,
      published: true,
      downloadCount: 690,
      authorUid: 'admin',
    ),
  ];

  Future<void> _handleDownload(StoreAppModel app) async {
    setState(() {
      _downloadProgress = 0.0;
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
                    icon: Icon(Icons.apps), label: 'My Apps'),
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
                    icon: Icon(Icons.apps), label: Text('My Apps')),
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
                        '${app.developerName} • ${app.category} • v${app.versionName} (${app.apkFileSize})'),
                    trailing: FilledButton.icon(
                      onPressed: () => setState(() => _selectedApp = app),
                      icon: const Icon(Icons.download),
                      label: Text(PlatformApkInstaller.isAndroidPlatform
                          ? 'Install'
                          : 'Download APK'),
                    ),
                    onTap: () => setState(() => _selectedApp = app),
                  ),
                )),
          ],
        );
      case 1:
        return Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(
              PlatformApkInstaller.isAndroidPlatform
                  ? 'Android PackageManager Scanner: Checks installed com.zayaanapps.* packages and compares versionCode for 1-tap updates.'
                  : 'Web Mode Active (https://zayaanapps.github.io/zayaan-apps/): Direct APK downloads from GitHub Releases are enabled. Android OS package installation is disabled on Web for security.',
              textAlign: TextAlign.center,
            ),
          ),
        );
      case 2:
        return const Center(
          child: Padding(
            padding: EdgeInsets.all(24),
            child: Text(
              'Zayaan Apps Admin Dashboard: Authenticate with Firebase Auth to add, edit, publish/unpublish apps, and sync GitHub Releases APK links in real time.',
              textAlign: TextAlign.center,
            ),
          ),
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
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            onPressed: () => setState(() => _selectedApp = null),
            icon: const Icon(Icons.arrow_back),
            label: const Text('Back to Catalog'),
          ),
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
        Text('Release Notes:\n${app.releaseNotes}'),
      ],
    );
  }
}
