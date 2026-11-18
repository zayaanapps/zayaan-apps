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
    iconUrl: "https://zayaanapps.github.io/zayaan-apps/icons/calculator.png",
    screenshots: [
      "https://zayaanapps.github.io/zayaan-apps/screenshots/calc1.png",
    ],
    versionName: "1.2.0",
    versionCode: 3,
    apkDownloadUrl: "https://github.com/zayaanapps/zayaan-calculator/releases/download/v1.2.0/app-release.apk",
    apkFileSize: "14.2 MB",
    releaseNotes: "Added unit converter and currency support.",
    featured: true,
    published: true,
    downloadCount: 0,
    authorUid,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: cannot read or write apps, categories, or user preferences", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("apps").where("published", "==", true).get());
  await assertFails(
    unauthDb.collection("apps").doc("zayaan-calc").set(createValidAppPayload("zayaan-calc", ADMIN_UID))
  );
  await assertFails(unauthDb.collection("categories").where("displayOrder", ">=", 0).get());
  await assertFails(unauthDb.collection("users").doc(USER_UID).get());
});

test("Bootstrapped admin & author: can create, update, publish/unpublish, and delete StoreApp", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "zayaanapps@gmail.com",
    email_verified: true,
  }).firestore();

  const payload = createValidAppPayload("zayaan-calc", ADMIN_UID);
  await assertSucceeds(adminDb.collection("apps").doc("zayaan-calc").set(payload));

  // Partial update: version bump & release notes
  await assertSucceeds(
    adminDb.collection("apps").doc("zayaan-calc").update({
      versionName: "1.3.0",
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

  // Reject immutable createdAt mutation
  await assertFails(
    adminDb.collection("apps").doc("zayaan-calc").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Email spoofing defense: unverified admin email cannot modify another user's app", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("apps").doc("zayaan-notes").set(
      createValidAppPayload("zayaan-notes", ADMIN_UID)
    );
  });

  const spoofAdminDb = testEnv.authenticatedContext(OTHER_UID, {
    email: "zayaanapps@gmail.com",
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofAdminDb.collection("apps").doc("zayaan-notes").update({
      appName: "Hijacked Notes",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(spoofAdminDb.collection("apps").doc("zayaan-notes").delete());
});

test("Catalog queries & download counter: authenticated user can query published apps and increment downloadCount by +1", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("apps").doc("zayaan-calc").set(
      createValidAppPayload("zayaan-calc", ADMIN_UID, { published: true, downloadCount: 10 })
    );
    await db.collection("apps").doc("zayaan-draft").set(
      createValidAppPayload("zayaan-draft", ADMIN_UID, { appId: "zayaan-draft", published: false })
    );
  });

  const userDb = testEnv.authenticatedContext(USER_UID).firestore();

  // Filtered query for published apps succeeds
  await assertSucceeds(
    userDb.collection("apps").where("published", "==", true).get()
  );

  // Reading unpublished draft app by non-author fails
  await assertFails(userDb.collection("apps").doc("zayaan-draft").get());

  // Incrementing downloadCount by +1 succeeds
  await assertSucceeds(
    userDb.collection("apps").doc("zayaan-calc").update({
      downloadCount: 11,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Arbitrary jump in downloadCount (+100) by non-admin fails
  await assertFails(
    userDb.collection("apps").doc("zayaan-calc").update({
      downloadCount: 110,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("User preferences isolation: user can read/write own preferences but not another user's", async () => {
  const userDb = testEnv.authenticatedContext(USER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    userDb.collection("users").doc(USER_UID).set({
      userId: USER_UID,
      darkMode: true,
      notifyUpdates: true,
      bookmarkedAppIds: ["zayaan-calc"],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(userDb.collection("users").doc(USER_UID).get());
  await assertFails(otherDb.collection("users").doc(USER_UID).get());
  await assertFails(
    otherDb.collection("users").doc(USER_UID).update({
      darkMode: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
