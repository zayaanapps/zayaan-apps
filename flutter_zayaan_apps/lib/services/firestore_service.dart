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
}
