import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import '../models/store_app.dart';

class ZayaanFirestoreService {
  /// Connects to the provisioned Zayaan Apps Firestore database.
  static const String kProvisionedDatabaseId =
      'ai-studio-android-zayaanap-6fb94416-b000-40bb-ac28-77481774653b';

  final FirebaseFirestore _db;
  final FirebaseAuth _auth;

  ZayaanFirestoreService({
    FirebaseFirestore? db,
    FirebaseAuth? auth,
  })  : _db = db ??
            FirebaseFirestore.instanceFor(
              app: Firebase.app(),
              databaseId: kProvisionedDatabaseId,
            ),
        _auth = auth ?? FirebaseAuth.instance;

  Stream<List<StoreAppModel>> observePublishedApps() {
    return _db
        .collection('apps')
        .where('published', isEqualTo: true)
        .snapshots()
        .map((snap) =>
            snap.docs.map((d) => StoreAppModel.fromFirestore(d)).toList());
  }

  Stream<List<StoreAppModel>> observeAdminApps(String uid) {
    return _db
        .collection('apps')
        .where('authorUid', isEqualTo: uid)
        .snapshots()
        .map((snap) =>
            snap.docs.map((d) => StoreAppModel.fromFirestore(d)).toList());
  }

  Future<void> saveApp(StoreAppModel app, {required bool isEdit}) async {
    final uid = _auth.currentUser?.uid;
    if (uid == null) {
      throw StateError('Administrator must be signed in before saving.');
    }
    final docRef = _db.collection('apps').doc(app.appId);
    if (isEdit) {
      await docRef.update(app.toUpdatePayload());
    } else {
      await docRef.set(app.toCreatePayload(uid));
    }
  }

  Future<void> incrementDownloadCount(String appId) async {
    await _db.collection('apps').doc(appId).update({
      'downloadCount': FieldValue.increment(1),
      'updatedAt': FieldValue.serverTimestamp(),
    });
  }

  Future<void> deleteApp(String appId) async {
    await _db.collection('apps').doc(appId).delete();
  }

  Stream<List<AppReviewModel>> observeReviewsForApp(String appId) {
    return _db
        .collection('reviews')
        .where('appId', isEqualTo: appId)
        .where('rating', isGreaterThanOrEqualTo: 1)
        .snapshots()
        .map((snap) =>
            snap.docs.map((d) => AppReviewModel.fromFirestore(d)).toList());
  }

  Future<void> submitReview({
    required StoreAppModel app,
    required int rating,
    required String comment,
    required bool verifiedInstall,
  }) async {
    final user = _auth.currentUser;
    if (user == null) {
      throw StateError('Sign in with Google to submit a review.');
    }
    final docId = '${app.appId}_${user.uid}'
        .replaceAll(RegExp(r'[^a-zA-Z0-9_\-]'), '_');
    final ref = _db.collection('reviews').doc(docId);
    final snap = await ref.get();
    final cleanRating = rating.clamp(1, 5);
    final userName = (user.displayName ?? user.email ?? 'Zayaan User')
        .split('@')
        .first;
    if (!snap.exists) {
      await ref.set({
        'appId': app.appId,
        'appName': app.appName,
        'userId': user.uid,
        'userName': userName,
        'rating': cleanRating,
        'comment': comment.trim().isEmpty ? 'Rated $cleanRating stars.' : comment.trim(),
        'verifiedInstall': verifiedInstall,
        'helpfulCount': 0,
        'developerReply': '',
        'flagged': false,
        'createdAt': FieldValue.serverTimestamp(),
        'updatedAt': FieldValue.serverTimestamp(),
      });
    } else {
      await ref.update({
        'rating': cleanRating,
        'comment': comment.trim(),
        'userName': userName,
        'verifiedInstall': verifiedInstall,
        'updatedAt': FieldValue.serverTimestamp(),
      });
    }
  }

  Future<void> voteReviewHelpful(AppReviewModel review) async {
    await _db.collection('reviews').doc(review.id).update({
      'helpfulCount': review.helpfulCount + 1,
      'flagged': review.flagged,
      'updatedAt': FieldValue.serverTimestamp(),
    });
  }

  Future<void> flagReview(AppReviewModel review, bool flagged) async {
    await _db.collection('reviews').doc(review.id).update({
      'helpfulCount': review.helpfulCount,
      'flagged': flagged,
      'updatedAt': FieldValue.serverTimestamp(),
    });
  }

  Stream<UserPreferenceModel> observeUserPreferences(String uid) {
    return _db.collection('users').doc(uid).snapshots().map(
          (snap) => UserPreferenceModel.fromFirestore(uid, snap.data()),
        );
  }

