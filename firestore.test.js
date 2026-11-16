const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";
const MALLORY_UID = "mallory_uid_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validVaultPayload(ownerId = ALICE_UID, memberUids = [ALICE_UID], now = new Date()) {
  return {
    ownerId,
    memberUids,
    inviteCode: "DUO2028",
    title: "May 2028 Ortak Hedef",
    currency: "₼",
    leatherTheme: "black",
    targetAmount: 20000,
    dailyTarget: 35,
    totalSaved: 0,
    ownerSaved: 0,
    partnerSaved: 0,
    ownerName: "Əli",
    partnerName: "",
    partnerUid: "",
    targetDateIso: "2028-05-31",
    createdAt: now,
    updatedAt: now,
  };
}

function validDepositPayload(vaultId = "vault_1", userId = ALICE_UID, memberUids = [ALICE_UID], now = new Date()) {
  return {
    vaultId,
    userId,
    userName: "Əli",
    memberUids,
    amount: 50,
    note: "İlk əmanət",
    dayKey: "2026-11-15",
    createdAt: now,
    updatedAt: now,
  };
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

test("Unauthenticated user cannot read or write vaults or deposits", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("vaults").get());
  await assertFails(unauthDb.collection("deposits").get());
});

test("Authenticated owner can create a valid vault and query via memberUids", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("vaults").doc("DUO2028").set(validVaultPayload())
  );
  await assertSucceeds(
    aliceDb.collection("vaults").where("memberUids", "array-contains", ALICE_UID).get()
  );
});

test("Non-member cannot get or list another user's vault", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("vaults").doc("DUO2028").set(validVaultPayload());
  });
  const malloryDb = testEnv.authenticatedContext(MALLORY_UID).firestore();
  await assertFails(malloryDb.collection("vaults").doc("DUO2028").get());
  await assertFails(malloryDb.collection("vaults").get());
});

test("Partner friend can join a 1-member vault via Action 2 update", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("vaults").doc("DUO2028").set(validVaultPayload(ALICE_UID, [ALICE_UID], now));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("vaults").doc("DUO2028").update({
      memberUids: [ALICE_UID, BOB_UID],
      partnerUid: BOB_UID,
      partnerName: "Rəşad",
      updatedAt: new Date(),
    })
  );

  // Now Bob is a member and can read the vault
  await assertSucceeds(bobDb.collection("vaults").doc("DUO2028").get());
});

test("Shadow update with ghost field on vault is rejected", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("vaults").doc("DUO2028").set(validVaultPayload(ALICE_UID, [ALICE_UID], now));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("vaults").doc("DUO2028").update({
      totalSaved: 100,
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Deposit creation requires existing vault where user is a member", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Fails when vault does not exist
  await assertFails(
    aliceDb.collection("deposits").doc("dep_1").set(validDepositPayload("missing_vault", ALICE_UID, [ALICE_UID]))
  );

  // Succeeds when vault exists and user is member
  await assertSucceeds(
    aliceDb.collection("vaults").doc("DUO2028").set(validVaultPayload(ALICE_UID, [ALICE_UID, BOB_UID]))
  );
  await assertSucceeds(
    aliceDb.collection("deposits").doc("dep_1").set(validDepositPayload("DUO2028", ALICE_UID, [ALICE_UID, BOB_UID]))
  );

  // Partner Bob can also query shared deposits
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("deposits").where("memberUids", "array-contains", BOB_UID).get()
  );
});
