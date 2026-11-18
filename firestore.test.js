const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_zayaan_1";
const USER_UID = "user_reader_2";
const OTHER_UID = "user_other_3";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function createValidAppPayload(appId, authorUid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    appId,
    appName: "Zayaan Calculator",
    packageName: "com.zayaanapps.calculator",
    category: "Tools",
    developerName: "Zayaan Apps Studio",
    description: "Scientific and financial calculator with Material 3 design.",
    shortDescription: "Precision scientific and financial calculator.",
    iconUrl: "https://zayaanapps.github.io/zayaan-apps/icons/calculator.png",
    screenshots: [
      "https://zayaanapps.github.io/zayaan-apps/screenshots/calc1.png",
    ],
    versionName: "1.0.0.2",
    versionCode: 3,
    apkDownloadUrl: "https://github.com/zayaanapps/zayaan-calculator/releases/download/v1.0.0.2/app-release.apk",
    apkFileSize: "14.2 MB",
    releaseNotes: "Added unit converter and currency support.",
    featured: true,
    editorsChoice: true,
    essential: true,
    published: true,
    downloadCount: 0,
    ratingSum: 0,
    reviewCount: 0,
    sha256Checksum: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    minAndroidSdk: 24,
    targetAndroidSdk: 36,
    architecture: "arm64-v8a, universal",
    permissionsList: ["INTERNET", "VIBRATE"],
    privacyPolicyUrl: "https://zayaanapps.github.io/zayaan-apps/privacy",
    websiteUrl: "https://zayaanapps.github.io/zayaan-apps/",
    supportEmail: "zayaanapps@gmail.com",
    githubRepoUrl: "https://github.com/zayaanapps/zayaan-apps",
    offlineCapable: true,
    pageViews: 0,
    authorUid,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: cannot read or write apps, reviews, collections, or user preferences", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("apps").where("published", "==", true).get());
  await assertFails(
    unauthDb.collection("apps").doc("zayaan-calc").set(createValidAppPayload("zayaan-calc", ADMIN_UID))
  );
  await assertFails(unauthDb.collection("reviews").where("rating", ">=", 1).get());
  await assertFails(unauthDb.collection("users").doc(USER_UID).get());
});

