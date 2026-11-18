import 'package:cloud_firestore/cloud_firestore.dart';

const List<String> kStoreCategories = [
  'Games',
  'Tools',
  'AI Apps',
  'Entertainment',
  'Productivity',
  'Education',
  'Utilities',
  'Business',
  'Social & Communication',
  'Other',
];

const List<String> kMarketplaceSubcategories = [
  'All',
  "Editor's Choice",
  'Essential Apps',
  'Offline Ready',
  'Under 15 MB',
  'Latest v1.0.0.x',
];

enum MarketplaceSortOption {
  relevance('Relevance'),
  popularity('Most Popular'),
  highestRated('Highest Rated'),
  recentlyUpdated('Recently Updated'),
  recentlyAdded('Recently Added'),
  alphabetical('Alphabetical (A-Z)'),
  fileSize('Smallest File Size');

  final String label;
  const MarketplaceSortOption(this.label);
}

class StoreAppModel {
  final String appId;
  final String appName;
  final String packageName;
  final String category;
  final String developerName;
  final String shortDescription;
  final String description;
  final String iconUrl;
  final List<String> screenshots;
  final String versionName;
  final int versionCode;
  final String apkDownloadUrl;
  final String apkFileSize;
  final String releaseNotes;
  final bool featured;
  final bool editorsChoice;
  final bool essential;
  final bool published;
  final int downloadCount;
  final int ratingSum;
  final int reviewCount;
  final int minAndroidSdk;
  final int targetAndroidSdk;
  final String architecture;
  final List<String> permissionsList;
  final String privacyPolicyUrl;
  final String websiteUrl;
  final String supportEmail;
  final String githubRepoUrl;
  final String sha256Checksum;
  final String promoVideoUrl;
  final bool offlineCapable;
  final int pageViews;
  final String authorUid;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const StoreAppModel({
    required this.appId,
    required this.appName,
    required this.packageName,
    required this.category,
    required this.developerName,
    this.shortDescription = '',
    required this.description,
    required this.iconUrl,
    required this.screenshots,
    required this.versionName,
    required this.versionCode,
    required this.apkDownloadUrl,
    required this.apkFileSize,
    required this.releaseNotes,
    required this.featured,
    this.editorsChoice = false,
    this.essential = false,
    required this.published,
    required this.downloadCount,
    this.ratingSum = 0,
    this.reviewCount = 0,
    this.minAndroidSdk = 24,
    this.targetAndroidSdk = 35,
    this.architecture = 'arm64-v8a, armeabi-v7a',
    this.permissionsList = const ['INTERNET'],
    this.privacyPolicyUrl = 'https://zayaanapps.github.io/zayaan-apps/',
    this.websiteUrl = 'https://zayaanapps.github.io/zayaan-apps/',
    this.supportEmail = 'zayaanapps@gmail.com',
    this.githubRepoUrl = 'https://github.com/zayaanapps/zayaan-apps',
    this.sha256Checksum = '',
    this.promoVideoUrl = '',
    this.offlineCapable = true,
    this.pageViews = 0,
    required this.authorUid,
    this.createdAt,
    this.updatedAt,
  });

  double get averageRating => reviewCount > 0 ? ratingSum / reviewCount : 0.0;