  Future<void> saveUserPreferences(UserPreferenceModel pref) async {
    final ref = _db.collection('users').doc(pref.userId);
    final snap = await ref.get();
    final payload = <String, dynamic>{
      'userId': pref.userId,
      'darkMode': pref.darkMode,
      'notifyUpdates': pref.notifyUpdates,
      'bookmarkedAppIds': pref.bookmarkedAppIds.take(50).toList(),
      'favoriteAppIds': pref.favoriteAppIds.take(50).toList(),
      'recentlyViewedAppIds': pref.recentlyViewedAppIds.take(30).toList(),
      'downloadedAppIds': pref.downloadedAppIds.take(50).toList(),
      'searchHistory': pref.searchHistory.take(20).toList(),
      'updatedAt': FieldValue.serverTimestamp(),
    };
    if (!snap.exists) {
      payload['createdAt'] = FieldValue.serverTimestamp();
      await ref.set(payload);
    } else {
      await ref.set(payload, SetOptions(merge: true));
    }
  }

  Stream<List<StoreNotificationModel>> observeNotifications() {
    return _db
        .collection('notifications')
        .where('published', isEqualTo: true)
        .snapshots()
        .map((snap) => snap.docs
            .map((d) => StoreNotificationModel.fromFirestore(d))
            .toList());
  }

  Stream<List<AppCollectionModel>> observeCollections() {
    return _db.collection('collections').snapshots().map((snap) =>
        snap.docs.map((d) => AppCollectionModel.fromFirestore(d)).toList());
  }

  Stream<List<DeveloperSubmissionModel>> observeSubmissions() {
    return _db.collection('submissions').snapshots().map((snap) => snap.docs
        .map((d) => DeveloperSubmissionModel.fromFirestore(d))
        .toList());
  }

  Future<void> createSubmission(DeveloperSubmissionModel sub) async {
    final uid = _auth.currentUser?.uid;
    if (uid == null) throw StateError('Sign in to submit an app.');
    final docId = sub.id.isNotEmpty
        ? sub.id
        : 'sub_${DateTime.now().millisecondsSinceEpoch}';
    await _db.collection('submissions').doc(docId).set({
      'id': docId,
      'appName': sub.appName,
      'packageName': sub.packageName,
      'category': sub.category,
      'developerName': sub.developerName,
      'developerEmail': sub.developerEmail,
      'githubRepoUrl': sub.githubRepoUrl,
      'apkDownloadUrl': sub.apkDownloadUrl,
      'versionName': sub.versionName,
      'versionCode': sub.versionCode,
      'description': sub.description,
      'status': 'PENDING',
      'reviewerNotes': '',
      'submitterUid': uid,
      'createdAt': FieldValue.serverTimestamp(),
      'updatedAt': FieldValue.serverTimestamp(),
    });
  }

  Stream<List<AppReportModel>> observeReports() {
    return _db.collection('reports').snapshots().map((snap) =>
        snap.docs.map((d) => AppReportModel.fromFirestore(d)).toList());
  }

  Future<void> submitReport({
    required String appId,
    required String appName,
    required String reason,
    required String details,
  }) async {
    final uid = _auth.currentUser?.uid;
    if (uid == null) throw StateError('Sign in to report an issue.');
    final docId = 'rep_${appId}_${DateTime.now().millisecondsSinceEpoch}';
    await _db.collection('reports').doc(docId).set({
      'id': docId,
      'appId': appId,
      'appName': appName,
      'reporterUid': uid,
      'reason': reason,
      'details': details,
      'status': 'OPEN',
      'createdAt': FieldValue.serverTimestamp(),
      'updatedAt': FieldValue.serverTimestamp(),
    });
  }

  Stream<List<AppVersionRecordModel>> observeVersionHistory(String appId) {
    return _db
        .collection('version_history')
        .where('appId', isEqualTo: appId)
        .snapshots()
        .map((snap) => snap.docs
            .map((d) => AppVersionRecordModel.fromFirestore(d))
            .toList());
  }

  Stream<List<AuditLogRecordModel>> observeAuditLogs() {
    return _db.collection('audit_logs').snapshots().map((snap) =>
        snap.docs.map((d) => AuditLogRecordModel.fromFirestore(d)).toList());
  }

  Future<void> recordAuditLog({
    required String action,
    required String targetType,
    required String targetId,
    required String summary,
  }) async {
    final user = _auth.currentUser;
    if (user == null) return;
    final logId = 'audit_${DateTime.now().millisecondsSinceEpoch}';
    await _db.collection('audit_logs').doc(logId).set({
      'id': logId,
      'actorUid': user.uid,
      'actorEmail': user.email ?? 'admin@zayaanapps.com',
      'action': action,
      'targetType': targetType,
      'targetId': targetId,
      'summary': summary,
      'createdAt': FieldValue.serverTimestamp(),
    });
  }
}

