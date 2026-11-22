const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-rafiquli-a8d07eb6-5ef8-4981-bcd4-911b31b1ff11";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const VALID_HASH = "pbkdf2_sha256$120000$YWJjZGVmZ2hpamtsbW5vcA==$MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
const UPDATED_HASH = "pbkdf2_sha256$120000$cXdlcnR5dWlvcGFzZGZnaGo=$OTg3NjU0MzIxMDk4NzY1NDMyMTA5ODc2NTQzMjEwOTg=";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function aliceDb() {
  return testEnv.authenticatedContext(ALICE_UID).firestore({ databaseId: DATABASE_ID });
}

function unauthDb() {
  return testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });
}

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

test("UserProfile: unauthenticated and list queries are denied; owner create/get/update/delete succeed", async () => {
  const unauth = unauthDb();
  await assertFails(unauth.collection("users").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "rafiqul",
      email: "rafiqul@example.com",
      passwordHash: VALID_HASH,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  const db = aliceDb();
  const created = new Date(Date.now() - 10000);

  // Listing /users is forbidden for PII isolation
  await assertFails(db.collection("users").get());

  // Valid owner profile registration
  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "rafiqul_bd",
      email: "rafiqul@example.com",
      passwordHash: VALID_HASH,
      profilePictureUrl: "https://example.com/avatar.png",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(db.collection("users").doc(ALICE_UID).get());

  // Valid partial profile update
  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).update({
      username: "rafiqul.islam",
      email: "rafiqul.new@example.com",
      passwordHash: UPDATED_HASH,
      profilePictureUrl: "https://example.com/new-avatar.png",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user isolation on profiles
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore({ databaseId: DATABASE_ID }).collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_user",
      email: "bob@example.com",
      passwordHash: VALID_HASH,
      createdAt: created,
      updatedAt: created,
    });
  });

  await assertFails(db.collection("users").doc(BOB_UID).get());
  await assertFails(
    db.collection("users").doc(BOB_UID).update({
      username: "hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(db.collection("users").doc(BOB_UID).delete());
});

test("UserProfile: rejects plaintext password, unsafe URL scheme, @DocumentId id field, and immutable changes", async () => {
  const db = aliceDb();
  const now = new Date(Date.now() - 5000);

  // Rejects plaintext password
  await assertFails(
    db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "rafiqul",
      email: "rafiqul@example.com",
      passwordHash: "plaintext_password_123456789012345",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Rejects non-HTTPS profilePictureUrl (javascript: / http:)
  await assertFails(
    db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "rafiqul",
      email: "rafiqul@example.com",
      passwordHash: VALID_HASH,
      profilePictureUrl: "http://insecure.example.com/avatar.png",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Rejects explicit id field
  await assertFails(
    db.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      userId: ALICE_UID,
      username: "rafiqul",
      email: "rafiqul@example.com",
      passwordHash: VALID_HASH,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid create, then reject immutable createdAt update
  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "rafiqul",
      email: "rafiqul@example.com",
      passwordHash: VALID_HASH,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    db.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Unauthenticated user cannot list, get, create, or delete transactions", async () => {
  const db = unauthDb();
  await assertFails(db.collection(`users/${ALICE_UID}/transactions`).get());
  await assertFails(db.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").get());
  await assertFails(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").set({
      userId: ALICE_UID,
      type: "expense",
      amount: 250.0,
      category: "খাবার",
      note: "দুপুরের খাবার",
      date: "2026-11-20",
      account: "cash",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(db.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").delete());
});

test("Authenticated owner: valid Kotlin create, partial update, list, and delete succeed", async () => {
  const db = aliceDb();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").set({
      userId: ALICE_UID,
      type: "expense",
      amount: 450.5,
      category: "বাজার",
      note: "সাপ্তাহিক বাজার",
      date: "2026-11-20",
      account: "cash",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(db.collection(`users/${ALICE_UID}/transactions`).get());

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").update({
      type: "income",
      amount: 1500.0,
      category: "ব্যবসা",
      note: "বিক্রয় আয়",
      date: "2026-11-20",
      account: "bank",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(db.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").delete());
});

test("Rejects @DocumentId `id` field, shadow fields, immutable mutations, and invalid data", async () => {
  const db = aliceDb();
  const now = new Date(Date.now() - 5000);

  await assertFails(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_with_id").set({
      id: "tx_with_id",
      userId: ALICE_UID,
      type: "expense",
      amount: 100,
      category: "খাবার",
      note: "Test",
      date: "2026-11-20",
      account: "cash",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_shadow").set({
      userId: ALICE_UID,
      type: "expense",
      amount: 100,
      category: "খাবার",
      note: "Test",
      date: "2026-11-20",
      account: "cash",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_neg").set({
      userId: ALICE_UID,
      type: "expense",
      amount: -50,
      category: "খাবার",
      note: "Test",
      date: "2026-11-20",
      account: "cash",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_bad_date").set({
      userId: ALICE_UID,
      type: "expense",
      amount: 50,
      category: "খাবার",
      note: "Test",
      date: "20-11-2026",
      account: "cash",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_future").set({
      userId: ALICE_UID,
      type: "expense",
      amount: 50,
      category: "খাবার",
      note: "Test",
      date: "2026-11-20",
      account: "cash",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_valid").set({
      userId: ALICE_UID,
      type: "expense",
      amount: 100,
      category: "খাবার",
      note: "Initial",
      date: "2026-11-20",
      account: "cash",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_valid").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_valid").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Cross-user isolation: Alice cannot read, list, create, update, or delete Bob's transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore({ databaseId: DATABASE_ID });
    await db.collection(`users/${BOB_UID}/transactions`).doc("bob_tx").set({
      userId: BOB_UID,
      type: "income",
      amount: 5000,
      category: "বেতন",
      note: "Bob salary",
      date: "2026-11-20",
      account: "bank",
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
  });

  const db = aliceDb();
  await assertFails(db.collection(`users/${BOB_UID}/transactions`).get());
  await assertFails(db.collection(`users/${BOB_UID}/transactions`).doc("bob_tx").get());
  await assertFails(
    db.collection(`users/${BOB_UID}/transactions`).doc("bob_tx").update({
      amount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(db.collection(`users/${BOB_UID}/transactions`).doc("bob_tx").delete());
  await assertFails(
    db.collection(`users/${BOB_UID}/transactions`).doc("alice_inject").set({
      userId: ALICE_UID,
      type: "income",
      amount: 100,
      category: "উপহার",
      note: "Injected",
      date: "2026-11-20",
      account: "cash",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
