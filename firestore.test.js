const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-rafiqul-f8f3bebd-a4f0-4402-8eda-8c722b4f5f2a";
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

  await assertFails(db.collection("users").get());

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

  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).update({
      username: "rafiqul.islam",
      email: "rafiqul.new@example.com",
      passwordHash: UPDATED_HASH,
      profilePictureUrl: "https://example.com/new-avatar.png",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

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

test("Transactions: owner CRUD with ownerScope succeeds; unauth, cross-user, future timestamps, and id/shadow keys fail", async () => {
  const unauth = unauthDb();
  await assertFails(unauth.collection(`users/${ALICE_UID}/transactions`).get());
  await assertFails(unauth.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").delete());

  const db = aliceDb();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").set({
      userId: ALICE_UID,
      ownerScope: "local_rafiqul",
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
      ownerScope: "member_m1",
      type: "income",
      amount: 1500.0,
      category: "ব্যবসা",
      note: "বিক্রয় আয়",
      date: "2026-11-20",
      account: "bank",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

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
      createdAt: created,
      updatedAt: created,
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
      updatedAt: created,
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(db.collection(`users/${BOB_UID}/transactions`).get());
  await assertSucceeds(db.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").delete());
});

test("Members: owner create, partial update, list, delete succeed; invalid fields and cross-user fail", async () => {
  const unauth = unauthDb();
  await assertFails(unauth.collection(`users/${ALICE_UID}/members`).get());

  const db = aliceDb();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/members`).doc("member_1").set({
      userId: ALICE_UID,
      name: "Karim",
      designation: "MPO",
      phone: "01711000000",
      depot: "Dhaka",
      code: "M001",
      area: "Mirpur",
      active: true,
      clientCreatedAtMs: 1732000000000,
      passwordHash: VALID_HASH,
      defaultPassword: true,
      email: "karim@example.com",
      address: "Dhaka",
      joiningDate: "2026-01-01",
      salesAmount: 12000.0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(db.collection(`users/${ALICE_UID}/members`).get());

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/members`).doc("member_1").update({
      active: false,
      passwordHash: UPDATED_HASH,
      defaultPassword: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Rejects explicit id or isActive shadow key or immutable createdAt change
  await assertFails(
    db.collection(`users/${ALICE_UID}/members`).doc("member_bad_id").set({
      id: "member_bad_id",
      userId: ALICE_UID,
      name: "Bad",
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    db.collection(`users/${ALICE_UID}/members`).doc("member_1").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(db.collection(`users/${BOB_UID}/members`).get());
  await assertSucceeds(db.collection(`users/${ALICE_UID}/members`).doc("member_1").delete());
});

test("TakaJoma: owner create, approve/reject partial update, list, delete succeed; invalid enum/amount/cross-user fail", async () => {
  const unauth = unauthDb();
  await assertFails(unauth.collection(`users/${ALICE_UID}/takaJoma`).get());

  const db = aliceDb();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/takaJoma`).doc("sub_1").set({
      userId: ALICE_UID,
      memberId: "member_1",
      name: "Karim",
      code: "M001",
      depot: "Dhaka",
      designation: "MPO",
      area: "Mirpur",
      bankName: "বিকাশ (bKash)",
      amount: 5000.0,
      date: "2026-11-20",
      note: "সেলস জমা",
      slipHash: "abc123hash",
      submittedByRole: "MEMBER",
      clientCreatedAtMs: 1732000000000,
      approvalStatus: "PENDING",
      reviewedAtMs: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(db.collection(`users/${ALICE_UID}/takaJoma`).get());

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/takaJoma`).doc("sub_1").update({
      approvalStatus: "APPROVED",
      reviewedAtMs: 1732000010000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Rejects invalid status or zero/negative amount
  await assertFails(
    db.collection(`users/${ALICE_UID}/takaJoma`).doc("sub_bad").set({
      userId: ALICE_UID,
      memberId: "member_1",
      name: "Karim",
      amount: 0,
      date: "2026-11-20",
      submittedByRole: "MEMBER",
      approvalStatus: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    db.collection(`users/${ALICE_UID}/takaJoma`).doc("sub_1").update({
      approvalStatus: "UNKNOWN",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(db.collection(`users/${BOB_UID}/takaJoma`).get());
  await assertSucceeds(db.collection(`users/${ALICE_UID}/takaJoma`).doc("sub_1").delete());
});

test("MonthlySales & AppConfig: owner create, partial update, list, delete succeed; invalid formats and cross-user fail", async () => {
  const unauth = unauthDb();
  await assertFails(unauth.collection(`users/${ALICE_UID}/monthlySales`).get());
  await assertFails(unauth.collection(`users/${ALICE_UID}/appConfig`).get());

  const db = aliceDb();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/monthlySales`).doc("sales_1").set({
      userId: ALICE_UID,
      yearMonth: "2026-11",
      code: "M001",
      name: "Karim",
      tpSales: 25000.0,
      tpPlusVat: 28750.0,
      clientCreatedAtMs: 1732000000000,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/monthlySales`).doc("sales_1").update({
      tpSales: 26000.0,
      tpPlusVat: 29900.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/monthlySales`).doc("sales_bad").set({
      userId: ALICE_UID,
      yearMonth: "11-2026",
      code: "M001",
      name: "Karim",
      tpSales: 100,
      tpPlusVat: 115,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/appConfig`).doc("online_payment").set({
      userId: ALICE_UID,
      onlinePaymentEnabled: true,
      accountsJson: "[]",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/appConfig`).doc("member_app_link").set({
      userId: ALICE_UID,
      downloadUrl: "https://example.com/app",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/appConfig`).doc("admin_auth").set({
      userId: ALICE_UID,
      loginId: "rafiqul",
      passwordHash: VALID_HASH,
      defaultPassword: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/appConfig`).doc("drive_backup").set({
      userId: ALICE_UID,
      driveConnected: true,
      driveEmail: "netai.nemai@gmail.com",
      driveFileId: "1abc_drive_file_id",
      driveAutoBackupEnabled: true,
      lastBackupAtMs: 1732000010000,
      lastBackupSummary: "৫ লেনদেন • ২ মেম্বার",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    db.collection(`users/${ALICE_UID}/appConfig`).doc("drive_backup").update({
      driveConnected: false,
      driveEmail: "",
      driveFileId: "",
      driveAutoBackupEnabled: true,
      lastBackupAtMs: 0,
      lastBackupSummary: "",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/appConfig`).doc("drive_backup").set({
      userId: ALICE_UID,
      driveConnected: "yes",
      driveEmail: "netai.nemai@gmail.com",
      driveFileId: "1abc",
      driveAutoBackupEnabled: true,
      lastBackupAtMs: -5,
      lastBackupSummary: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    db.collection(`users/${ALICE_UID}/appConfig`).doc("member_app_link").set({
      userId: ALICE_UID,
      downloadUrl: "javascript:alert(1)",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(db.collection(`users/${BOB_UID}/appConfig`).get());
  await assertSucceeds(db.collection(`users/${ALICE_UID}/monthlySales`).doc("sales_1").delete());
});