  factory StoreAppModel.fromFirestore(DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    final desc = (data['description'] as String?) ?? '';
    final shortDesc = (data['shortDescription'] as String?) ?? '';
    final featuredVal = (data['featured'] as bool?) ?? false;
    return StoreAppModel(
      appId: (data['appId'] as String?) ?? doc.id,
      appName: (data['appName'] as String?) ?? 'Untitled App',
      packageName: (data['packageName'] as String?) ?? 'com.zayaanapps.app',
      category: (data['category'] as String?) ?? 'Tools',
      developerName: (data['developerName'] as String?) ?? 'Zayaan Apps Studio',
      shortDescription: shortDesc.isNotEmpty
          ? shortDesc
          : (desc.isNotEmpty ? desc.split('\n').first : ''),
      description: desc,
      iconUrl: (data['iconUrl'] as String?) ?? '',
      screenshots: (data['screenshots'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const <String>[],
      versionName: (data['versionName'] as String?) ?? '1.0.0.1',
      versionCode: (data['versionCode'] as num?)?.toInt() ?? 1,
      apkDownloadUrl: (data['apkDownloadUrl'] as String?) ?? '',
      apkFileSize: (data['apkFileSize'] as String?) ?? '12.0 MB',
      releaseNotes: (data['releaseNotes'] as String?) ?? '',
      featured: featuredVal,
      editorsChoice: (data['editorsChoice'] as bool?) ?? featuredVal,
      essential: (data['essential'] as bool?) ?? false,
      published: (data['published'] as bool?) ?? true,
      downloadCount: (data['downloadCount'] as num?)?.toInt() ?? 0,
      ratingSum: (data['ratingSum'] as num?)?.toInt() ?? 0,
      reviewCount: (data['reviewCount'] as num?)?.toInt() ?? 0,
      minAndroidSdk: (data['minAndroidSdk'] as num?)?.toInt() ?? 24,
      targetAndroidSdk: (data['targetAndroidSdk'] as num?)?.toInt() ?? 35,
      architecture: (data['architecture'] as String?) ?? 'arm64-v8a, armeabi-v7a',
      permissionsList: (data['permissionsList'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const <String>['INTERNET'],
      privacyPolicyUrl: (data['privacyPolicyUrl'] as String?) ??
          'https://zayaanapps.github.io/zayaan-apps/',
      websiteUrl:
          (data['websiteUrl'] as String?) ?? 'https://zayaanapps.github.io/zayaan-apps/',
      supportEmail: (data['supportEmail'] as String?) ?? 'zayaanapps@gmail.com',
      githubRepoUrl: (data['githubRepoUrl'] as String?) ??
          'https://github.com/zayaanapps/zayaan-apps',
      sha256Checksum: (data['sha256Checksum'] as String?) ?? '',
      promoVideoUrl: (data['promoVideoUrl'] as String?) ?? '',
      offlineCapable: (data['offlineCapable'] as bool?) ?? true,
      pageViews: (data['pageViews'] as num?)?.toInt() ?? 0,
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
      'shortDescription': shortDescription,
      'description': description,
      'iconUrl': iconUrl,
      'screenshots': screenshots.take(10).toList(),
      'versionName': versionName,
      'versionCode': versionCode,
      'apkDownloadUrl': apkDownloadUrl,
      'apkFileSize': apkFileSize,
      'releaseNotes': releaseNotes,
      'featured': featured,
      'editorsChoice': editorsChoice,
      'essential': essential,
      'published': published,
      'downloadCount': downloadCount,
      'minAndroidSdk': minAndroidSdk,
      'targetAndroidSdk': targetAndroidSdk,
      'architecture': architecture,
      'permissionsList': permissionsList.take(30).toList(),
      'privacyPolicyUrl': privacyPolicyUrl,
      'websiteUrl': websiteUrl,
      'supportEmail': supportEmail,
      'githubRepoUrl': githubRepoUrl,
      'sha256Checksum': sha256Checksum,
      'promoVideoUrl': promoVideoUrl,
      'offlineCapable': offlineCapable,
      'pageViews': pageViews,
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
      'shortDescription': shortDescription,
      'description': description,
      'iconUrl': iconUrl,
      'screenshots': screenshots.take(10).toList(),
      'versionName': versionName,
      'versionCode': versionCode,
      'apkDownloadUrl': apkDownloadUrl,
      'apkFileSize': apkFileSize,
      'releaseNotes': releaseNotes,
      'featured': featured,
      'editorsChoice': editorsChoice,
      'essential': essential,
      'published': published,
      'downloadCount': downloadCount,
      'minAndroidSdk': minAndroidSdk,
      'targetAndroidSdk': targetAndroidSdk,
      'architecture': architecture,
      'permissionsList': permissionsList.take(30).toList(),
      'privacyPolicyUrl': privacyPolicyUrl,
      'websiteUrl': websiteUrl,
      'supportEmail': supportEmail,
      'githubRepoUrl': githubRepoUrl,
      'sha256Checksum': sha256Checksum,
      'promoVideoUrl': promoVideoUrl,
      'offlineCapable': offlineCapable,
      'pageViews': pageViews,
      'updatedAt': FieldValue.serverTimestamp(),
    };
  }
}

class AppReviewModel {
  final String id;
  final String appId;
  final String appName;
  final String userId;
  final String userName;
  final int rating;
  final String comment;
  final bool verifiedInstall;
  final int helpfulCount;
  final String developerReply;
  final bool flagged;

  const AppReviewModel({
    required this.id,
    required this.appId,
    required this.appName,
    required this.userId,
    required this.userName,
    required this.rating,
    required this.comment,
    this.verifiedInstall = false,
    this.helpfulCount = 0,
    this.developerReply = '',
    this.flagged = false,
  });

  factory AppReviewModel.fromFirestore(
      DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return AppReviewModel(
      id: doc.id,
      appId: (data['appId'] as String?) ?? '',
      appName: (data['appName'] as String?) ?? '',
      userId: (data['userId'] as String?) ?? '',
      userName: (data['userName'] as String?) ?? 'Zayaan User',
      rating: (data['rating'] as num?)?.toInt() ?? 5,
      comment: (data['comment'] as String?) ?? '',
      verifiedInstall: (data['verifiedInstall'] as bool?) ?? false,
      helpfulCount: (data['helpfulCount'] as num?)?.toInt() ?? 0,
      developerReply: (data['developerReply'] as String?) ?? '',
      flagged: (data['flagged'] as bool?) ?? false,
    );
  }
}

class UserPreferenceModel {
  final String userId;
  final bool darkMode;
  final bool notifyUpdates;
  final List<String> bookmarkedAppIds;
  final List<String> favoriteAppIds;
  final List<String> recentlyViewedAppIds;
  final List<String> downloadedAppIds;
  final List<String> searchHistory;

  const UserPreferenceModel({
    required this.userId,
    this.darkMode = true,
    this.notifyUpdates = true,
    this.bookmarkedAppIds = const [],
    this.favoriteAppIds = const [],
    this.recentlyViewedAppIds = const [],
    this.downloadedAppIds = const [],
    this.searchHistory = const [],
  });

  factory UserPreferenceModel.fromFirestore(
      String uid, Map<String, dynamic>? data) {
    if (data == null) return UserPreferenceModel(userId: uid);
    return UserPreferenceModel(
      userId: (data['userId'] as String?) ?? uid,
      darkMode: (data['darkMode'] as bool?) ?? true,
      notifyUpdates: (data['notifyUpdates'] as bool?) ?? true,
      bookmarkedAppIds: (data['bookmarkedAppIds'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
      favoriteAppIds: (data['favoriteAppIds'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
      recentlyViewedAppIds: (data['recentlyViewedAppIds'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
      downloadedAppIds: (data['downloadedAppIds'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
      searchHistory: (data['searchHistory'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
    );
  }
}

class StoreNotificationModel {
  final String id;
  final String title;
  final String body;
  final String category;
  final String targetAppId;

  const StoreNotificationModel({
    required this.id,
    required this.title,
    required this.body,
    this.category = 'ANNOUNCEMENT',
    this.targetAppId = '',
  });

  factory StoreNotificationModel.fromFirestore(
      DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return StoreNotificationModel(
      id: doc.id,
      title: (data['title'] as String?) ?? '',
      body: (data['body'] as String?) ?? '',
      category: (data['category'] as String?) ?? 'ANNOUNCEMENT',
      targetAppId: (data['targetAppId'] as String?) ?? '',
    );
  }
}

class AppCollectionModel {
  final String id;
  final String title;
  final String subtitle;
  final String badgeText;
  final List<String> appIds;
  final bool published;

  const AppCollectionModel({
    required this.id,
    required this.title,
    required this.subtitle,
    this.badgeText = 'COLLECTION',
    this.appIds = const [],
    this.published = true,
  });

  factory AppCollectionModel.fromFirestore(
      DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return AppCollectionModel(
      id: (data['id'] as String?) ?? doc.id,
      title: (data['title'] as String?) ?? '',
      subtitle: (data['subtitle'] as String?) ?? '',
      badgeText: (data['badgeText'] as String?) ?? 'COLLECTION',
      appIds: (data['appIds'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
      published: (data['published'] as bool?) ?? true,
    );
  }
}

class DeveloperSubmissionModel {
  final String id;
  final String appName;
  final String packageName;
  final String category;
  final String developerName;
  final String developerEmail;
  final String githubRepoUrl;
  final String apkDownloadUrl;
  final String versionName;
  final int versionCode;
  final String description;
  final String status;
  final String reviewerNotes;
  final String submitterUid;

  const DeveloperSubmissionModel({
    required this.id,
    required this.appName,
    required this.packageName,
    this.category = 'Tools',
    required this.developerName,
    required this.developerEmail,
    this.githubRepoUrl = '',
    required this.apkDownloadUrl,
    this.versionName = '1.0.0.1',
    this.versionCode = 1,
    this.description = '',
    this.status = 'PENDING',
    this.reviewerNotes = '',
    this.submitterUid = '',
  });

  factory DeveloperSubmissionModel.fromFirestore(
      DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return DeveloperSubmissionModel(
      id: (data['id'] as String?) ?? doc.id,
      appName: (data['appName'] as String?) ?? '',
      packageName: (data['packageName'] as String?) ?? '',
      category: (data['category'] as String?) ?? 'Tools',
      developerName: (data['developerName'] as String?) ?? '',
      developerEmail: (data['developerEmail'] as String?) ?? '',
      githubRepoUrl: (data['githubRepoUrl'] as String?) ?? '',
      apkDownloadUrl: (data['apkDownloadUrl'] as String?) ?? '',
      versionName: (data['versionName'] as String?) ?? '1.0.0.1',
      versionCode: (data['versionCode'] as num?)?.toInt() ?? 1,
      description: (data['description'] as String?) ?? '',
      status: (data['status'] as String?) ?? 'PENDING',
      reviewerNotes: (data['reviewerNotes'] as String?) ?? '',
      submitterUid: (data['submitterUid'] as String?) ?? '',
    );
  }
}

class AppReportModel {
  final String id;
  final String appId;
  final String appName;
  final String reporterUid;
  final String reason;
  final String details;
  final String status;

  const AppReportModel({
    required this.id,
    required this.appId,
    required this.appName,
    required this.reporterUid,
    required this.reason,
    required this.details,
    this.status = 'OPEN',
  });

  factory AppReportModel.fromFirestore(
      DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return AppReportModel(
      id: (data['id'] as String?) ?? doc.id,
      appId: (data['appId'] as String?) ?? '',
      appName: (data['appName'] as String?) ?? '',
      reporterUid: (data['reporterUid'] as String?) ?? '',
      reason: (data['reason'] as String?) ?? 'GENERAL',
      details: (data['details'] as String?) ?? '',
      status: (data['status'] as String?) ?? 'OPEN',
    );
  }
}

class AppVersionRecordModel {
  final String id;
  final String appId;
  final String versionName;
  final int versionCode;
  final String apkDownloadUrl;
  final String apkFileSize;
  final String sha256Checksum;
  final String releaseNotes;

  const AppVersionRecordModel({
    required this.id,
    required this.appId,
    required this.versionName,
    required this.versionCode,
    required this.apkDownloadUrl,
    this.apkFileSize = '12.0 MB',
    this.sha256Checksum = '',
    this.releaseNotes = '',
  });

  factory AppVersionRecordModel.fromFirestore(
      DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return AppVersionRecordModel(
      id: (data['id'] as String?) ?? doc.id,
      appId: (data['appId'] as String?) ?? '',
      versionName: (data['versionName'] as String?) ?? '1.0.0.1',
      versionCode: (data['versionCode'] as num?)?.toInt() ?? 1,
      apkDownloadUrl: (data['apkDownloadUrl'] as String?) ?? '',
      apkFileSize: (data['apkFileSize'] as String?) ?? '12.0 MB',
      sha256Checksum: (data['sha256Checksum'] as String?) ?? '',
      releaseNotes: (data['releaseNotes'] as String?) ?? '',
    );
  }
}

class AuditLogRecordModel {
  final String id;
  final String actorUid;
  final String actorEmail;
  final String action;
  final String targetType;
  final String targetId;
  final String summary;

  const AuditLogRecordModel({
    required this.id,
    required this.actorUid,
    required this.actorEmail,
    required this.action,
    required this.targetType,
    required this.targetId,
    required this.summary,
  });

  factory AuditLogRecordModel.fromFirestore(
      DocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data() ?? <String, dynamic>{};
    return AuditLogRecordModel(
      id: (data['id'] as String?) ?? doc.id,
      actorUid: (data['actorUid'] as String?) ?? '',
      actorEmail: (data['actorEmail'] as String?) ?? '',
      action: (data['action'] as String?) ?? '',
      targetType: (data['targetType'] as String?) ?? '',
      targetId: (data['targetId'] as String?) ?? '',
      summary: (data['summary'] as String?) ?? '',
    );
  }
}

