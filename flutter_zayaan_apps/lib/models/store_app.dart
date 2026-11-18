import 'package:cloud_firestore/cloud_firestore.dart';

const List<String> kStoreCategories = [
  'Games',
  'Tools',
  'AI Apps',
  'Entertainment',
  'Productivity',
  'Education',
  'Utilities',
  'Other',
];

class StoreAppModel {
  final String appId;
  final String appName;
  final String packageName;
  final String category;
  final String developerName;
  final String description;
  final String iconUrl;
  final List<String> screenshots;
  final String versionName;
  final int versionCode;
  final String apkDownloadUrl;
  final String apkFileSize;
  final String releaseNotes;
  final bool featured;
  final bool published;
  final int downloadCount;
  final String authorUid;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const StoreAppModel({
    required this.appId,
    required this.appName,
    required this.packageName,
    required this.category,
    required this.developerName,
    required this.description,
    required this.iconUrl,
    required this.screenshots,
    required this.versionName,
    required this.versionCode,
    required this.apkDownloadUrl,
    required this.apkFileSize,
    required this.releaseNotes,
    required this.featured,
    required this.published,
    required this.downloadCount,
    required this.authorUid,
    this.createdAt,
    this.updatedAt,
  });

  factory StoreAppModel.fromFirestore(DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return StoreAppModel(
      appId: (data['appId'] as String?) ?? doc.id,
      appName: (data['appName'] as String?) ?? 'Untitled App',
      packageName: (data['packageName'] as String?) ?? 'com.zayaanapps.app',
      category: (data['category'] as String?) ?? 'Tools',
      developerName: (data['developerName'] as String?) ?? 'Zayaan Apps Studio',
      description: (data['description'] as String?) ?? '',
      iconUrl: (data['iconUrl'] as String?) ?? '',
      screenshots: (data['screenshots'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const <String>[],
      versionName: (data['versionName'] as String?) ?? '1.0.0',
      versionCode: (data['versionCode'] as num?)?.toInt() ?? 1,
      apkDownloadUrl: (data['apkDownloadUrl'] as String?) ?? '',
      apkFileSize: (data['apkFileSize'] as String?) ?? '12.0 MB',
      releaseNotes: (data['releaseNotes'] as String?) ?? '',
      featured: (data['featured'] as bool?) ?? false,
      published: (data['published'] as bool?) ?? true,
      downloadCount: (data['downloadCount'] as num?)?.toInt() ?? 0,
      authorUid: (data['authorUid'] as String?) ?? '',
      createdAt: (data['createdAt'] as Timestamp?)?.toDate(),
      updatedAt: (data['updatedAt'] as Timestamp?)?.toDate(),
    );
  }

  Map<String, dynamic> toCreatePayload(String uid) {
    return {
      'appId': appId,
      'appName': appName,
      'packageName': packageName,
      'category': kStoreCategories.contains(category) ? category : 'Other',
      'developerName': developerName,
      'description': description,
      'iconUrl': iconUrl,
      'screenshots': screenshots.take(10).toList(),
      'versionName': versionName,
      'versionCode': versionCode,
      'apkDownloadUrl': apkDownloadUrl,
      'apkFileSize': apkFileSize,
      'releaseNotes': releaseNotes,
      'featured': featured,
      'published': published,
      'downloadCount': downloadCount,
      'authorUid': uid,
      'createdAt': FieldValue.serverTimestamp(),
      'updatedAt': FieldValue.serverTimestamp(),
    };
  }

  Map<String, dynamic> toUpdatePayload() {
    return {
      'appName': appName,
      'packageName': packageName,
      'category': kStoreCategories.contains(category) ? category : 'Other',
      'developerName': developerName,
      'description': description,
      'iconUrl': iconUrl,
      'screenshots': screenshots.take(10).toList(),
      'versionName': versionName,
      'versionCode': versionCode,
      'apkDownloadUrl': apkDownloadUrl,
      'apkFileSize': apkFileSize,
      'releaseNotes': releaseNotes,
      'featured': featured,
      'published': published,
      'downloadCount': downloadCount,
      'updatedAt': FieldValue.serverTimestamp(),
    };
  }
}