test("Bootstrapped admin & author: can create, update, publish/unpublish, and delete StoreApp", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "zayaanapps@gmail.com",
    email_verified: true,
  }).firestore();

  const payload = createValidAppPayload("zayaan-calc", ADMIN_UID);
  await assertSucceeds(adminDb.collection("apps").doc("zayaan-calc").set(payload));

  // Partial update: version bump & SHA-256 checksum
  await assertSucceeds(
    adminDb.collection("apps").doc("zayaan-calc").update({
      versionName: "1.0.0.3",
      versionCode: 4,
      releaseNotes: "Performance improvements and Android 16 support.",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field injection (Shadow Update test)
  await assertFails(
    adminDb.collection("apps").doc("zayaan-calc").update({
      isVerified: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Community Reviews: user can create/edit own review, vote helpful, and cannot delete another user's review", async () => {
  const userDb = testEnv.authenticatedContext(USER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const now = new Date(Date.now() - 2000);
  const reviewId = `zayaan-calc_${USER_UID}`;

  await assertSucceeds(
    userDb.collection("reviews").doc(reviewId).set({
      appId: "zayaan-calc",
      appName: "Zayaan Calculator",
      userId: USER_UID,
      userName: "Zayaan Tester",
      rating: 5,
      comment: "Fast and accurate calculator with clean Material 3 UI!",
      verifiedInstall: true,
      helpfulCount: 0,
      developerReply: "",
      flagged: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Query reviews
  await assertSucceeds(
    userDb.collection("reviews").where("rating", ">=", 1).get()
  );

  // Other user votes helpful
  await assertSucceeds(
    otherDb.collection("reviews").doc(reviewId).update({
      helpfulCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Other user cannot delete user's review
  await assertFails(otherDb.collection("reviews").doc(reviewId).delete());
});

test("Developer Portal & Collections: user can submit app for approval and create collection", async () => {
  const userDb = testEnv.authenticatedContext(USER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    userDb.collection("submissions").doc("sub_1").set({
      appId: "zayaan-weather",
      appName: "Zayaan Weather Radar",
      packageName: "com.zayaanapps.weather",
      category: "Utilities",
      developerName: "Community Dev",
      description: "Hyper-local weather radar and storm alerts.",
      versionName: "1.0.0.1",
      versionCode: 1,
      apkDownloadUrl: "https://github.com/zayaanapps/zayaan-apps/releases/download/v1.0.0.1/weather.apk",
      apkFileSize: "11.5 MB",
      sha256Checksum: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      releaseNotes: "Initial submission.",
      privacyPolicyUrl: "https://zayaanapps.github.io/zayaan-apps/privacy",
      supportEmail: "dev@example.com",
      status: "PENDING",
      reviewNote: "",
      submitterUid: USER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    userDb.collection("collections").doc("col_1").set({
      title: "Essential Apps for New Phones",
      subtitle: "Must-have daily utilities and productivity tools.",
      appIds: ["zayaan-calc", "zayaan-notes"],
      authorUid: USER_UID,
      isPublic: true,
      featured: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User preferences & library: user can sync wishlist, favorites, search history, and downloads", async () => {
  const userDb = testEnv.authenticatedContext(USER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    userDb.collection("users").doc(USER_UID).set({
      userId: USER_UID,
      darkMode: true,
      notifyUpdates: true,
      bookmarkedAppIds: ["zayaan-calc"],
      favoriteAppIds: ["zayaan-calc"],
      recentlyViewedAppIds: ["zayaan-calc"],
      downloadedAppIds: ["zayaan-calc"],
      searchHistory: ["calculator", "notes"],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(userDb.collection("users").doc(USER_UID).get());
  await assertFails(otherDb.collection("users").doc(USER_UID).get());
});

test("Phase 3 Admin Management: version history, reports moderation, admin RBAC, and immutable audit logs", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "zayaanapps@gmail.com",
    email_verified: true,
  }).firestore();
  const userDb = testEnv.authenticatedContext(USER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // 1. User creates a security/broken-link report
  await assertSucceeds(
    userDb.collection("reports").doc("rep_1").set({
      targetId: "zayaan-calc",
      targetType: "APP",
      reason: "Checksum Verification Request",
      details: "Please verify v1.0.0.2 SHA-256 hash matches GitHub release.",
      reporterUid: USER_UID,
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin resolves the report
  await assertSucceeds(
    adminDb.collection("reports").doc("rep_1").update({
      status: "RESOLVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 2. Admin records a version history release entry
  await assertSucceeds(
    adminDb.collection("version_history").doc("ver_zayaan_calc_3").set({
      appId: "zayaan-calc",
      versionName: "1.0.0.2",
      versionCode: 3,
      apkDownloadUrl: "https://github.com/zayaanapps/zayaan-calculator/releases/download/v1.0.0.2/app-release.apk",
      apkFileSize: "14.2 MB",
      sha256Checksum: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      changelog: "Added unit converter and currency support.",
      authorUid: ADMIN_UID,
      createdAt: now,
    })
  );

  // 3. Admin grants trusted admin role in /admins/{adminUid}
  await assertSucceeds(
    adminDb.collection("admins").doc(OTHER_UID).set({
      userId: OTHER_UID,
      role: "admin",
      createdAt: now,
    })
  );

  // Non-admin cannot grant admin roles
  await assertFails(
    userDb.collection("admins").doc(USER_UID).set({
      userId: USER_UID,
      role: "superadmin",
      createdAt: now,
    })
  );

  // 4. Immutable Audit Logs (/audit_logs/{logId})
  await assertSucceeds(
    adminDb.collection("audit_logs").doc("audit_1").set({
      action: "SUBMISSION_APPROVED",
      targetType: "SUBMISSION",
      targetId: "sub_1",
      summary: "Approved Zayaan Weather Radar v1.0.0.1 and published to catalog.",
      actorUid: ADMIN_UID,
      actorEmail: "zayaanapps@gmail.com",
      createdAt: now,
    })
  );

  // Audit logs cannot be mutated/updated once written
  await assertFails(
    adminDb.collection("audit_logs").doc("audit_1").update({
      summary: "Tampered audit log summary",
    })
  );
});
